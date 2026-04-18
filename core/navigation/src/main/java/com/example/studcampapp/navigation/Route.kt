package com.example.studcampapp.navigation

import kotlinx.serialization.Serializable

object Route {
    @Serializable data object Start
    @Serializable data object Auth
    @Serializable data object Register
    @Serializable data object ChatList
    @Serializable data object Chat
    @Serializable data object Profile
    @Serializable data object EditProfile
    @Serializable data object RoomOptions
    @Serializable data object CreateRoom
    @Serializable data object JoinRoom
    @Serializable data object RoomInfo
}
