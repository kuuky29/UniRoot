package com.uniroot.app.newmethod

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.ZipException

/**
 * THE SCRIPT, in the app (NotStable): downloads the LATEST official ksud
 * (KernelSU Next OR classic, per the selected flavor) and patches it with
 * the 8 Samsung KDP+DEFEX kos bundled in the APK — the exact port of
 * patch-ksud-next.py.
 *
 * How it works (README-PATCH): ksud embeds its assets via rust-embed +
 * include-flate = raw DEFLATE streams glued into .rodata. At runtime the
 * decoder stops at the final block and IGNORES the following bytes -> a
 * shorter replacement stream + zero-padding works, no section moved. No
 * hash is verified at runtime.
 *
 * Already up to date? -> "you are already updated".
 * - Next: the result lands in filesDir/ksud-next/ = a ksud Next profile,
 *   auto-selected (KsudNextProfiles.addDynamic).
 * - Classic: the result lands in filesDir/ksud-classic-latest, which
 *   DFBridge.stageKsud stages automatically for the classic flavor.
 */
object DfKsudUpdater {

    private val REPOS_NEXT = listOf("KernelSU-Next/KernelSU-Next", "rifsxd/KernelSU-Next")
    private val REPOS_CLASSIC = listOf("tiann/KernelSU", "KernelSU/KernelSU")
    private const val KO_ASSET_DIR = "df/samsung-ko"
    private const val KO_ASSET_DIR_CLASSIC = "df/samsung-ko-classic"
    private const val NEXT_MODULE_VERSION = "3.4.2"
    private val KMIS = listOf(
        "android12-5.10", "android13-5.10", "android13-5.15", "android14-5.15",
        "android14-6.1", "android15-6.6", "android16-6.12", "android17-6.18")

    /** journal -> UI + logcat */
    var log: ((String) -> Unit)? = null
    private fun say(msg: String) { log?.invoke(msg); android.util.Log.i("DfKsudUpd", msg) }

    // ------------------------------------------------------------------
    // Deflate / inflate (java.util.zip, nowrap = raw deflate)
    // ------------------------------------------------------------------

    private fun inflateRaw(data: ByteArray, off: Int, limitBytes: Int, maxOut: Int): ByteArray? {
        return try {
            val inf = Inflater(true)
            inf.setInput(data, off, limitBytes)
            val out = ByteArray(maxOut)
            val n = inf.inflate(out)
            inf.end()
            if (n <= 0) null else out.copyOf(n)
        } catch (e: ZipException) { null } catch (e: Exception) { null }
    }

    private fun deflateRaw(raw: ByteArray, level: Int, strategy: Int): ByteArray {
        val def = Deflater(level, true)
        def.setStrategy(strategy)
        def.setInput(raw)
        def.finish()
        val buf = ByteArray(raw.size + raw.size / 16 + 256)
        val n = def.deflate(buf)
        def.end()
        return buf.copyOf(n)
    }

    /** Smallest raw deflate stream that fits maxSize (like the .py). */
    private fun compressBest(raw: ByteArray, maxSize: Int): ByteArray? {
        var best: ByteArray? = null
        for (level in intArrayOf(9, 6, 1)) {
            for (strategy in intArrayOf(Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY)) {
                val comp = deflateRaw(raw, level, strategy)
                if (best == null || comp.size < best.size) best = comp
                if (comp.size <= maxSize) return comp
            }
        }
        return best
    }

    private fun containsBytes(data: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > data.size) return false
        outer@ for (i in 0..data.size - needle.size) {
            for (j in needle.indices) if (data[i + j] != needle[j]) continue@outer
            return true
        }
        return false
    }

    private fun containsBytes(data: ByteArray, needle: ByteArray, at: Int): Boolean {
        for (j in needle.indices) if (data[at + j] != needle[j]) return false
        return true
    }

    private fun byteIndexOfFrom(data: ByteArray, element: Byte, from: Int): Int {
        for (i in from until data.size) if (data[i] == element) return i
        return -1
    }

    // ------------------------------------------------------------------
    // Stream scan (faithful port of scan_streams)
    // ------------------------------------------------------------------

    private class Stream(val start: Int, val consumed: Int, val payload: ByteArray) {
        var vermagic: String = ""
        var sha: String = ""
        var kind: String = "unknown"
        var kmi: String = ""
        var how: String = ""
    }

    private fun consumedBytes(data: ByteArray, off: Int): Int {
        val inf = Inflater(true)
        inf.setInput(data, off, data.size - off)
        val buf = ByteArray(64 shl 20)
        return try {
            inf.inflate(buf)
            data.size - off - inf.remaining
        } finally {
            inf.end()
        }
    }

    private fun scanStreams(data: ByteArray): List<Stream> {
        val out = ArrayList<Stream>()
        var off = 0
        val n = data.size - 64
        while (off < n) {
            // First deflate byte: only dynamic blocks (BTYPE=2) are candidates
            val b = data[off].toInt() and 7
            if (b == 4 || b == 5) {
                val head = inflateRaw(data, off, 262144, 4096)
                if (head != null && head.size >= 4 && head[0] == 0x7f.toByte() &&
                    head[1] == 'E'.code.toByte() && head[2] == 'L'.code.toByte() &&
                    head[3] == 'F'.code.toByte()) {
                    val full = inflateRaw(data, off, data.size - off, 64 shl 20)
                    if (full != null) {
                        val used = consumedBytes(data, off)
                        out.add(Stream(off, used, full))
                        off += used
                        continue
                    }
                }
            }
            off++
        }
        return out
    }

    // ------------------------------------------------------------------
    // Classification (vermagic / hash / probes)
    // ------------------------------------------------------------------

    private val MAJOR_TO_KMIS = mapOf(
        "5.10" to listOf("android12-5.10", "android13-5.10"),
        "5.15" to listOf("android13-5.15", "android14-5.15"),
        "6.1" to listOf("android14-6.1"),
        "6.6" to listOf("android15-6.6"),
        "6.12" to listOf("android16-6.12"),
        "6.18" to listOf("android17-6.18"))

    private fun vermagicOf(payload: ByteArray): String {
        val needle = "vermagic=".toByteArray()
        var i = 0
        while (i + needle.size < payload.size) {
            if (payload[i] == needle[0] && containsBytes(payload, needle, i)) {
                val end = byteIndexOfFrom(payload, 0.toByte(), i + needle.size)
                    .let { if (it < 0) payload.size else it }
                val vm = payload.copyOfRange(i + needle.size, minOf(end, i + needle.size + 120))
                    .toString(Charsets.ISO_8859_1)
                if (Regex("^\\d+\\.\\d+\\.\\d+").containsMatchIn(vm) && vm.contains("mod_unload")) return vm
            }
            i++
        }
        return ""
    }

    private fun kmiFromVermagic(vm: String): String {
        val first = vm.split(" ").firstOrNull() ?: ""
        val major = Regex("^(\\d+\\.\\d+)").find(first)?.groupValues?.get(1) ?: ""
        val cands = MAJOR_TO_KMIS[major] ?: return ""
        if (cands.size == 1) return cands[0]
        for (c in cands) {
            val tag = Regex("^android(\\d+)-").find(c)?.groupValues?.get(1) ?: continue
            if (first.contains("android$tag")) return c
        }
        return "" // 5.10/5.15 without tag -> hash resolution (reference)
    }

    /** Only the KernelSU module may be replaced. Other embedded modules
     * (notably susfs_guard_lkm since Next 3.4.2) share the same vermagic. */
    private fun isKernelSuModule(payload: ByteArray): Boolean =
        containsBytes(payload, "\u0000name=kernelsu\u0000".toByteArray())

    private fun classify(streams: MutableList<Stream>, refHashes: Map<String, String>) {
        for (s in streams) {
            s.vermagic = vermagicOf(s.payload)
            s.sha = sha256Hex(s.payload)
            if (s.vermagic.isNotEmpty()) {
                if (!isKernelSuModule(s.payload)) {
                    s.kind = "other-ko"
                    continue
                }
                s.kind = "ko"
                // Exact release hashes take priority over the kernel's
                // version string, which can omit the Android KMI tag.
                val reference = refHashes[s.sha]
                if (reference != null) { s.kmi = reference; s.how = "reference" }
                else {
                    val k = kmiFromVermagic(s.vermagic)
                    if (k.isNotEmpty()) { s.kmi = k; s.how = "vermagic" }
                }
            } else {
                s.kind = when {
                    containsBytes(s.payload, "BusyBox".toByteArray()) -> "busybox"
                    containsBytes(s.payload, "Replaced module vermagic".toByteArray()) -> "ksuinit"
                    containsBytes(s.payload, "Hello, KernelSU".toByteArray()) -> "ksuinit"
                    containsBytes(s.payload, "bootctl".toByteArray()) -> "bootctl"
                    else -> "unknown"
                }
            }
        }
    }

    private fun sha256Hex(b: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    // ------------------------------------------------------------------
    // Downloads
    // ------------------------------------------------------------------

    private fun httpGet(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "UniRoot-NotStable")
        conn.connect()
        if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode} $url")
        return conn.inputStream.use { it.readBytes() }
    }

    /** Latest release: (tag, repo, raw json) for the selected flavor. */
    private fun fetchLatest(classic: Boolean): Triple<String, String, String> {
        val repos = if (classic) REPOS_CLASSIC else REPOS_NEXT
        for (repo in repos) {
            runCatching {
                val json = String(httpGet("https://api.github.com/repos/$repo/releases/latest"))
                val tag = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
                if (!tag.isNullOrBlank()) return Triple(tag, repo, json)
            }
        }
        throw IllegalStateException("GitHub API unreachable")
    }

    private fun downloadKsud(tag: String, repo: String, progress: (Int, String) -> Unit): ByteArray {
        progress(20, "Downloading the official ksud…")
        say("[*] Downloading the official ksud…")
        val ksud = httpGet("https://github.com/$repo/releases/download/$tag/ksud-aarch64-linux-android")
        say("    ${ksud.size} bytes")
        return ksud
    }

    // ------------------------------------------------------------------
    // Main entry
    // ------------------------------------------------------------------

    /**
     * Downloads + patches + registers the result. Returns the final message.
     * [onLog] receives every step; [onProgress] drives the progress popup.
     */
    fun run(
        context: Context,
        classic: Boolean = false,
        onLog: (String) -> Unit,
        onProgress: (Int, String) -> Unit = { _, _ -> },
    ): String {
        log = onLog
        val progress: (Int, String) -> Unit = { pct, stage -> onProgress(pct, stage) }
        val flavorName = if (classic) "KernelSU classic" else "KernelSU Next"
        progress(5, "Looking for the latest release…")
        say("[*] Looking for the latest $flavorName release…")
        val (tag, repo, releaseJson) = fetchLatest(classic)
        say("[*] Latest release: $tag ($repo)")

        val version = tag.removePrefix("v")
        if (!classic) {
            requireMatchingNextModules(version)
            say("[*] Samsung kernel modules: v$NEXT_MODULE_VERSION (UAPI 5)")
            say("[!] Use the KernelSU Next 3.4.2 manager (UAPI 5); an older loaded module changes after reboot and re-root.")
        }
        // The release's manager APK name carries the VERSION CODE
        // (KernelSU_Next_v3.4.0_33294-release.apk) -> id = "3.4.0-33294".
        val code = Regex("""KernelSU(?:_Next)?_v[\d.]+_(\d+)-release\.apk""").find(releaseJson)
            ?.groupValues?.get(1)
        val niceName = if (code != null) "$version-$code" else "$version-samsung"

        val ksud = downloadKsud(tag, repo, progress)
        val patched = patchKsud(context, ksud, tag, classic, progress)

        return if (classic) {
            // CLASSIC: the engine stages the SELECTED classic profile
            // (ksud-classic-latest, see KsudClassicProfiles); the tag marker
            // drives "you are already updated". Profile name = version-code.
            File(context.filesDir, "ksud-classic-latest").writeBytes(patched)
            File(context.filesDir, "ksud-classic-latest.tag").writeText(tag)
            KsudClassicProfiles.selectLatest(context, niceName,
                "official ksud $tag + Samsung KDP+DEFEX kos - patched on device")
            say("[+] Classic profile '$niceName' saved and SELECTED (ksud-classic-latest)")
            progress(100, "Done: $tag patched -> classic profile '$niceName'")
            "OK: $tag patched -> classic profile '$niceName'"
        } else {
            val profileId = niceName
            if (alreadyUpdatedNext(context, profileId, patched)) {
                KsudNextProfiles.setSelected(context, profileId)
                progress(100, "you are already updated")
                say("you are already updated")
                say("    ($tag is already patched: profile '$profileId')")
                return "you are already updated ($tag)"
            }
            KsudNextProfiles.addDynamic(context, profileId, patched,
                "official ksud $tag + Samsung KDP+DEFEX modules v$NEXT_MODULE_VERSION (UAPI 5)")
            say("[+] Profile '$profileId' created and SELECTED")
            progress(100, "Done")
            "OK: $tag patched -> profile '$profileId'"
        }
    }

    private fun alreadyUpdatedNext(context: Context, profileId: String, patched: ByteArray): Boolean =
        profileMatches(File(KsudNextProfiles.dynamicDir(context), profileId), patched)

    /** A release ID alone cannot identify which Samsung module bundle it contains. */
    internal fun profileMatches(file: File, patched: ByteArray): Boolean = runCatching {
        file.isFile && file.length() == patched.size.toLong() && file.readBytes().contentEquals(patched)
    }.getOrDefault(false)

    /** Avoid silently combining a future daemon with this build's older modules. */
    internal fun requireMatchingNextModules(version: String) {
        check(version.removePrefix("v") == NEXT_MODULE_VERSION) {
            "No matching Samsung modules for $version. This UniRoot build includes v$NEXT_MODULE_VERSION modules; update UniRoot first."
        }
    }

    // ------------------------------------------------------------------
    // The COMMON patch pipeline (scan -> inject -> verify)
    // ------------------------------------------------------------------

    private fun patchKsud(
        context: Context, ksud: ByteArray, tag: String, classic: Boolean,
        progress: (Int, String) -> Unit,
    ): ByteArray {
        val repo = if (classic) REPOS_CLASSIC[0] else REPOS_NEXT[0]
        val refBase = "https://github.com/$repo/releases/download/$tag"

        // References: the official kos OF THIS RELEASE (hash resolution of
        // the ambiguous 5.10/5.15 slots). Best-effort: an unresolved slot is
        // skipped (ksud loads the ko of its OWN kmi anyway).
        progress(40, "Downloading reference kos…")
        say("[*] Downloading official kos of $tag (reference)…")
        val refHashes = HashMap<String, String>()
        for (kmi in KMIS) {
            val bytes = if (classic) {
                runCatching { httpGet("$refBase/lkm-aarch64-${kmi}_kernelsu.ko") }
                    .recoverCatching { httpGet("$refBase/aarch64_${kmi}_kernelsu.ko") }
                    .recoverCatching { httpGet("$refBase/aarch64-${kmi}_kernelsu.ko") }
                    .recoverCatching { httpGet("$refBase/${kmi}_kernelsu.ko") }
                    .getOrNull()
            } else {
                runCatching { httpGet("$refBase/aarch64_${kmi}_kernelsu.ko") }
                    .recoverCatching { httpGet("$refBase/aarch64-${kmi}_kernelsu.ko") }
                    .recoverCatching { httpGet("$refBase/${kmi}_kernelsu.ko") }
                    .getOrNull()
            }
            if (bytes != null) refHashes[sha256Hex(bytes)] = kmi
        }
        say("    ${refHashes.size}/8 references loaded")

        // The Samsung KDP+DEFEX kos bundled in the APK - matched by the KMI
        // IN THE FILE NAME (android16-6.12_kernelsu-XXX.ko accepted), like
        // patch-ksud-next.py does.
        val koDir = if (classic) KO_ASSET_DIR_CLASSIC else KO_ASSET_DIR
        val koAssets = runCatching { context.assets.list(koDir)?.toList() ?: emptyList() }
            .getOrDefault(emptyList())
        val replacements = HashMap<String, ByteArray>()
        for (kmi in KMIS) {
            val name = koAssets.firstOrNull { it.startsWith("${kmi}_kernelsu") && it.endsWith(".ko") }
                ?: throw IllegalStateException("Samsung ko missing from the APK: $kmi")
            val raw = context.assets.open("$koDir/$name").use { it.readBytes() }
            if (!classic) {
                check(containsBytes(raw, "\u0000v$NEXT_MODULE_VERSION\u0000".toByteArray())) {
                    "Samsung module $kmi is not v$NEXT_MODULE_VERSION; update UniRoot."
                }
            }
            replacements[kmi] = raw
        }

        return patchAssets(ksud, replacements, refHashes, progress, requireAllModules = !classic)
    }

    /** Pure binary transformation, also used by the JVM regression tests. */
    internal fun patchAssets(
        ksud: ByteArray,
        replacements: Map<String, ByteArray>,
        refHashes: Map<String, String>,
        progress: (Int, String) -> Unit,
        requireAllModules: Boolean = false,
    ): ByteArray {

        progress(55, "Scanning embedded assets…")
        say("[*] Patching (deflate scan + in-place injection)…")
        val data = ksud.copyOf()
        val streams = scanStreams(data).toMutableList()
        classify(streams, refHashes)
        say("    ${streams.size} embedded assets found")

        val slots = HashMap<String, Stream>()
        for (s in streams) if (s.kind == "ko" && s.kmi.isNotEmpty()) {
            check(s.kmi !in slots) { "two slots ${s.kmi}" }
            slots[s.kmi] = s
        }

        class Plan(val kmi: String, val slot: Stream, val comp: ByteArray, val raw: ByteArray)
        val plan = ArrayList<Plan>()
        val skipped = ArrayList<String>()
        for (kmi in replacements.keys.sorted()) {
            val raw = replacements[kmi]!!
            check(raw.size >= 4 && raw[0] == 0x7f.toByte()) { "$kmi is not an ELF" }
            val slot = slots[kmi]
            if (slot == null) {
                skipped.add(kmi)
                say("[!] slot $kmi not found/ambiguous - skipped (harmless: ksud loads the ko of its own kmi)")
                continue
            }
            val comp = compressBest(raw, slot.consumed)
                ?: throw IllegalStateException("$kmi does not compress below ${slot.consumed} bytes")
            plan.add(Plan(kmi, slot, comp, raw))
        }
        check(plan.isNotEmpty()) { "no patchable slot in this ksud" }
        check(!requireAllModules || skipped.isEmpty()) {
            "Missing Samsung module slots: ${skipped.joinToString()}. Check reference downloads; no profile was saved."
        }

        progress(75, "Injecting Samsung kos…")
        for (p in plan) {
            val pad = p.slot.consumed - p.comp.size
            p.comp.copyInto(data, p.slot.start)
            java.util.Arrays.fill(data, p.slot.start + p.comp.size, p.slot.start + p.slot.consumed, 0)
            say("[+] ${p.kmi} @0x${Integer.toHexString(p.slot.start)}: ${p.slot.consumed} -> ${p.comp.size} bytes + $pad pad")
        }

        // VERIFY: re-scan of the output, exactly what ksud does at runtime.
        progress(90, "Verifying…")
        say("[*] Verifying (re-scanning the output)…")
        val outStreams = scanStreams(data)
        val byStart = outStreams.associateBy { it.start }
        var ok = outStreams.size == streams.size
        for (p in plan) {
            val got = byStart[p.slot.start]
            if (got == null || !got.payload.contentEquals(p.raw)) { ok = false; say("[!] ${p.kmi}: content mismatch") }
        }
        val patchedStarts = plan.map { it.slot.start }.toSet()
        for (a in streams) {
            if (a.start in patchedStarts) continue
            val got = byStart[a.start]
            if (got == null || !got.payload.contentEquals(a.payload)) { ok = false; say("[!] untouched asset modified @0x${Integer.toHexString(a.start)}") }
        }
        check(ok) { "verification failed - binary not reliable" }
        say("[OK] ${plan.size} injected, ${streams.size - plan.size} preserved")
        return data
    }
}
