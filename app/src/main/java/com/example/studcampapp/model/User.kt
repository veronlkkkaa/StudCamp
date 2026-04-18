package com.example.studcampapp.model

import kotlinx.serialization.Serializable

@Serializable
data class User (
    val id: String,
    val login: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val email: String? = null
)