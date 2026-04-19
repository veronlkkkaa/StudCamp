package com.example.studcampapp.feature.auth.ui

import androidx.lifecycle.ViewModel
import com.example.studcampapp.data.repository.UserRepository
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl
import com.example.studcampapp.model.User

class AuthViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl
) : ViewModel() {

    val currentUser get() = userRepository.currentUser

    fun login(user: User) = userRepository.login(user)

    fun logout() = userRepository.logout()
}
