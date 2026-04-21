package com.example.studcampapp.model

import kotlinx.serialization.Serializable

@Serializable
data class RoomState(
    val users: List<User>,
    val messages: List<ChatMessage>,
    val name: String = "",
    val id: String = ""
)
