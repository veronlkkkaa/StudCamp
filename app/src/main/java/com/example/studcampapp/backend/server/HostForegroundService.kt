package com.example.studcampapp.backend.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.studcampapp.R

class HostForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_HOST -> {
                val roomName = intent.getStringExtra(EXTRA_ROOM_NAME)?.ifBlank { DEFAULT_ROOM_NAME } ?: DEFAULT_ROOM_NAME
                startForeground(NOTIFICATION_ID, buildNotification(roomName))
                HostRuntime.start(applicationContext, roomName)
            }

            ACTION_STOP_HOST -> {
                HostRuntime.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
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
        private const val CHANNEL_ID = "lyra_host_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_ROOM_NAME = "Lyra Room"

        const val ACTION_START_HOST = "com.example.studcampapp.action.START_HOST"
        const val ACTION_STOP_HOST = "com.example.studcampapp.action.STOP_HOST"
        const val EXTRA_ROOM_NAME = "extra_room_name"

        fun start(context: Context, roomName: String) {
            val intent = Intent(context, HostForegroundService::class.java).apply {
                action = ACTION_START_HOST
                putExtra(EXTRA_ROOM_NAME, roomName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HostForegroundService::class.java).apply {
                action = ACTION_STOP_HOST
            }
            context.startService(intent)
        }
    }
}

