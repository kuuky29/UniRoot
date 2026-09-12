package com.uniroot.app.autoroot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.uniroot.app.R
import com.uniroot.app.engine.RootEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Auto-root on boot: BOOT_COMPLETED -> foreground service -> re-run the last
 * used profile once per fresh boot (boot-count guarded). The single ongoing
 * notification is a live activity (Android 16 ProgressStyle -> Samsung Now
 * Bar / Live notifications) tracking the root stages, with a Stop action.
 * While the exploit runs, toasts guide the user: don't touch the phone,
 * pull down the notification panel for live progress.
 */
class AutoRootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val engine = RootEngine(context.applicationContext)
        engine.initialize()
        if (!engine.autoRootOnBoot) return
        val profileName = engine.lastRunProfile() ?: return
        if (engine.profileByName(profileName) == null) return
        // Once per boot.
        val bootCount = runCatching {
            AndroidSettings.Global.getInt(context.contentResolver, AndroidSettings.Global.BOOT_COUNT, 0)
        }.getOrDefault(0)
        if (engine.autoRootBootCount() == bootCount) return

        val service = Intent(context, AutoRootService::class.java)
        runCatching { context.startForegroundService(service) }
    }
}

class AutoRootService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var engine: RootEngine

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRequested = true
            hideOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        stopRequested = false
        // Settle phase: the user CAN touch the phone — one guiding toast only.
        runCatching {
            Toast.makeText(this, getString(R.string.autoroot_starting_soon), Toast.LENGTH_LONG).show()
        }
        postLive(5, getString(R.string.autoroot_preparing), withStop = true)
        startForeground(NOTIF_ID, lastBuilt!!)
        engine = RootEngine(applicationContext)
        engine.initialize()
        val profileName = engine.lastRunProfile()
        val profile = engine.profileByName(profileName)
        if (profile == null) { stopSelf(); return START_NOT_STICKY }

        // Once-per-boot guard (recorded BEFORE running so a crash also counts).
        val bootCount = runCatching {
            AndroidSettings.Global.getInt(contentResolver, AndroidSettings.Global.BOOT_COUNT, 0)
        }.getOrDefault(0)
        if (engine.autoRootBootCount() == bootCount) { stopSelf(); return START_NOT_STICKY }
        engine.setAutoRootBootCount(bootCount)

        val needsShizuku = profile.name.startsWith("S26")
        scope.launch {
            var status = "Crash"
            try {
                updateLive(8, getString(R.string.autoroot_settling))
                // Let the system finish booting (selinux loads, vendors up, slab
                // state sane). The payload itself waits for an allocator quiet
                // window — starting later lets it find one immediately.
                if (!waitWithCancel(90_000L)) { stopSelf(); return@launch }

                if (needsShizuku) {
                    updateLive(12, getString(R.string.autoroot_waiting_shizuku))
                    if (!waitForShizuku(timeoutMs = 5 * 60_000L)) {
                        postResult(getString(R.string.autoroot_shizuku_missing), success = false)
                        return@launch
                    }
                    if (!engine.shizukuPermissionGranted()) {
                        postResult(getString(R.string.autoroot_permission_missing), success = false)
                        return@launch
                    }
                }

                // Flavor guard: only block when a DIFFERENT flavor's module is
                // already loaded (switching then would contaminate the boot).
                // A phone with NO root at boot always runs normally.
                if (engine.ksuModuleLoaded()) {
                    val loaded = engine.loadedKsuFlavor()
                    if (loaded != null && loaded != profile.flavor) {
                        hideOverlay()
                        postResult(getString(R.string.autoroot_flavor_changed), success = false)
                        return@launch
                    }
                }

                // Exploit is launching: from here on the phone must NOT be touched.
                showOverlay(getString(R.string.autoroot_overlay_running))
                val toastSpam = launch {
                    var i = 0
                    while (true) {
                        val msg = if (i < 4) getString(R.string.autoroot_dont_touch)
                        else getString(R.string.autoroot_check_notif)
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                        i++
                        delay(if (i >= 5) 3_000L else 4_000L)
                        if (i >= 5) i = 0
                    }
                }
                updateLive(20, getString(R.string.autoroot_running, profile.name))
                engine.clearLogs()
                engine.progressListener = { p, label -> updateLive(p, "Auto-root: $label") }
                var runAttempt = 0
                while (runAttempt < 3 && !stopRequested) {
                    if (runAttempt > 0) {
                        updateLive(10, getString(R.string.autoroot_retrying, runAttempt))
                        if (!waitWithCancel(60_000L)) break
                    }
                    engine.clearLogs()
                    status = engine.runExecutionPipeline(profile, needsShizuku)
                    if (status == "Success" || status == "Reboot required") break
                    runAttempt++
                }
                toastSpam.cancel()

                withContext(Dispatchers.Main) {
                    val msg = if (status == "Success") getString(R.string.autoroot_run_success)
                    else getString(R.string.autoroot_run_failed)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                }
                if (status == "Success") {
                    engine.refreshRootedLive()
                    postResult(getString(R.string.autoroot_run_success), success = true)
                } else {
                    postResult(getString(R.string.autoroot_run_failed), success = false)
                }
            } catch (e: Exception) {
                postResult("Auto-root error: ${e.message}", success = false)
            } finally {
                hideOverlay()
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /** Waits [totalMs] in 1 s steps; returns false as soon as the user pressed Stop. */
    private suspend fun waitWithCancel(totalMs: Long): Boolean {
        var waited = 0L
        while (waited < totalMs) {
            if (stopRequested) return false
            delay(1_000L)
            waited += 1_000L
        }
        return !stopRequested
    }

    private suspend fun waitForShizuku(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (stopRequested) return false
            if (Shizuku.pingBinder()) {
                if (engine.shizukuPermissionGranted()) return true
            }
            delay(5_000L)
        }
        return false
    }

    private var overlayView: View? = null
    private var liveProgress = 0
    private var lastBuilt: Notification? = null

    /** Always-on-top banner shown only while the exploit is actually running. */
    private fun showOverlay(text: String) {
        if (!AndroidSettings.canDrawOverlays(this)) return
        val wm = getSystemService(WindowManager::class.java)
        overlayView?.let { runCatching { wm.removeView(it) } }
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(-0x1)
            setBackgroundColor(0xCC101820.toInt())
            gravity = Gravity.CENTER
            setPadding(32, 28, 32, 28)
            textSize = 16f
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.graphics.PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP
        runCatching { wm.addView(tv, params); overlayView = tv }
    }

    private fun hideOverlay() {
        overlayView?.let { v -> runCatching { getSystemService(WindowManager::class.java).removeView(v) } }
        overlayView = null
    }

    /** THE single live notification (FGS + progress + Stop). No second notif. */
    private fun postLive(progress: Int, text: String, withStop: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Auto-root", NotificationManager.IMPORTANCE_LOW))
        }
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Uni-Root — auto-root")
            .setContentText(text)
            .setOngoing(true)
        if (android.os.Build.VERSION.SDK_INT >= 36) {
            builder.setStyle(Notification.ProgressStyle().setProgress(progress.coerceIn(0, 100)))
        } else {
            builder.setProgress(100, progress.coerceIn(0, 100), progress in 1..99)
        }
        if (withStop) {
            val stopIntent = PendingIntent.getService(
                this, 0,
                Intent(this, AutoRootService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    getString(R.string.autoroot_stop),
                    stopIntent,
                ).build(),
            )
        }
        val notif = builder.build()
        lastBuilt = notif
        runCatching { manager.notify(NOTIF_ID, notif) }
    }

    private fun updateLive(progress: Int, text: String) {
        liveProgress = progress.coerceIn(0, 100)
        postLive(liveProgress, text, withStop = true)
    }

    /** Writes the final result notification (stays until swiped away). */
    private fun postResult(text: String, success: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Auto-root", NotificationManager.IMPORTANCE_LOW))
        }
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (success) "Uni-Root — rooted" else "Uni-Root — auto-root failed")
            .setContentText(text)
            .setAutoCancel(true)
        runCatching { manager.notify(NOTIF_ID + 1, builder.build()) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "auto_root"
        private const val NOTIF_ID = 41
        const val ACTION_STOP = "com.uniroot.app.autoroot.STOP"
        @Volatile var stopRequested = false
    }
}
