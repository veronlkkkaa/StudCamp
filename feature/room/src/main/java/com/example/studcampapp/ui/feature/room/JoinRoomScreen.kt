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
import com.example.studcampapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun JoinRoomScreen(
    onBack: () -> Unit,
    onJoined: () -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var nickname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 64.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Подключиться",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = TextPrimary
            )

            Text(
                text = "Введи адрес сервера и свой ник",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it; error = null },
                    label = { Text("IP-адрес", fontFamily = InterFontFamily, color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }; error = null },
                    label = { Text("Порт", fontFamily = InterFontFamily, color = TextSecondary) },
                    modifier = Modifier.width(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it; error = null },
                label = { Text("Твой ник", fontFamily = InterFontFamily, color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors(),
                singleLine = true
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val portInt = port.toIntOrNull() ?: return@Button
                    if (ip.isBlank() || nickname.isBlank()) return@Button
                    isLoading = true
                    error = null
                    scope.launch {
                        val trimmedIp = ip.trim()
                        ChatClient.join(trimmedIp, portInt, nickname.trim())
                            .onSuccess {
                                val roomName = "$trimmedIp:$portInt"
                                RoomStore.setRoomName(roomName)
                                RoomHistoryStore.saveRoom(
                                    SavedRoom(
                                        id = "$trimmedIp:$portInt",
                                        name = roomName,
                                        serverIp = trimmedIp,
                                        serverPort = portInt,
                                        myNickname = nickname.trim(),
                                        lastVisited = System.currentTimeMillis()
                                    )
                                )
                                ChatClient.connect()
                                onJoined()
                            }
                            .onFailure { e ->
                                error = "Не удалось подключиться: ${e.message}"
                            }
                        isLoading = false
                    }
                },
                enabled = !isLoading && ip.isNotBlank() && port.isNotBlank() && nickname.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
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
                            "Подключиться",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple,
    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Wisteria
)
