package com.example.studcampapp.data

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class RoomParticipant(
    val id: String,
    val displayName: String,
    val isGuest: Boolean,
    val avatarUri: Uri? = null
)

data class RoomInfo(
    val name: String,
    val creatorId: String,
    val creatorName: String,
    val participants: List<RoomParticipant>
)

object RoomStore {
    var currentRoom by mutableStateOf(
        RoomInfo(
            name = "Общий чат",
            creatorId = "",
            creatorName = "",
            participants = emptyList()
        )
    )

    fun setRoomName(name: String) {
        currentRoom = currentRoom.copy(name = name)
    }
}
