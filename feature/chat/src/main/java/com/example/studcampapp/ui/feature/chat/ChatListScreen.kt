package com.example.studcampapp.ui.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.studcampapp.model.UserStore
import com.example.studcampapp.ui.theme.*

data class RoomPreview(val name: String, val lastMessage: String, val members: Int)

@Composable
fun ChatListScreen(
    onRoomClick: () -> Unit,
    onCreateRoom: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    val fakeRooms = listOf(
        RoomPreview("Общий чат", "Привет всем!", 12),
        RoomPreview("Разработчики", "Кто знает Compose?", 5),
        RoomPreview("Случайные", "lol", 3),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val user = UserStore.currentUser
            val localAvatarUri = UserStore.localAvatarUri
            Surface(
                onClick = onProfileClick,
                color = androidx.compose.ui.graphics.Color.Transparent,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (localAvatarUri != null) {
                        AsyncImage(
                            model = localAvatarUri,
                            contentDescription = "Аватар",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(colors = listOf(PurpleLight, PurpleVibrant))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.firstName?.firstOrNull()?.toString() ?: user?.login?.firstOrNull()?.toString() ?: "?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Чаты",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = TextPrimary
            )

            OutlinedButton(
                onClick = onCreateRoom,
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Purple.copy(alpha = 0.2f),
                    contentColor = Wisteria
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+ Комната",
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Wisteria
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(fakeRooms) { room ->
                RoomItem(room = room, onClick = onRoomClick)
            }
        }
    }
}

@Composable
fun RoomItem(room: RoomPreview, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Purple.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = room.name.first().toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        color = Wisteria
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = room.lastMessage,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = TextSecondary
                )
            }

            Text(
                text = "${room.members} чел.",
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                color = TextSecondary
            )
        }
    }
}
