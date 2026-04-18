package com.example.studcampapp.feature.chat.domain.usecase

import android.content.Context
import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.model.FileInfo

class DownloadFileUseCase(
    private val chatRepository: ChatRepository = ChatRepositoryImpl
) {
    operator fun invoke(context: Context, fileInfo: FileInfo): Long =
        chatRepository.downloadFile(context, fileInfo)
}
