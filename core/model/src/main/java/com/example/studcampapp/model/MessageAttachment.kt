package com.example.studcampapp.model

import android.net.Uri

data class MessageAttachment(
    val uri: Uri,
    val type: AttachmentType,
    val fileName: String
)
