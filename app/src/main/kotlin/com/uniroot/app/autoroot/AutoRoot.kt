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
 * used profile once. This is exactly the validated LKM late-load usage: ONE
 * run per fresh boot. The service waits for the system to settle and, for
 * Shizuku profiles, for the Shizuku binder to come back (wireless debugging
 * auto-start). A boot-count guard prevents double runs, and the notification
 * carries a Stop action to abort the automatic launch at any moment.
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        showOverlay(getString(R.string.autoroot_overlay))
        startAsForeground(getString(R.string.autoroot_preparing))
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
            // Live "Don't touch the phone" spam at the bottom of the screen —
            // visible over everything, so no need to open the notifications.
            val toastSpam = launch(Dispatchers.Main) {
                while (true) {
                    Toast.makeText(applicationContext,
                        getString(R.string.autoroot_dont_touch), Toast.LENGTH_SHORT).show()
                    delay(5_000L)
                }
            }
            try {
                notify(getString(R.string.autoroot_settling))
                // Let the system finish booting (selinux loads, vendors up, slab
                // state sane). The payload itself waits for an allocator quiet
                // window — starting later lets it find one immediately.
                if (!waitWithCancel(90_000L)) { stopSelf(); return@launch }

                if (needsShizuku) {
                    notify(getString(R.string.autoroot_waiting_shizuku))
                    // Shizuku auto-start (wireless debugging) can take a while after boot.
                    if (!waitForShizuku(timeoutMs = 5 * 60_000L)) {
                        notifyFail(getString(R.string.autoroot_shizuku_missing))
                        return@launch
                    }
                    if (!engine.shizukuPermissionGranted()) {
                        notifyFail(getString(R.string.autoroot_permission_missing))
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
                        notifyFail(getString(R.string.autoroot_flavor_changed))
                        return@launch
                    }
                }

                showOverlay(getString(R.string.autoroot_overlay_running))
                showOverlay(getString(R.string.autoroot_overlay_running))
                notify(getString(R.string.autoroot_running, profile.name))
                engine.clearLogs()
                status = engine.runExecutionPipeline(profile, needsShizuku)
                withContext(Dispatchers.Main) {
                    if (status == "Success") {
                        engine.refreshRootedLive()
                        Toast.makeText(applicationContext,
                            getString(R.string.autoroot_run_success), Toast.LENGTH_LONG).show()
                        notifyOk(getString(R.string.autoroot_success))
                    } else {
                        Toast.makeText(applicationContext,
                            getString(R.string.autoroot_run_failed), Toast.LENGTH_LONG).show()
                        notifyFail(getString(R.string.autoroot_result, status))
                    }
                }
            } catch (e: Exception) {
                notifyFail("Auto-root error: ${e.message}")
            } finally {
                toastSpam.cancel()
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

    /** Always-on-top banner: "DON'T TOUCH THE PHONE" during the whole auto-root. */
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
        runCatching { wm.addView(tv, params) ; overlayView = tv }
    }

    private fun hideOverlay() {
        overlayView?.let { v -> runCatching { getSystemService(WindowManager::class.java).removeView(v) } }
        overlayView = null
    }

    private fun buildNotification(text: String, withStop: Boolean): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Auto-root", NotificationManager.IMPORTANCE_LOW))
        }
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Uni-Root")
            .setContentText(text)
            .setOngoing(true)
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
        return builder.build()
    }

    private fun startAsForeground(text: String) {
        runCatching { startForeground(NOTIF_ID, buildNotification(text, withStop = true)) }
    }

    private fun notify(text: String) {
        notifyInternal(text, withStop = true)
    }

    private fun notifyInternal(text: String, withStop: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Auto-root", NotificationManager.IMPORTANCE_LOW))
        }
        runCatching { manager.notify(NOTIF_ID + 1, buildNotification(text, withStop)) }
    }

    private fun notifyOk(text: String) = notify(text)
    private fun notifyFail(text: String) = notify(text)

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
