package com.example.studcampapp.feature.profile.domain.usecase

import com.example.studcampapp.data.repository.UserRepository
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl

class LogoutUseCase(
    private val userRepository: UserRepository = UserRepositoryImpl
) {
    operator fun invoke() = userRepository.logout()
}
