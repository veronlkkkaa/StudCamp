package com.example.studcampapp.model

import kotlinx.serialization.Serializable

@Serializable
data class User (
    val id: String,
    val login: String,
    val firstName: String?,
    val lastName: String?,
    val middleName: String?,
    val avatarUrl: String? = null,
    val phone: String?,
    val email: String?
)