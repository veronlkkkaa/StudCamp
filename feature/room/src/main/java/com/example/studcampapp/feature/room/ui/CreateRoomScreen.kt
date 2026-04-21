package com.example.studcampapp.feature.room.ui

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl
import com.example.studcampapp.feature.room.ui.RoomViewModel
import com.example.studcampapp.ui.theme.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateRoomScreen(
    onBack: () -> Unit,
    onRoomCreated: () -> Unit,
    onStartHost: (String) -> Unit = {},
    viewModel: RoomViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    var roomName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(UserRepositoryImpl.currentUser?.login ?: "") }

    LaunchedEffect(viewModel.navigateToChat) {
        if (viewModel.navigateToChat) {
            viewModel.onNavigated()
            onRoomCreated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
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
                color = appColors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Придумай название и свой ник",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = appColors.textSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = {
                    Text("Название комнаты", fontFamily = InterFontFamily, color = appColors.textSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    cursorColor = appColors.accent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = {
                    Text("Твой ник", fontFamily = InterFontFamily, color = appColors.textSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    cursorColor = appColors.accent
                ),
                singleLine = true
            )

            if (viewModel.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = viewModel.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (roomName.isBlank()) return@Button
                    val safeRoomName = roomName.trim()
                    val safeNickname = nickname.trim()
                    onStartHost(safeRoomName)
                    viewModel.join("127.0.0.1", 8080, safeNickname, safeRoomName)
                },
                enabled = !viewModel.isLoading && roomName.isNotBlank(),
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
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Создать",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = Color.White
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
                tint = appColors.accent
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.runtime.Composable
private fun CreateRoomScreenPreview() {
    com.example.studcampapp.ui.theme.AppTheme {
        CreateRoomScreen(onBack = {}, onRoomCreated = {})
    }
}
