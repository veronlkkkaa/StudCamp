package com.example.studcampapp.feature.room.domain.usecase

import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.data.repository.impl.RoomRepositoryImpl
import com.example.studcampapp.model.SavedRoom

class GetRoomHistoryUseCase(
    private val roomRepository: RoomRepository = RoomRepositoryImpl
) {
    operator fun invoke(): List<SavedRoom> = roomRepository.rooms
}
