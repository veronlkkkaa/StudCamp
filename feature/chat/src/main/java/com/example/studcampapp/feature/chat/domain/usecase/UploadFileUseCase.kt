package com.example.studcampapp.feature.chat.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.model.FileInfo

class UploadFileUseCase(
    private val chatRepository: ChatRepository = ChatRepositoryImpl
) {
    suspend operator fun invoke(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String
    ): Result<FileInfo> = chatRepository.uploadFile(context, uri, fileName, mimeType)
}
