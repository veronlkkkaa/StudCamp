package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.session.SessionStore

object HostRuntime {
    private val sessionStore = SessionStore()
    private val hostServer = HostServer(sessionStore = sessionStore)

    fun start() {
        hostServer.start()
    }

    fun stop() {
        hostServer.stop()
    }
}

