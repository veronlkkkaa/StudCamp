package com.example.studcampapp.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageStatus { Sending, Sent, Read }
