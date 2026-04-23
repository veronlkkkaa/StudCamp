package com.example.studcampapp

import android.app.Application
import com.example.studcampapp.data.RoomHistoryStore
import com.example.studcampapp.data.UserStore
import com.example.studcampapp.network.ChatClient

class LyraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserStore.init(this)
        RoomHistoryStore.init(this)
        ChatClient.initContext(this)
    }
}