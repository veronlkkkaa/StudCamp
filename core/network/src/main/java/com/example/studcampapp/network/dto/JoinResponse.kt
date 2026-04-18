package com.example.studcampapp.network.dto

import com.example.studcampapp.model.RoomState
import com.example.studcampapp.model.User
import kotlinx.serialization.Serializable

@Serializable
data class JoinResponse(
    val sessionId: String,
    val user: User,
    val state: RoomState
)
