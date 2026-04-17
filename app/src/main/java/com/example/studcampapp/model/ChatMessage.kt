package com.example.studcampapp.model

import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Int,
    val user: User,
    val text: String,
    val time: LocalDateTime,
    val fileInfo: FileInfo? = null
)
