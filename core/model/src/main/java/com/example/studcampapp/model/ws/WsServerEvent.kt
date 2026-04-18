package com.example.studcampapp.model.ws

import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.RoomState
import com.example.studcampapp.model.User
import kotlinx.serialization.Serializable

@Serializable
sealed class WsServerEvent {

    @Serializable
    data class RoomStateEvent(
        val state: RoomState
    ) : WsServerEvent()

    @Serializable
    data class UserJoined(
        val user: User
    ) : WsServerEvent()

    @Serializable
    data class UserLeft(
        val userId: String
    ) : WsServerEvent()

    @Serializable
    data class NewMessage(
        val message: ChatMessage
    ) : WsServerEvent()

    @Serializable
    data class FileShared(
        val file: FileInfo
    ) : WsServerEvent()

    @Serializable
    data class Error(
        val code: String,
        val message: String
    ) : WsServerEvent()

    @Serializable
    object Pong : WsServerEvent()

    @Serializable
    object HostClosed : WsServerEvent()
}
