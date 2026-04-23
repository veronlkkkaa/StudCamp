package com.example.studcampapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import com.example.studcampapp.model.SavedRoom
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RoomHistoryStore {
    private val _rooms = mutableStateListOf<SavedRoom>()
    val rooms: List<SavedRoom> get() = _rooms

    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences("room_history", Context.MODE_PRIVATE)
        load()
    }

    fun saveRoom(room: SavedRoom) {
        val idx = _rooms.indexOfFirst {
            it.id == room.id || (it.serverIp == room.serverIp && it.serverPort == room.serverPort)
        }
        if (idx >= 0) {
            _rooms[idx] = room
        } else {
            _rooms.add(0, room)
        }
        persist()
    }

    fun updateNickname(roomId: String, nickname: String) {
        val idx = _rooms.indexOfFirst { it.id == roomId }
        if (idx >= 0) {
            _rooms[idx] = _rooms[idx].copy(myNickname = nickname)
            persist()
        }
    }

    fun updateRoomName(roomId: String, name: String) {
        val idx = _rooms.indexOfFirst { it.id == roomId }
        if (idx >= 0) {
            _rooms[idx] = _rooms[idx].copy(name = name)
            persist()
        }
    }

    fun updateLastMessage(id: String, message: String) {
        val idx = _rooms.indexOfFirst { it.id == id }
        if (idx >= 0) {
            _rooms[idx] = _rooms[idx].copy(
                lastMessage = message,
                lastVisited = System.currentTimeMillis()
            )
            persist()
        }
    }

    fun removeRoom(id: String) {
        val removed = _rooms.removeAll { it.id == id }
        if (removed) persist()
    }

    fun clear() {
        _rooms.clear()
        prefs?.edit()?.remove("rooms")?.apply()
    }

    private fun load() {
        val str = prefs?.getString("rooms_v2", null) ?: return
        val list = runCatching { json.decodeFromString<List<SavedRoom>>(str) }.getOrNull() ?: return
        val deduped = list
            .sortedByDescending { it.lastVisited }
            .distinctBy { it.id }
        _rooms.clear()
        _rooms.addAll(deduped)
    }

    private fun persist() {
        prefs?.edit()?.putString("rooms_v2", json.encodeToString(_rooms.toList()))?.apply()
    }
}
