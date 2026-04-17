package com.example.studcampapp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object UserStore {
    var currentUser by mutableStateOf<User?>(null)

    fun login(user: User) {
        currentUser = user
    }

    fun updateAvatar(uri: android.net.Uri) {
        currentUser = currentUser?.copy(avatarUri = uri)
    }

    fun logout() {
        currentUser = null
    }
}
