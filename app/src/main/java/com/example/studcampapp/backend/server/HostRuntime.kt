package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.file.FileStore
import com.example.studcampapp.backend.session.SessionStore
import java.io.File

object HostRuntime {
    private val sessionStore = SessionStore()
    private var hostServer: HostServer? = null

    @Synchronized
    fun start(cacheDir: File) {
        if (hostServer != null) return

        val fileStore = FileStore(File(cacheDir, "host-files"))
        hostServer = HostServer(
            sessionStore = sessionStore,
            fileStore = fileStore
        )
        hostServer?.start()
    }

    @Synchronized
    fun stop() {
        hostServer?.stop()
        hostServer = null
    }
}

