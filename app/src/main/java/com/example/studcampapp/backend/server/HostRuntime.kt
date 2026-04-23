package com.example.studcampapp.backend.server

import android.content.Context
import com.example.studcampapp.backend.file.FileStore
import com.example.studcampapp.backend.session.SessionStore
import java.io.File
import java.util.UUID

object HostRuntime {
    private val sessionStore = SessionStore()
    private var hostServer: HostServer? = null

    @Synchronized
    fun start(context: Context, roomName: String = "Lyra Room") {
        if (hostServer != null) return

        val roomId = UUID.randomUUID().toString()
        sessionStore.setInitialRoomState(roomName, roomId)
        val fileStore = FileStore(File(context.cacheDir, "host-files"))
        val nsdPublisher = NsdPublisher(context.applicationContext)
        hostServer = HostServer(
            sessionStore = sessionStore,
            fileStore = fileStore,
            nsdPublisher = nsdPublisher,
            roomName = roomName,
            roomId = roomId
        )
        hostServer?.start()
    }

    @Synchronized
    fun isRunning(): Boolean = hostServer != null

    @Synchronized
    fun stop() {
        hostServer?.stop()
        hostServer = null
    }
}

