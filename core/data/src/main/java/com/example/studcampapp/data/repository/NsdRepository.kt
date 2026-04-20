package com.example.studcampapp.data.repository

import android.content.Context
import com.example.studcampapp.model.DiscoveredRoom

interface NsdRepository {
    val discoveredRooms: List<DiscoveredRoom>
    fun startDiscovery(context: Context)
    fun stopDiscovery()
}
