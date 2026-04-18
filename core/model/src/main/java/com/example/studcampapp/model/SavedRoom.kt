package com.example.studcampapp.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedRoom(
    val id: String,
    val name: String,
    val serverIp: String,
    val serverPort: Int,
    val myNickname: String,
    val lastMessage: String = "",
    val lastVisited: Long = 0L
)
