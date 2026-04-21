package com.example.studcampapp.data.repository

import android.net.Uri
import com.example.studcampapp.model.User

interface UserRepository {
    val currentUser: User?
    val localAvatarUri: Uri?
    val isGuest: Boolean

    fun login(user: User)
    fun logout()
    fun updateAvatar(uri: Uri)
    fun updateProfile(login: String, phone: String)
}
