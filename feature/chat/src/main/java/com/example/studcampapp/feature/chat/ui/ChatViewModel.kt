package com.example.studcampapp.feature.chat.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.data.repository.RoomRepository
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.data.repository.impl.RoomRepositoryImpl
import com.example.studcampapp.feature.chat.domain.usecase.DisconnectUseCase
import com.example.studcampapp.feature.chat.domain.usecase.DownloadFileUseCase
import com.example.studcampapp.feature.chat.domain.usecase.SendMessageUseCase
import com.example.studcampapp.feature.chat.domain.usecase.UploadFileUseCase
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.MessageAttachment
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryImpl,
    private val roomRepository: RoomRepository = RoomRepositoryImpl
) : ViewModel() {

    private val sendMessageUseCase = SendMessageUseCase(chatRepository)
    private val uploadFileUseCase = UploadFileUseCase(chatRepository)
    private val downloadFileUseCase = DownloadFileUseCase(chatRepository)
    private val disconnectUseCase = DisconnectUseCase(chatRepository)

    val messages get() = chatRepository.messages
    val participants get() = chatRepository.participants
    val myUser get() = chatRepository.myUser
    val isHostClosed get() = chatRepository.isHostClosed
    val sessionInvalidated get() = chatRepository.sessionInvalidated
    val connectionError get() = chatRepository.connectionError
    val lastServerError get() = chatRepository.lastServerError
    val uploadProgress get() = chatRepository.uploadProgress
    val roomName get() = chatRepository.currentRoomName
    val baseUrl get() = chatRepository.baseUrl

    fun sendMessage(text: String, fileInfo: FileInfo? = null) =
        sendMessageUseCase(text, fileInfo)

    fun downloadFile(context: Context, fileInfo: FileInfo): Long =
        downloadFileUseCase(context, fileInfo)

    suspend fun downloadToCache(context: Context, fileInfo: FileInfo): Result<java.io.File> =
        chatRepository.downloadToCache(context, fileInfo)

    fun disconnect() = disconnectUseCase()

    fun getAuthHeader(): String? = chatRepository.getAuthHeader()

    fun renameRoom(newName: String) {
        viewModelScope.launch {
            chatRepository.renameRoom(newName)
        }
    }

    fun uploadAndSend(
        context: Context,
        text: String,
        attachment: MessageAttachment,
        mimeType: String
    ) {
        viewModelScope.launch {
            uploadFileUseCase(context, attachment.uri, attachment.fileName, mimeType)
                .onSuccess { fileInfo -> sendMessageUseCase(text, fileInfo) }
                .onFailure { e ->
                    Log.w("StudCampFile", "uploadAndSend: upload failed, sending text only", e)
                    sendMessageUseCase(text)
                }
        }
    }
}
