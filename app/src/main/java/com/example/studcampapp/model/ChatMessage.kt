package com.example.studcampapp.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Int,
    val user: User,
    val text: String,
    val timeEpochMillis: Long,
    val fileInfo: FileInfo? = null
)
