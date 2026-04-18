package com.example.studcampapp.model

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(
    val id: String,
    val fileName: String,
    val size: Long,
    val fileUrl: String
)
