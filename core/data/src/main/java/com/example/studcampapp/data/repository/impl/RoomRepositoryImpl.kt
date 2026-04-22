package com.example.studcampapp.data.repository.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.studcampapp.data.RoomHistoryStore
import com.example.studcampapp.data.RoomStore
import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.model.SavedRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object RoomRepositoryImpl : RoomRepository {
    override val currentRoomName: String get() = RoomStore.currentRoom.name
    override val rooms: List<SavedRoom> get() = RoomHistoryStore.rooms

    private var reachableIds by mutableStateOf<Set<String>>(emptySet())

    override var isCheckingRooms by mutableStateOf(false)
        private set

    override val activeRooms: List<SavedRoom>
        get() = rooms.filter { it.id in reachableIds }

    override fun clearAllRooms() {
        RoomHistoryStore.clear()
        reachableIds = emptySet()
    }

    override fun setRoomName(name: String) = RoomStore.setRoomName(name)

    override fun saveRoom(room: SavedRoom) = RoomHistoryStore.saveRoom(room)

    override fun updateLastMessage(id: String, message: String) =
        RoomHistoryStore.updateLastMessage(id, message)

    override fun removeRoom(id: String) {
        RoomHistoryStore.removeRoom(id)
        reachableIds = reachableIds - id
    }

    override suspend fun refreshActiveRooms() {
        val allRooms = rooms
        if (allRooms.isEmpty()) {
            reachableIds = emptySet()
            return
        }
        isCheckingRooms = true
        reachableIds = coroutineScope {
            allRooms
                .map { room ->
                    async { if (isReachable(room.serverIp, room.serverPort)) room.id else null }
                }
                .map { it.await() }
                .filterNotNull()
                .toSet()
        }
        isCheckingRooms = false
    }

    private suspend fun isReachable(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("http://$ip:$port/health")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
            }
            try {
                connection.connect()
                connection.responseCode in 200..299
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(false)
    }
}
