package com.example.studcampapp.data.repository.impl

import android.content.Context
import com.example.studcampapp.data.repository.NsdRepository
import com.example.studcampapp.model.DiscoveredRoom
import com.example.studcampapp.network.NsdDiscovery

object NsdRepositoryImpl : NsdRepository {
    override val discoveredRooms: List<DiscoveredRoom> get() = NsdDiscovery.rooms
    override fun startDiscovery(context: Context) = NsdDiscovery.start(context)
    override fun stopDiscovery() = NsdDiscovery.stop()
}
