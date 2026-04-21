package com.example.studcampapp.feature.profile.domain.usecase

import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.data.repository.UserRepository
import com.example.studcampapp.data.repository.impl.RoomRepositoryImpl
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl

class LogoutUseCase(
    private val userRepository: UserRepository = UserRepositoryImpl,
    private val roomRepository: RoomRepository = RoomRepositoryImpl
) {
    operator fun invoke() {
        userRepository.logout()
        roomRepository.clearAllRooms()
    }
}
