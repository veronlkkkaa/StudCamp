package com.example.studcampapp.data.repository

import com.example.studcampapp.model.SavedRoom

interface RoomRepository {
    val currentRoomName: String
    val rooms: List<SavedRoom>
    val activeRooms: List<SavedRoom>
    val isCheckingRooms: Boolean

    fun setRoomName(name: String)
    fun saveRoom(room: SavedRoom)
    fun updateLastMessage(id: String, message: String)
    suspend fun refreshActiveRooms()
}
