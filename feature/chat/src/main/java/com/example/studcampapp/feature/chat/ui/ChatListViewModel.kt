package com.example.studcampapp.feature.chat.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.data.repository.UserRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.data.repository.impl.RoomRepositoryImpl
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl
import com.example.studcampapp.model.SavedRoom
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryImpl,
    private val roomRepository: RoomRepository = RoomRepositoryImpl,
    private val userRepository: UserRepository = UserRepositoryImpl
) : ViewModel() {

    val rooms get() = roomRepository.activeRooms
    val isCheckingRooms get() = roomRepository.isCheckingRooms
    val currentUser get() = userRepository.currentUser
    val localAvatarUri get() = userRepository.localAvatarUri

    var connectingId by mutableStateOf<String?>(null)
        private set
    var connectError by mutableStateOf<String?>(null)
        private set

    fun refreshRooms() {
        viewModelScope.launch { roomRepository.refreshActiveRooms() }
    }

    fun reconnect(room: SavedRoom, onSuccess: () -> Unit) {
        if (connectingId != null) return
        connectError = null
        connectingId = room.id
        viewModelScope.launch {
            val targetBaseUrl = "http://${room.serverIp}:${room.serverPort}"
            val hasActiveSession = !chatRepository.getAuthHeader().isNullOrBlank()
            if (hasActiveSession && chatRepository.baseUrl == targetBaseUrl) {
                if (!chatRepository.isConnected) {
                    chatRepository.connect()
                }
                connectingId = null
                onSuccess()
                return@launch
            }

            chatRepository.disconnect()
            var joined = false
            var lastError = ""
            repeat(6) {
                if (joined) return@repeat
                val result = chatRepository.join(room.serverIp, room.serverPort, room.myNickname)
                if (result.isSuccess) {
                    joined = true
                } else {
                    lastError = result.exceptionOrNull()?.message ?: ""
                    delay(400L)
                }
            }
            if (!joined) {
                connectError = mapReconnectError(lastError, room.id)
                connectingId = null
                return@launch
            }
            val serverName = chatRepository.currentRoomName.ifBlank { room.name }
            roomRepository.setRoomName(serverName)
            roomRepository.saveRoom(room.copy(name = serverName, lastVisited = System.currentTimeMillis()))
            chatRepository.connect()
            connectingId = null
            onSuccess()
        }
    }

    private fun mapReconnectError(error: String, roomId: String): String {
        val msg = error.lowercase()
        return when {
            msg.contains("connection refused") || msg.contains("not found") || msg.contains("404") -> {
                roomRepository.removeRoom(roomId)
                "Комната не существует или была закрыта"
            }
            msg.contains("no route to host") || msg.contains("unable to resolve host") ||
            msg.contains("failed to connect") -> {
                roomRepository.removeRoom(roomId)
                "Не удалось найти сервер в сети"
            }
            msg.contains("timed out") || msg.contains("timeout") ->
                "Сервер не отвечает — проверь подключение"
            msg.contains("nickname is already in use") || msg.contains("conflict") || msg.contains("409") ->
                "Этот никнейм уже занят в комнате"
            else -> "Не удалось подключиться: $error"
        }
    }
}
