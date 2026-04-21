package com.example.studcampapp.feature.chat.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.feature.chat.ui.ChatListViewModel
import com.example.studcampapp.model.SavedRoom
import com.example.studcampapp.ui.theme.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChatListScreen(
    onRoomConnected: () -> Unit,
    onCreateRoom: () -> Unit,
    onProfileClick: () -> Unit = {},
    viewModel: ChatListViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.refreshRooms() }

    val appColors = LocalAppColors.current
    val rooms = viewModel.rooms
    val isCheckingRooms = viewModel.isCheckingRooms
    val connectingId = viewModel.connectingId
    val connectError = viewModel.connectError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val user = viewModel.currentUser
            val localAvatarUri = viewModel.localAvatarUri
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
                                text = user?.firstName?.firstOrNull()?.toString()
                                    ?: user?.login?.firstOrNull()?.toString() ?: "?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                color = appColors.textPrimary
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
                color = appColors.textPrimary
            )

            OutlinedButton(
                onClick = onCreateRoom,
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Purple.copy(alpha = 0.2f),
                    contentColor = appColors.accent
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+ Комната",
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = appColors.accent
                )
            }
        }

        if (connectError != null) {
            Text(
                text = connectError!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontFamily = InterFontFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (isCheckingRooms) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
        } else if (rooms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Нет активных комнат",
                        fontSize = 16.sp,
                        fontFamily = InterFontFamily,
                        color = appColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Создай или подключись к комнате",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = appColors.textSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomItem(
                        room = room,
                        isConnecting = connectingId == room.id,
                        onClick = {
                            viewModel.reconnect(room) { onRoomConnected() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RoomItem(
    room: SavedRoom,
    isConnecting: Boolean = false,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = appColors.surface,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isConnecting
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
                        text = room.name.first().toString().uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        color = appColors.accent
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
                    color = appColors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (room.lastMessage.isNotEmpty()) room.lastMessage
                           else "${room.serverIp}:${room.serverPort}  ·  @${room.myNickname}",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = appColors.textSecondary,
                    maxLines = 1
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Purple,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.runtime.Composable
private fun ChatListScreenPreview() {
    com.example.studcampapp.ui.theme.AppTheme {
        ChatListScreen(onRoomConnected = {}, onCreateRoom = {})
    }
}
