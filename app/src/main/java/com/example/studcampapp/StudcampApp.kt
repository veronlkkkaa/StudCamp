package com.example.studcampapp

import android.app.Application
import com.example.studcampapp.data.RoomHistoryStore

class StudcampApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RoomHistoryStore.init(this)
    }
}