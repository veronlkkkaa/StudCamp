package com.example.studcampapp.model

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
            creatorId = "1",
            creatorName = "Алексей М.",
            participants = listOf(
                RoomParticipant("1", "Алексей М.", false),
                RoomParticipant("2", "Мария С.", false),
                RoomParticipant("3", "Гость 3", true),
                RoomParticipant("4", "Гость 4", true),
                RoomParticipant("5", "Дмитрий К.", false),
            )
        )
    )
}
