package com.example.studcampapp.data.repository.impl

import android.net.Uri
import com.example.studcampapp.data.RoomHistoryStore
import com.example.studcampapp.data.UserStore
import com.example.studcampapp.data.repository.UserRepository
import com.example.studcampapp.model.User

object UserRepositoryImpl : UserRepository {
    override val currentUser: User? get() = UserStore.currentUser
    override val localAvatarUri: Uri? get() = UserStore.localAvatarUri
    override val isGuest: Boolean get() = UserStore.isGuest

    override fun login(user: User) = UserStore.login(user)
    override fun logout() {
        UserStore.logout()
        RoomHistoryStore.clear()
    }
    override fun updateAvatar(uri: Uri) = UserStore.updateAvatar(uri)
    override fun updateProfile(login: String, phone: String) = UserStore.updateProfile(login, phone)
}
