package com.example.studcampapp.feature.room.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.feature.room.ui.RoomViewModel
import com.example.studcampapp.ui.theme.*

@Composable
fun JoinRoomScreen(
    onBack: () -> Unit,
    onJoined: () -> Unit,
    viewModel: RoomViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    var ip by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    val discoveredRooms = viewModel.discoveredRooms

    LaunchedEffect(Unit) { viewModel.startDiscovery(context) }
    DisposableEffect(Unit) { onDispose { viewModel.stopDiscovery() } }

    LaunchedEffect(viewModel.navigateToChat) {
        if (viewModel.navigateToChat) {
            viewModel.onNavigated()
            onJoined()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
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
                tint = appColors.accent
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
                color = appColors.textPrimary
            )

            Text(
                text = "Введи IP-адрес сервера и свой ник",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = appColors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Комнаты поблизости",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textSecondary
                )
                if (discoveredRooms.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = Purple,
                        strokeWidth = 1.5.dp
                    )
                }
            }

            if (discoveredRooms.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    discoveredRooms.forEach { room ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = appColors.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ip = room.ip }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = room.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = room.ip,
                                        fontSize = 12.sp,
                                        fontFamily = InterFontFamily,
                                        color = appColors.textSecondary
                                    )
                                }
                                Text(
                                    text = "Выбрать",
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily,
                                    color = appColors.accent
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Идёт поиск...",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = appColors.textSecondary.copy(alpha = 0.5f)
                )
            }

            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("IP-адрес", fontFamily = InterFontFamily, color = appColors.textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors(appColors),
                singleLine = true
            )

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Твой ник", fontFamily = InterFontFamily, color = appColors.textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors(appColors),
                singleLine = true
            )

            if (viewModel.error != null) {
                Text(
                    text = viewModel.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (ip.isBlank()) return@Button
                    val trimmedIp = ip.trim()
                    viewModel.join(trimmedIp, 8080, nickname.trim(), trimmedIp)
                },
                enabled = !viewModel.isLoading && ip.isNotBlank(),
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
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Подключиться",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors(appColors: AppColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple,
    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
    focusedTextColor = appColors.textPrimary,
    unfocusedTextColor = appColors.textPrimary,
    cursorColor = appColors.accent
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.runtime.Composable
private fun JoinRoomScreenPreview() {
    com.example.studcampapp.ui.theme.AppTheme {
        JoinRoomScreen(onBack = {}, onJoined = {})
    }
}
