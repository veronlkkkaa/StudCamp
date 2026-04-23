package com.example.studcampapp.backend.server

import android.content.Context
import android.util.Log
import com.example.studcampapp.backend.file.FileStore
import com.example.studcampapp.backend.session.SessionStore
import java.io.File
import java.util.UUID

object HostRuntime {
    private const val TAG = "HostRuntime"
    private val sessionStore = SessionStore()
    private var hostServer: HostServer? = null

    @Synchronized
    fun start(context: Context, roomName: String = "Lyra Room") {
        Log.d(TAG, "start() called, already running=${hostServer != null}")
        if (hostServer != null) return

        val roomId = UUID.randomUUID().toString()
        Log.d(TAG, "roomId=$roomId roomName=$roomName")
        sessionStore.setInitialRoomState(roomName, roomId)
        val fileStore = FileStore(File(context.cacheDir, "host-files"))
        val nsdPublisher = NsdPublisher(context.applicationContext)
        Log.d(TAG, "creating HostServer")
        hostServer = HostServer(
            sessionStore = sessionStore,
            fileStore = fileStore,
            nsdPublisher = nsdPublisher,
            roomName = roomName,
            roomId = roomId
        )
        Log.d(TAG, "calling hostServer.start()")
        hostServer?.start()
        Log.d(TAG, "hostServer.start() returned OK")
    }

    @Synchronized
    fun isRunning(): Boolean = hostServer != null

    @Synchronized
    fun stop() {
        hostServer?.stop()
        hostServer = null
    }
}

