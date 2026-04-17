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
import com.example.studcampapp.ui.theme.*

@Composable
fun JoinRoomScreen(
    onBack: () -> Unit,
    onJoined: (roomCode: String) -> Unit
) {
    var roomCode by remember { mutableStateOf("") }

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
                text = "Подключиться",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Введи код комнаты",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it },
                label = {
                    Text(
                        "Код комнаты",
                        fontFamily = InterFontFamily,
                        color = TextSecondary
                    )
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (roomCode.isNotBlank()) {
                        onJoined(roomCode.trim())
                    }
                },
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
