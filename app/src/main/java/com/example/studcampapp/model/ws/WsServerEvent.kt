package com.example.studcampapp.model.ws

import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.RoomState
import com.example.studcampapp.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WsServerEvent {

    @Serializable
    @SerialName("room_state")
    data class RoomStateEvent(
        val state: RoomState
    ) : WsServerEvent()

    @Serializable
    @SerialName("user_joined")
    data class UserJoined(
        val user: User
    ) : WsServerEvent()

    @Serializable
    @SerialName("user_left")
    data class UserLeft(
        val userId: String
    ) : WsServerEvent()

    @Serializable
    @SerialName("new_message")
    data class NewMessage(
        val message: ChatMessage
    ) : WsServerEvent()

    @Serializable
    @SerialName("file_shared")
    data class FileShared(
        val file: FileInfo
    ) : WsServerEvent()

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : WsServerEvent()

    @Serializable
    @SerialName("pong")
    object Pong : WsServerEvent()

    @Serializable
    @SerialName("host_closed")
    object HostClosed : WsServerEvent()
}