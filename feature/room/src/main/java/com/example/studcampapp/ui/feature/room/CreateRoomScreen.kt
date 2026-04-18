package com.example.studcampapp.ui.feature.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studcampapp.model.ChatClient
import com.example.studcampapp.model.RoomHistoryStore
import com.example.studcampapp.model.RoomStore
import com.example.studcampapp.model.SavedRoom
import com.example.studcampapp.model.UserStore
import com.example.studcampapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CreateRoomScreen(
    onBack: () -> Unit,
    onRoomCreated: () -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(UserStore.currentUser?.login ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Создать комнату",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Придумай название и свой ник",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it; error = null },
                label = {
                    Text("Название комнаты", fontFamily = InterFontFamily, color = TextSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Wisteria
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it; error = null },
                label = {
                    Text("Твой ник", fontFamily = InterFontFamily, color = TextSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Wisteria
                ),
                singleLine = true
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (roomName.isBlank() || nickname.isBlank()) return@Button
                    isLoading = true
                    error = null
                    scope.launch {
                        ChatClient.join("127.0.0.1", 8080, nickname.trim())
                            .onSuccess {
                                val name = roomName.trim()
                                RoomStore.setRoomName(name)
                                RoomHistoryStore.saveRoom(
                                    SavedRoom(
                                        id = "127.0.0.1:8080",
                                        name = name,
                                        serverIp = "127.0.0.1",
                                        serverPort = 8080,
                                        myNickname = nickname.trim(),
                                        lastVisited = System.currentTimeMillis()
                                    )
                                )
                                ChatClient.connect()
                                onRoomCreated()
                            }
                            .onFailure { e ->
                                error = "Ошибка запуска: ${e.message}"
                            }
                        isLoading = false
                    }
                },
                enabled = !isLoading && roomName.isNotBlank() && nickname.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(colors = listOf(PurpleVibrant, Purple)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Создать",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Wisteria
            )
        }
    }
}
