package com.example.studcampapp.backend.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.studcampapp.R

class HostForegroundService : Service() {

    private var wifiLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action} sdk=${Build.VERSION.SDK_INT}")
        when (intent?.action) {
            ACTION_START_HOST -> {
                val roomName = intent.getStringExtra(EXTRA_ROOM_NAME)?.ifBlank { DEFAULT_ROOM_NAME } ?: DEFAULT_ROOM_NAME
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
                Log.d(TAG, "startForeground: roomName=$roomName serviceType=$serviceType")
                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(roomName), serviceType)
                    Log.d(TAG, "startForeground OK")
                } catch (e: Exception) {
                    Log.e(TAG, "startForeground FAILED", e)
                    stopSelf()
                    return START_NOT_STICKY
                }

                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null) {
                    runCatching {
                        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "lyra:host:wifi").also {
                            it.setReferenceCounted(false)
                            it.acquire()
                        }
                        Log.d(TAG, "wifiLock acquired")
                    }.onFailure { Log.w(TAG, "wifiLock failed", it) }
                    runCatching {
                        multicastLock = wifiManager.createMulticastLock("lyra:host:multicast").also {
                            it.setReferenceCounted(false)
                            it.acquire()
                        }
                        Log.d(TAG, "multicastLock acquired")
                    }.onFailure { Log.w(TAG, "multicastLock failed", it) }
                } else {
                    Log.w(TAG, "WifiManager is null")
                }

                Log.d(TAG, "calling HostRuntime.start")
                runCatching { HostRuntime.start(applicationContext, roomName) }
                    .onSuccess { Log.d(TAG, "HostRuntime.start OK") }
                    .onFailure { Log.e(TAG, "HostRuntime.start FAILED", it) }
            }

            ACTION_STOP_HOST -> {
                Log.d(TAG, "stopping host")
                HostRuntime.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        wifiLock?.let { if (it.isHeld) it.release() }
        multicastLock?.let { if (it.isHeld) it.release() }
        wifiLock = null
        multicastLock = null
        HostRuntime.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun buildNotification(roomName: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Lyra host is running")
            .setContentText("Room: $roomName")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lyra Host",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "HostFgService"
        private const val CHANNEL_ID = "lyra_host_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_ROOM_NAME = "Lyra Room"

        const val ACTION_START_HOST = "com.example.studcampapp.action.START_HOST"
        const val ACTION_STOP_HOST = "com.example.studcampapp.action.STOP_HOST"
        const val EXTRA_ROOM_NAME = "extra_room_name"

        fun start(context: Context, roomName: String) {
            Log.d(TAG, "HostForegroundService.start() roomName=$roomName")
            try {
                val intent = Intent(context, HostForegroundService::class.java).apply {
                    action = ACTION_START_HOST
                    putExtra(EXTRA_ROOM_NAME, roomName)
                }
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "startForegroundService dispatched OK")
            } catch (e: Exception) {
                Log.e(TAG, "startForegroundService FAILED", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, HostForegroundService::class.java).apply {
                action = ACTION_STOP_HOST
            }
            context.startService(intent)
        }
    }
}

