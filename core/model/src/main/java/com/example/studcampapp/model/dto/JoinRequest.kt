package com.example.studcampapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class JoinRequest(
    val login: String,
    val firstName: String?,
    val lastName: String?,
    val middleName: String?,
    val avatarUrl: String? = null,
    val phone: String?,
    val email: String?
)
