package com.example.studcampapp

import android.app.Application
import com.example.studcampapp.backend.server.HostRuntime
import com.example.studcampapp.data.RoomHistoryStore

class StudcampApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HostRuntime.start(cacheDir)
        RoomHistoryStore.init(this)
    }

    override fun onTerminate() {
        HostRuntime.stop()
        super.onTerminate()
    }
}
