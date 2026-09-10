package com.uniroot.app.autoroot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import com.uniroot.app.R
import com.uniroot.app.engine.RootEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * Auto-root on boot: BOOT_COMPLETED -> foreground service -> re-run the last
 * used profile once. This is exactly the validated LKM late-load usage: ONE
 * run per fresh boot. The service waits for the system to settle and, for
 * Shizuku profiles, for the Shizuku binder to come back (wireless debugging
 * auto-start). A boot-count guard prevents double runs.
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
        startAsForeground("Auto-root: preparing…")
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
                notify("Auto-root: waiting for boot to settle…")
                // Let the system finish booting (selinux loads, vendors up, slab state sane).
                delay(45_000L)

                if (needsShizuku) {
                    notify("Auto-root: waiting for Shizuku…")
                    // Shizuku auto-start (wireless debugging) can take a while after boot.
                    val ready = waitForShizuku(timeoutMs = 5 * 60_000L)
                    if (!ready) {
                        notifyFail("Shizuku did not start — open Shizuku and run manually")
                        return@launch
                    }
                    if (!engine.shizukuPermissionGranted()) {
                        notifyFail("Shizuku permission missing — run once manually")
                        return@launch
                    }
                }

                notify("Auto-root: running \"${profile.name}\"…")
                engine.clearLogs()
                status = engine.runExecutionPipeline(profile, needsShizuku)
                if (status == "Success") {
                    engine.refreshRootedLive()
                    notifyOk("Auto-root SUCCESS — \"${profile.name}\"")
                } else {
                    notifyFail("Auto-root $status — check Uni-Root Logs page")
                }
            } catch (e: Exception) {
                notifyFail("Auto-root error: ${e.message}")
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun waitForShizuku(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (Shizuku.pingBinder()) {
                if (engine.shizukuPermissionGranted()) return true
            }
            delay(5_000L)
        }
        return false
    }

    private fun startAsForeground(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Auto-root", NotificationManager.IMPORTANCE_LOW))
        }
        val notification: Notification
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            notification = Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Uni-Root")
                .setContentText(text)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            notification = Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Uni-Root")
                .setContentText(text)
                .setOngoing(true)
                .build()
        }
        runCatching { startForeground(NOTIF_ID, notification) }
    }

    private fun notify(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Auto-root", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Uni-Root")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIF_ID + 1, notification) }
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
    }
}
