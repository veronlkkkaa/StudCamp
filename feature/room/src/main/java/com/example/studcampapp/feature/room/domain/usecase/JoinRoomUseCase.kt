package com.example.studcampapp.feature.room.domain.usecase

import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.data.repository.impl.RoomRepositoryImpl
import com.example.studcampapp.model.SavedRoom
import kotlinx.coroutines.delay

private const val MAX_ATTEMPTS = 8
private const val RETRY_DELAY_MS = 400L

class JoinRoomUseCase(
    private val chatRepository: ChatRepository = ChatRepositoryImpl,
    private val roomRepository: RoomRepository = RoomRepositoryImpl
) {
    suspend operator fun invoke(
        ip: String,
        port: Int,
        nickname: String,
        roomName: String
    ): Result<Unit> {
        var lastResult: Result<Unit> = Result.failure(IllegalStateException("No attempts made"))
        repeat(MAX_ATTEMPTS) { attempt ->
            lastResult = chatRepository.join(ip, port, nickname)
            if (lastResult.isSuccess) return@repeat
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        return lastResult.onSuccess {
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
}
