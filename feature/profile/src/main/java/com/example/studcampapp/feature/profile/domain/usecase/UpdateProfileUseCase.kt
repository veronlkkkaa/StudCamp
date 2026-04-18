package com.example.studcampapp.feature.profile.domain.usecase

import android.net.Uri
import com.example.studcampapp.data.repository.UserRepository
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl

class UpdateProfileUseCase(
    private val userRepository: UserRepository = UserRepositoryImpl
) {
    fun updateProfile(login: String, phone: String) =
        userRepository.updateProfile(login, phone)

    fun updateAvatar(uri: Uri) =
        userRepository.updateAvatar(uri)
}
