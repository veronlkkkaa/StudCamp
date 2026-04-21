package com.example.studcampapp.data.repository.impl

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.studcampapp.data.repository.ChatRepository
import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.User
import com.example.studcampapp.network.ChatClient

object ChatRepositoryImpl : ChatRepository {
    override val messages: List<ChatMessage> get() = ChatClient.messages
    override val participants: List<User> get() = ChatClient.participants
    override val myUser: User? get() = ChatClient.myUser
    override val isHostClosed: Boolean get() = ChatClient.isHostClosed
    override val connectionError: String? get() = ChatClient.connectionError
    override val uploadProgress: Float? get() = ChatClient.uploadProgress
    override val baseUrl: String get() = ChatClient.baseUrl

    override suspend fun join(ip: String, port: Int, login: String): Result<Unit> =
        ChatClient.join(ip, port, login)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun connect() = ChatClient.connect()

    override fun disconnect() = ChatClient.disconnect()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun sendMessage(text: String, fileInfo: FileInfo?) =
        ChatClient.sendMessage(text, fileInfo)

    override suspend fun uploadFile(
        context: Context, uri: Uri, fileName: String, mimeType: String
    ): Result<FileInfo> = ChatClient.uploadFile(context, uri, fileName, mimeType)

    override fun downloadFile(context: Context, fileInfo: FileInfo): Long =
        ChatClient.downloadFile(context, fileInfo)

    override fun getAuthHeader(): String? = ChatClient.getAuthHeader()
}
