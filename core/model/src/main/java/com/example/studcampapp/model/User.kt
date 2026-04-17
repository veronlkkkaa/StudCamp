package com.example.studcampapp.model

import android.net.Uri

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val phone: String,
    val avatarUri: Uri? = null
)
