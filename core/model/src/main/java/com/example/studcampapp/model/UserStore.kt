package com.example.studcampapp.model

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object UserStore {
    var currentUser by mutableStateOf<User?>(null)
    var localAvatarUri by mutableStateOf<Uri?>(null)

    fun login(user: User) {
        currentUser = user
        localAvatarUri = null
    }

    fun updateAvatar(uri: Uri) {
        localAvatarUri = uri
    }

    fun updateProfile(login: String, phone: String) {
        currentUser = currentUser?.copy(login = login, phone = phone)
    }

    fun logout() {
        currentUser = null
        localAvatarUri = null
    }
}
