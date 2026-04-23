package com.example.studcampapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.User

interface ChatRepository {
    val messages: List<ChatMessage>
    val participants: List<User>
    val myUser: User?
    val isHostClosed: Boolean
    val isConnected: Boolean
    val sessionInvalidated: Boolean
    val connectionError: String?
    val uploadProgress: Float?
    val baseUrl: String
    val currentRoomName: String
    val currentRoomId: String

    suspend fun join(ip: String, port: Int, login: String): Result<Unit>
    fun connect()
    fun disconnect()
    fun sendMessage(text: String, fileInfo: FileInfo? = null)
    suspend fun uploadFile(context: Context, uri: Uri, fileName: String, mimeType: String): Result<FileInfo>
    fun downloadFile(context: Context, fileInfo: FileInfo): Long
    suspend fun downloadToCache(context: Context, fileInfo: FileInfo): Result<java.io.File>
    fun getAuthHeader(): String?
    suspend fun renameRoom(newName: String): Result<Unit>
}
