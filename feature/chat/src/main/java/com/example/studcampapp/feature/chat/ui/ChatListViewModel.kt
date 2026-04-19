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
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryImpl,
    private val roomRepository: RoomRepository = RoomRepositoryImpl,
    private val userRepository: UserRepository = UserRepositoryImpl
) : ViewModel() {

    val rooms get() = roomRepository.rooms
    val currentUser get() = userRepository.currentUser
    val localAvatarUri get() = userRepository.localAvatarUri

    var connectingId by mutableStateOf<String?>(null)
        private set
    var connectError by mutableStateOf<String?>(null)
        private set

    fun reconnect(room: SavedRoom, onSuccess: () -> Unit) {
        if (connectingId != null) return
        connectError = null
        connectingId = room.id
        viewModelScope.launch {
            chatRepository.join(room.serverIp, room.serverPort, room.myNickname)
                .onSuccess {
                    roomRepository.setRoomName(room.name)
                    roomRepository.saveRoom(room.copy(lastVisited = System.currentTimeMillis()))
                    chatRepository.connect()
                    connectingId = null
                    onSuccess()
                }
                .onFailure { e ->
                    connectError = "Не удалось подключиться: ${e.message}"
                    connectingId = null
                }
        }
    }
}
