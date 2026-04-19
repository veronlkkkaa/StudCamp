package com.example.studcampapp.data.repository.impl

import com.example.studcampapp.data.RoomHistoryStore
import com.example.studcampapp.data.RoomStore
import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.model.SavedRoom

object RoomRepositoryImpl : RoomRepository {
    override val currentRoomName: String get() = RoomStore.currentRoom.name
    override val rooms: List<SavedRoom> get() = RoomHistoryStore.rooms

    override fun setRoomName(name: String) = RoomStore.setRoomName(name)

    override fun saveRoom(room: SavedRoom) = RoomHistoryStore.saveRoom(room)

    override fun updateLastMessage(id: String, message: String) =
        RoomHistoryStore.updateLastMessage(id, message)
}
