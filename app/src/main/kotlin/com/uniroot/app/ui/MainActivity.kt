package com.uniroot.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.uniroot.app.R
import com.uniroot.app.engine.DeviceProfile
import com.uniroot.app.engine.RunLogFile
import com.uniroot.app.engine.RootEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    private lateinit var engine: RootEngine

    private var deviceName by mutableStateOf("")
    private var socName by mutableStateOf("")
    private var kernelRelease by mutableStateOf("")
    private var autoMatched by mutableStateOf(false)
    private var advancedVisible by mutableStateOf(false)
    private var shizukuEnabled by mutableStateOf(false)
    private var profiles by mutableStateOf(listOf<DeviceProfile>())
    private var selectedProfileName by mutableStateOf<String?>(null)
    private var sheetVisible by mutableStateOf(false)
    private var sheetDismissible by mutableStateOf(false)
    private var permissionTick by mutableIntStateOf(0)

    private var useLatestKsu by mutableStateOf(false)
    private var latestKsuTag by mutableStateOf("")
    private var usePatchedKsud by mutableStateOf(false)
    private var patchedKsudName by mutableStateOf("")
    private var autoRootOnBoot by mutableStateOf(false)
    private var ksuNextMode by mutableStateOf(false)
    private var rmgStatus by mutableStateOf("")
    private var tab by mutableStateOf(UniTab.UNIROOT)
    private var runLogs by mutableStateOf(listOf<RunLogFile>())
    private var logViewerFile by mutableStateOf<RunLogFile?>(null)
    private var logViewerContent by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = RootEngine(applicationContext)
        engine.initialize()
        Shizuku.addRequestPermissionResultListener(this)
        useLatestKsu = engine.useLatestKsu
        latestKsuTag = engine.latestKsuTag()
        usePatchedKsud = engine.usePatchedKsud
        patchedKsudName = engine.patchedKsudFile()?.name ?: ""
        autoRootOnBoot = engine.autoRootOnBoot
        ksuNextMode = engine.ksuFlavor == "kernelsu_next"
        runLogs = engine.listRunLogs()
        refreshDevice()
        refreshProfiles()
        checkRootMyGalaxy(silent = true)
        setContent {
            @Suppress("UNUSED_EXPRESSION") permissionTick
            val context = LocalContext.current
            val rooted by engine.rooted.collectAsState()
            val logs by engine.logLines.collectAsState()
            val running by engine.running.collectAsState()
            UniApp(
                state = UniUiState(
                    deviceName = deviceName,
                    socName = socName,
                    kernelRelease = kernelRelease,
                    kernelSupported = selectedProfileName != null,
                    rooted = rooted,
                    running = running,
                    advancedVisible = advancedVisible,
                    shizukuEnabled = shizukuEnabled,
                    profiles = profiles,
                    selectedProfileName = selectedProfileName,
                    executionSheetVisible = sheetVisible,
                    executionSheetDismissible = sheetDismissible,
                    logLines = logs,
                    useLatestKsu = useLatestKsu,
                    latestKsuTag = latestKsuTag,
                    usePatchedKsud = usePatchedKsud,
                    patchedKsudName = patchedKsudName,
                    autoRootOnBoot = autoRootOnBoot,
                    rmgStatus = rmgStatus,
                    ksuNextMode = ksuNextMode,
                    tab = tab,
                    runLogs = runLogs,
                    logViewerFile = logViewerFile,
                    logViewerContent = logViewerContent,
                ),
                actions = object : UniActions {
                    override fun onRun() = launchRun()
                    override fun onCloseExecutionSheet() { if (sheetDismissible) sheetVisible = false }
                    override fun onToggleAdvanced() { advancedVisible = !advancedVisible }
                    override fun onCopyLogs() = copyLogs()
                    override fun onTabSelected(newTab: UniTab) { tab = newTab }
                    override fun onProfileSelected(index: Int) {
                        selectedProfileName = profiles.getOrNull(index)?.name
                        autoMatched = false
                    }
                    override fun onShizukuChanged(enabled: Boolean) { shizukuEnabled = enabled }
                    override fun onKsuFlavorChanged(next: Boolean) {
                        ksuNextMode = next
                        engine.ksuFlavor = if (next) "kernelsu_next" else "kernelsu"
                        refreshProfiles()
                    }
                    override fun onResetProfiles() = resetProfiles()
                    override fun onProfileSave(profile: DeviceProfile, originalName: String?) {
                        engine.addOrUpdateProfile(profile, originalName)
                        refreshProfiles()
                        selectedProfileName = profile.name
                        Toast.makeText(context, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                    }
                    override fun onProfileDelete(name: String) {
                        engine.deleteProfile(name)
                        refreshProfiles()
                        if (selectedProfileName == name) {
                            selectedProfileName = engine.detectDevice().matchedProfileName
                                ?.takeIf { engine.profileByName(it) != null }
                        }
                    }
                    override fun onUseLatestKsuChanged(enabled: Boolean) {
                        useLatestKsu = enabled
                        engine.useLatestKsu = enabled
                        if (enabled) {
                            Toast.makeText(context, R.string.use_latest_enabled_toast, Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onDownloadLatestKsu() = downloadLatestKsu()
                    override fun onUsePatchedKsudChanged(enabled: Boolean) {
                        usePatchedKsud = enabled
                        engine.usePatchedKsud = enabled
                    }
                    override fun onAutoRootChanged(enabled: Boolean) {
                        autoRootOnBoot = enabled
                        engine.autoRootOnBoot = enabled
                        if (enabled && android.os.Build.VERSION.SDK_INT >= 33) {
                            runCatching {
                                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                            }
                        }
                    }
                    override fun onInstallPatchedKsud(file: File) {
                        val installed = engine.installPatchedKsud(file)
                        runCatching { file.delete() }
                        patchedKsudName = installed?.name ?: ""
                        Toast.makeText(context, if (installed != null) R.string.patched_ksud_installed else R.string.pick_failed, Toast.LENGTH_SHORT).show()
                    }
                    override fun onCheckRmg() = checkRootMyGalaxy(silent = false)
                    override fun onRunLogOpen(file: RunLogFile) {
                        logViewerContent = runCatching { file.file.readText() }.getOrDefault("")
                        logViewerFile = file
                    }
                    override fun onRunLogShare(file: RunLogFile) = shareRunLog(file.file)
                    override fun onRunLogDelete(file: RunLogFile) {
                        engine.deleteRunLog(file.file)
                        runLogs = engine.listRunLogs()
                        if (logViewerFile == file) { logViewerFile = null; logViewerContent = "" }
                    }
                    override fun onRunLogViewerClose() { logViewerFile = null; logViewerContent = "" }
                },
            )
        }
        setupSystemBars()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onRequestPermissionResult(requestCode: Int, result: Int) {
        permissionTick++
    }

    private fun refreshDevice() {
        val info = engine.detectDevice()
        deviceName = info.model
        socName = info.soc
        kernelRelease = info.kernel
        if (info.matchedProfileName != null && engine.profileByName(info.matchedProfileName) != null) {
            selectedProfileName = info.matchedProfileName
            autoMatched = true
        }
    }

    private fun refreshProfiles() {
        val flavor = if (ksuNextMode) "kernelsu_next" else "kernelsu"
        val suffix = if (ksuNextMode) " Next" else ""
        profiles = engine.profiles.filter { it.flavor == flavor }
        if (selectedProfileName == null || profiles.none { it.name == selectedProfileName }) {
            selectedProfileName = engine.detectDevice().matchedProfileName
                ?.let { matched -> engine.profileByName(matched + suffix)?.name ?: engine.profileByName(matched)?.name }
                ?: profiles.firstOrNull { it.name.startsWith("RMG · ") }?.name
        }
    }

    private fun checkRootMyGalaxy(silent: Boolean) {
        lifecycleScope.launch {
            rmgStatus = engine.checkRootMyGalaxy()
            refreshProfiles()
            if (!silent) Toast.makeText(this@MainActivity, rmgStatus, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * After a successful run: kill the (possibly stale) KernelSU manager so it
     * re-reads the freshly loaded module, then bring it back up.
     */
    private fun restartKsuManager() {
        // The relaunched manager follows the selected flavor: KernelSU Next
        // profiles ship real Next builds (S25: ksud-nxt-S938X, S26: ksud-nxt-S948X).
        val (installed, pkg) = engine.isKsuManagerInstalled(ksuNextMode)
        if (!installed) {
            Toast.makeText(this, R.string.manager_not_installed, Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            val stopped = engine.forceStopKsuManager(pkg)
            delay(if (stopped) 800L else 200L)
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(intent) }
            }
        }
    }

    private fun downloadLatestKsu() {
        val profile = engine.profileByName(selectedProfileName) ?: run {
            Toast.makeText(this, R.string.no_profile_selected, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = engine.fetchLatestKsuFor(profile)
            engine.appendLog("[KernelSU] $result")
            latestKsuTag = engine.latestKsuTag()
            Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetProfiles() {
        engine.resetProfiles()
        refreshProfiles()
        selectedProfileName = engine.detectDevice().matchedProfileName?.takeIf { engine.profileByName(it) != null }
        Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show()
    }

    private fun copyLogs() {
        val text = engine.logLines.value.joinToString("\n")
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("uniroot-log", text))
        Toast.makeText(this, R.string.action_copy, Toast.LENGTH_SHORT).show()
    }

    private fun shareRunLog(file: File) {
        runCatching {
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, file.name))
        }
    }

    private fun launchRun() {
        val profile = engine.profileByName(selectedProfileName) ?: return

        // S26 Ultra : via Shizuku — mais TOUJOURS de façon visible : aucun retour
        // silencieux, l'utilisateur voit exactement ce qui bloque.
        if (profile.name.startsWith("S26")) {
            if (!engine.shizukuBinderActive()) {
                engine.clearLogs()
                sheetVisible = true
                sheetDismissible = true
                engine.appendLog("[!] Shizuku binder is NOT active.")
                engine.appendLog("[!] Start Shizuku (wireless debugging) first, then run again.")
                return
            }
            if (!engine.shizukuPermissionGranted()) {
                engine.appendLog("[!] Shizuku permission missing — accepting the prompt, run again after.")
                engine.requestShizukuPermission()
                sheetVisible = true
                sheetDismissible = true
                return
            }
            shizukuEnabled = true
        }

        engine.setLastRunProfile(profile.name)
        engine.clearLogs()
        sheetVisible = true
        sheetDismissible = false
        if (autoMatched) engine.appendLog("[Mode] Profile auto-detected for this device")
        engine.appendLog("[Mode] " + (if (shizukuEnabled || profile.name.startsWith("S26")) "Shizuku (shell UID 2000)" else "Local (app context)"))

        lifecycleScope.launch {
            engine.setRunning(true)
            var status = "Crash"
            try {
                status = engine.runExecutionPipeline(profile, shizukuEnabled || profile.name.startsWith("S26"))
                engine.appendLog("[Pipeline] Finished: $status")
                runLogs = engine.listRunLogs()
                if (status == "Success") {
                    engine.refreshRootedLive()
                    // Log sheet closes itself. The manager is only touched AFTER a
                    // 20 s settle: both observed freezes happened 14 s after the
                    // late-load, during the manager force-stop + relaunch.
                    delay(1200)
                    sheetVisible = false
                    delay(19_000)
                    restartKsuManager()
                }
            } catch (e: Exception) {
                // Nothing may fail silently: any exception lands in the visible log.
                engine.appendLog("[Error] Unexpected: ${e.javaClass.simpleName}: ${e.message}")
                android.util.Log.e("UniRoot", "pipeline failed", e)
                runCatching { engine.abandonActiveRunLog() }
                runLogs = engine.listRunLogs()
            } finally {
                engine.setRunning(false)
                sheetDismissible = true
            }
        }
    }

    private fun setupSystemBars() {
        val controller = window.decorView.windowInsetsController ?: return
        val lightStatus = if (resources.getBoolean(R.bool.window_light_status_bar)) {
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        } else 0
        val lightNavigation = if (resources.getBoolean(R.bool.window_light_navigation_bar)) {
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        } else 0
        controller.setSystemBarsAppearance(
            lightStatus or lightNavigation,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
    }
}
