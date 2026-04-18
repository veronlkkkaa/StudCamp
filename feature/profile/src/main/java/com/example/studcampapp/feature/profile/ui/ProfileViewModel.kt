package com.example.studcampapp.feature.profile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.studcampapp.data.repository.UserRepository
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl
import com.example.studcampapp.feature.profile.domain.usecase.LogoutUseCase
import com.example.studcampapp.feature.profile.domain.usecase.UpdateProfileUseCase

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl,
    private val updateProfileUseCase: UpdateProfileUseCase = UpdateProfileUseCase(),
    private val logoutUseCase: LogoutUseCase = LogoutUseCase()
) : ViewModel() {

    val currentUser get() = userRepository.currentUser
    val localAvatarUri get() = userRepository.localAvatarUri

    fun updateProfile(login: String, phone: String) =
        updateProfileUseCase.updateProfile(login, phone)

    fun updateAvatar(uri: Uri) = updateProfileUseCase.updateAvatar(uri)

    fun logout() = logoutUseCase()
}
