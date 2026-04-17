package com.example.studcampapp.model.ws

import com.example.studcampapp.model.FileInfo
import kotlinx.serialization.Serializable

@Serializable
sealed class WsClientEvent {

    @Serializable
    data class SendMessage(
        val text: String,
        val fileInfo: FileInfo? = null
    ) : WsClientEvent()

    @Serializable
    data class SendFile(
        val fileId: String
    ) : WsClientEvent()

    @Serializable
    object Ping : WsClientEvent()
}