package com.example.studcampapp.feature.chat.domain.usecase

import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.model.FileInfo

class SendMessageUseCase(
    private val chatRepository: ChatRepository = ChatRepositoryImpl
) {
    operator fun invoke(text: String, fileInfo: FileInfo? = null) {
        chatRepository.sendMessage(text, fileInfo)
    }
}
