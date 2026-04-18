package com.example.studcampapp.feature.chat.domain.usecase

import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl

class DisconnectUseCase(
    private val chatRepository: ChatRepository = ChatRepositoryImpl
) {
    operator fun invoke() = chatRepository.disconnect()
}
