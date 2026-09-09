package com.uniroot.app.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub side of the app.
 *
 * - Root My Galaxy: reads the official support feed (targets-v3.json) from the
 *   Root-My-Galaxy-Payloads repository, matches the local device and downloads the
 *   pinned exploit + ksud artifacts, exactly like the Root My Galaxy app does.
 * - KernelSU / KernelSU Next: downloads the latest official ksud binary and the
 *   prebuilt LKM (.ko) matching the running kernel KMI from the selected flavor.
 */
class GitHubSupport(private val context: Context) {

    // ---------------------------------------------------------------------
    // Root My Galaxy support feed
    // ---------------------------------------------------------------------

    fun fetchRmgTargets(): List<RmgTarget> {
        val commit = downloadText(RMG_COMMIT_API, MAX_COMMIT_RESPONSE_BYTES)
            .let { JSONObject(it).getJSONObject("object").getString("sha") }
        require(commit.matches(Regex("[0-9a-f]{40}"))) { "Invalid RMG commit" }
        val manifest = downloadText("$RMG_RAW/$commit/support/targets-v3.json", MAX_MANIFEST_BYTES)
        val root = JSONObject(manifest)
        require(root.getInt("schemaVersion") == 3) { "Unsupported RMG feed schema" }
        val payloads = root.getJSONArray("payloads")
        return buildList {
            for (i in 0 until payloads.length()) {
                runCatching {
                    val p = payloads.getJSONObject(i)
                    val exploit = p.getJSONObject("exploit")
                    val kernelsu = p.getJSONObject("kernelsu")
                    add(RmgTarget(
                        payloadId = p.getString("payloadId"),
                        displayName = p.getString("displayName"),
                        models = p.getJSONArray("models").toStringList(),
                        kernelVersions = p.getJSONArray("kernelVersions").toStringList(),
                        exploitUrl = pin(exploit.getString("url"), commit),
                        exploitSize = exploit.getLong("size"),
                        ksudUrl = pin(kernelsu.getString("url"), commit),
                        ksudSize = kernelsu.getLong("size"),
                    ))
                }
            }
        }
    }

    /** Same matching rules as Root My Galaxy: exact model + exact three-part kernel version. */
    fun matchRmgTarget(targets: List<RmgTarget>, model: String, kernelVersion: String): RmgTarget? =
        targets.firstOrNull { t ->
            t.models.any { it.equals(model, ignoreCase = true) } && t.kernelVersions.contains(kernelVersion)
        }

    /**
     * Downloads the exploit (cve-2026-43499-app.so) and the KernelSU daemon of a
     * matched Root My Galaxy payload and turns them into a usable device profile
     * (Local mode: app payload run through the root helper, ksud late-load embeds
     * its own kernel module, so the .ko path reuses the ksud file).
     */
    fun downloadRmgProfile(target: RmgTarget): DeviceProfile {
        val dir = File(File(context.filesDir, "rmg"), target.payloadId).apply { mkdirs() }
        val exploit = downloadArtifact(target.exploitUrl, target.exploitSize, File(dir, "cve-2026-43499-app.so"))
        val ksud = downloadArtifact(target.ksudUrl, target.ksudSize, File(dir, "ksud"))
        return DeviceProfile(
            name = "RMG · ${target.displayName}",
            kaslrOffset = "",
            pathSo = exploit.absolutePath,
            pathKo = ksud.absolutePath,
            pathKsud = ksud.absolutePath,
            deviceType = "samsung",
            pathCveNormal = null,
            pathCveRoot = null,
        )
    }

    // ---------------------------------------------------------------------
    // KernelSU / KernelSU Next latest artifacts
    // ---------------------------------------------------------------------

    data class KsuLatest(
        val tag: String,
        val ksudFile: File,
        val koFile: File?,
    )

    /**
     * Downloads the latest ksud (aarch64-linux-android) and the LKM matching [kmi]
     * (e.g. "android15-6.6") from the official KernelSU releases:
     * "ksud-aarch64-linux-android" and "lkm-aarch64-<kmi>_kernelsu.ko".
     */
    fun fetchLatestKsu(kmi: String, destDir: File, log: (String) -> Unit): KsuLatest {
        val release = JSONObject(downloadText("https://api.github.com/repos/tiann/kernelSU/releases/latest", MAX_RELEASE_BYTES))
        val tag = release.getString("tag_name")
        val assets = release.getJSONArray("assets")
        var ksudUrl: String? = null
        var koUrl: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            when (a.getString("name")) {
                "ksud-aarch64-linux-android" -> ksudUrl = a.getString("browser_download_url")
                "lkm-aarch64-${kmi}_kernelsu.ko" -> koUrl = a.getString("browser_download_url")
            }
        }
        requireNotNull(ksudUrl) { "No ksud-aarch64-linux-android asset in $tag" }
        destDir.mkdirs()
        val ksudFile = downloadArtifact(ksudUrl, -1L, File(destDir, "ksud-kernelsu-latest"))
        log("[KernelSU] Downloaded ksud from KernelSU $tag")
        var koFile: File? = null
        if (koUrl != null) {
            koFile = downloadArtifact(koUrl, -1L, File(destDir, "kernelsu-kernelsu-latest.ko"))
            log("[KernelSU] Downloaded module $kmi from KernelSU $tag")
        } else {
            log("[KernelSU] KernelSU $tag publishes no prebuilt module for $kmi; keeping current .ko")
        }
        return KsuLatest(tag, ksudFile, koFile)
    }

    /** "6.6.127-android15-8-…" → "android15-6.6" (the GKI KMI used in asset names). */
    fun kmiFromKernel(kernel: String): String {
        val m = Regex("android(\\d+)-(\\d+)\\.(\\d+)").find(kernel)
            ?: return ""
        return "android${m.groupValues[1]}-${m.groupValues[2]}.${m.groupValues[3]}"
    }

    /**
     * Downloads the official KernelSU manager APK from the latest GitHub
     * release ("KernelSU_v…-release.apk") so the full manager is one tap away.
     */
    fun downloadManagerApk(destDir: File): File {
        val release = JSONObject(downloadText("https://api.github.com/repos/tiann/kernelSU/releases/latest", MAX_RELEASE_BYTES))
        val tag = release.getString("tag_name")
        val assets = release.getJSONArray("assets")
        val apks = mutableListOf<Pair<String, String>>() // name → url
        for (i in 0 until assets.length()) {
            val name = assets.getJSONObject(i).getString("name")
            if (name.endsWith(".apk")) apks.add(name to assets.getJSONObject(i).getString("browser_download_url"))
        }
        val apkUrl = apks.firstOrNull { it.first.matches(Regex("KernelSU_v[0-9].*-release\\.apk")) }?.second
            ?: apks.firstOrNull { !it.first.contains("debug", ignoreCase = true) }?.second
        requireNotNull(apkUrl) { "No manager APK in $tag" }
        destDir.mkdirs()
        return downloadArtifact(apkUrl, -1L, File(destDir, "manager-kernelsu.apk"))
    }

    /** Leading three-part kernel version, same extraction as Root My Galaxy. */
    fun kernelThreePart(kernel: String): String {
        val m = Regex("(\\d+\\.\\d+\\.\\d+)").find(kernel) ?: return ""
        return m.groupValues[1]
    }

    // ---------------------------------------------------------------------
    // HTTP helpers
    // ---------------------------------------------------------------------

    private fun pin(url: String, commit: String): String {
        require(url.startsWith(RMG_RAW_MAIN)) { "Unexpected RMG artifact host" }
        return "$RMG_RAW/$commit/${url.removePrefix(RMG_RAW_MAIN)}"
    }

    private fun downloadArtifact(url: String, expectedSize: Long, dest: File): File {
        val tmp = File(dest.parentFile, dest.name + ".part")
        val conn = open(url)
        conn.inputStream.use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (expectedSize in 1..total) { conn.disconnect(); require(false) { "Artifact larger than expected" } }
                    output.write(buffer, 0, n)
                }
                output.fd.sync()
                if (expectedSize >= 0 && total != expectedSize) { conn.disconnect(); require(false) { "Incomplete download ($total/$expectedSize)" } }
            }
        }
        conn.disconnect()
        if (dest.exists()) dest.delete()
        require(tmp.renameTo(dest)) { "Cannot finalize ${dest.name}" }
        dest.setReadable(true, false)
        dest.setExecutable(true, false)
        return dest
    }

    private fun downloadText(url: String, maximum: Int): String {
        val conn = open(url)
        val bytes = conn.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                require(output.size() + n <= maximum) { "Response too large" }
                output.write(buffer, 0, n)
            }
            output.toByteArray()
        }
        conn.disconnect()
        return bytes.toString(Charsets.UTF_8)
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "UniRoot/${unirootVersionName(context)}")
            connect()
            require(responseCode == HttpURLConnection.HTTP_OK) { "HTTP $responseCode for $url" }
        }

    private fun unirootVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (i in 0 until length()) add(getString(i))
    }

    companion object {
        private const val RMG_COMMIT_API = "https://api.github.com/repos/BuSung-dev/Root-My-Galaxy-Payloads/git/ref/heads/main"
        private const val RMG_RAW = "https://raw.githubusercontent.com/BuSung-dev/Root-My-Galaxy-Payloads"
        private const val RMG_RAW_MAIN = "$RMG_RAW/main/"
        private const val MAX_COMMIT_RESPONSE_BYTES = 16 * 1024
        private const val MAX_MANIFEST_BYTES = 256 * 1024
        private const val MAX_RELEASE_BYTES = 512 * 1024
    }
}
