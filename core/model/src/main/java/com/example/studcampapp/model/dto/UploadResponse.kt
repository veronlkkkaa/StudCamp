package com.example.studcampapp.model.dto

import com.example.studcampapp.model.FileInfo
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val fileInfo: FileInfo
)
