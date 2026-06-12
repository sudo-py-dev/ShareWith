package com.share.with

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class FileSharingService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopServer()
            stopSelf()
            return START_NOT_STICKY
        }

        startServer()
        return START_STICKY
    }

    private fun startServer() {
        createNotificationChannel()
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        acquireWakeLock()
        acquireWifiLock()

        val port = AppState.serverPort
        ServerManager.start(this, port) { error ->
            AppState.addLog("Error starting server: ${error.message}")
            stopSelf()
        }
    }

    private fun stopServer() {
        ServerManager.stop()
        releaseWakeLock()
        releaseWifiLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                wakeLock =
                    powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "ShareWith::FileSharingServiceWakeLock",
                    ).apply {
                        // 1 hour
                        acquire(60 * 60 * 1000L)
                    }
                AppState.addLog("Background WakeLock acquired")
            } else {
                AppState.addLog("Warning: PowerManager not available, WakeLock not acquired")
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                AppState.addLog("Background WakeLock released")
            }
        } catch (e: Exception) {
            AppState.addLog("Error releasing WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    private fun acquireWifiLock() {
        if (wifiLock == null) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val mode = WifiManager.WIFI_MODE_FULL_HIGH_PERF
                wifiLock =
                    wifiManager.createWifiLock(mode, "ShareWith::FileSharingServiceWifiLock").apply {
                        acquire()
                    }
                AppState.addLog("Background WifiLock acquired")
            } else {
                AppState.addLog("Warning: WifiManager not available, WifiLock not acquired")
            }
        }
    }

    private fun releaseWifiLock() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                AppState.addLog("Background WifiLock released")
            }
        } catch (e: Exception) {
            AppState.addLog("Error releasing WifiLock: ${e.message}")
        } finally {
            wifiLock = null
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_service_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notif_channel_service_desc)
            }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val stopIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, FileSharingService::class.java).apply { action = ACTION_STOP },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val serverUrl = AppState.getServerUrl(fallbackToLocalhost = true) ?: "http://localhost:${AppState.serverPort}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_server_running_title))
            .setContentText(getString(R.string.notif_server_access_at, serverUrl))
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notif_action_stop), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "sharewith_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_STOP = "com.share.with.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, FileSharingService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FileSharingService::class.java)
            context.stopService(intent)
        }
    }
}
