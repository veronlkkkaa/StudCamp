package com.example.studcampapp.network.dto

import com.example.studcampapp.model.FileInfo
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val fileInfo: FileInfo
)
