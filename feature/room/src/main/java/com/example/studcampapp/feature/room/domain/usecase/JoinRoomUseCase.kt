package com.example.studcampapp.feature.room.domain.usecase

import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.data.repository.impl.RoomRepositoryImpl
import com.example.studcampapp.model.SavedRoom

class JoinRoomUseCase(
    private val chatRepository: ChatRepository = ChatRepositoryImpl,
    private val roomRepository: RoomRepository = RoomRepositoryImpl
) {
    suspend operator fun invoke(
        ip: String,
        port: Int,
        nickname: String,
        roomName: String
    ): Result<Unit> = chatRepository.join(ip, port, nickname).onSuccess {
        roomRepository.setRoomName(roomName)
        roomRepository.saveRoom(
            SavedRoom(
                id = "$ip:$port",
                name = roomName,
                serverIp = ip,
                serverPort = port,
                myNickname = nickname,
                lastVisited = System.currentTimeMillis()
            )
        )
        chatRepository.connect()
    }
}
