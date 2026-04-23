package com.example.studcampapp.network.ws

import com.example.studcampapp.model.FileInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WsClientEvent {

    @Serializable
    @SerialName("send_message")
    data class SendMessage(
        val text: String,
        val fileInfo: FileInfo? = null,
        val clientMsgId: String? = null
    ) : WsClientEvent()

    @Serializable
    @SerialName("send_file")
    data class SendFile(
        val fileId: String
    ) : WsClientEvent()

    @Serializable
    @SerialName("ping")
    object Ping : WsClientEvent()
}
