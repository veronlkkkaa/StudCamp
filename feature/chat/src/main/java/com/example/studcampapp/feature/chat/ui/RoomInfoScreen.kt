package com.example.studcampapp.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.feature.chat.ui.ChatViewModel
import com.example.studcampapp.model.User
import com.example.studcampapp.ui.theme.*

@Composable
fun RoomInfoScreen(onBack: () -> Unit, viewModel: ChatViewModel = viewModel()) {
    val appColors = LocalAppColors.current
    val roomName = viewModel.roomName
    val participants = viewModel.participants
    val myUserId = viewModel.myUser?.id

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
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = appColors.accent)
            }
            Text(
                text = "Информация о комнате",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = appColors.textPrimary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = appColors.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = roomName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = appColors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Участников: ${participants.size}",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            color = appColors.textSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Участники",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = appColors.accent,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(participants, key = { it.id }) { user ->
                ParticipantItem(user = user, isMe = user.id == myUserId)
            }
        }
    }
}

@Composable
fun ParticipantItem(user: User, isMe: Boolean = false) {
    val appColors = LocalAppColors.current
    val initial = user.login.firstOrNull()?.toString() ?: "?"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = appColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(PurpleLight, PurpleVibrant))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = appColors.textPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = user.login,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                color = appColors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            if (isMe) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Purple.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "Вы",
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        color = appColors.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
