package com.uniroot.app.engine

data class DeviceProfile(
    val name: String, val kaslrOffset: String, val pathSo: String, val pathKo: String, val pathKsud: String,
    val deviceType: String, val pathCveNormal: String?, val pathCveRoot: String?,
    /** "kernelsu" (default) or "kernelsu_next" — drives the home-page switch + manager relaunch. */
    val flavor: String = "kernelsu",
)

data class DeviceInfo(
    val model: String,
    val kernel: String,
    val soc: String,
    val matchedProfileName: String?,
)

/** One entry of the Root My Galaxy support feed (targets-v3.json). */
data class RmgTarget(
    val payloadId: String,
    val displayName: String,
    val models: List<String>,
    val kernelVersions: List<String>,
    val exploitUrl: String,
    val exploitSize: Long,
    val ksudUrl: String,
    val ksudSize: Long,
)

/** One saved run log box (Root My Galaxy style): timestamp + final status, shareable as .txt. */
data class RunLogFile(
    val file: java.io.File,
    val timeMillis: Long,
    val status: String,
)
