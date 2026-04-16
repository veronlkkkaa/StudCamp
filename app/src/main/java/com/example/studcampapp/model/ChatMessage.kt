package com.example.studcampapp.model

import java.time.LocalDateTime

data class ChatMessage(
    val id: Int,
    val author: String,
    val text: String,
    val time: LocalDateTime
)
