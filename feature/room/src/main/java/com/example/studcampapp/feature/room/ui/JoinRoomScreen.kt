package com.example.studcampapp.feature.room.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.data.repository.impl.UserRepositoryImpl
import com.example.studcampapp.model.DiscoveredRoom
import com.example.studcampapp.network.HostConnectionConfig
import com.example.studcampapp.network.NetworkEndpointResolver
import com.example.studcampapp.ui.theme.AppColors
import com.example.studcampapp.ui.theme.InterFontFamily
import com.example.studcampapp.ui.theme.LocalAppColors
import com.example.studcampapp.ui.theme.Purple
import com.example.studcampapp.ui.theme.PurpleVibrant

enum class JoinMode {
    DISCOVERY,
    MANUAL
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun JoinRoomScreen(
    onBack: () -> Unit,
    onJoined: () -> Unit,
    isHostRunning: Boolean = false,
    hostedRoomName: String = "",
    viewModel: RoomViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current

    val hostIp = remember { NetworkEndpointResolver.resolveHostIp() }
    val hostPort = HostConnectionConfig.DEFAULT_PORT

    var joinMode by remember { mutableStateOf(JoinMode.DISCOVERY) }
    var ip by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf(hostPort.toString()) }
    var selectedRoom by remember { mutableStateOf<DiscoveredRoom?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    val nickname = UserRepositoryImpl.currentUser?.login ?: ""
    val discoveredRooms = viewModel.discoveredRooms

    fun isOwnEndpoint(candidateIp: String, candidatePort: Int): Boolean {
        return isHostRunning && candidateIp == hostIp && candidatePort == hostPort
    }

    fun isOwnRoom(room: DiscoveredRoom): Boolean {
        return isOwnEndpoint(room.ip, room.port) || (isHostRunning && room.displayName == hostedRoomName)
    }

    fun validatePort(raw: String): Int? {
        val value = raw.toIntOrNull() ?: return null
        return if (value in 1..65535) value else null
    }

    LaunchedEffect(Unit) {
        viewModel.startDiscovery(context)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopDiscovery() }
    }

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
                .padding(top = 64.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Подключиться",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = appColors.textPrimary
            )

            Text(
                text = "Выберите комнату из списка или подключитесь вручную",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = appColors.textSecondary
            )

            TabRow(
                selectedTabIndex = joinMode.ordinal,
                containerColor = appColors.surface
            ) {
                Tab(
                    selected = joinMode == JoinMode.DISCOVERY,
                    onClick = {
                        joinMode = JoinMode.DISCOVERY
                        localError = null
                    },
                    text = { Text("Найти комнату", fontFamily = InterFontFamily) }
                )
                Tab(
                    selected = joinMode == JoinMode.MANUAL,
                    onClick = {
                        joinMode = JoinMode.MANUAL
                        localError = null
                    },
                    text = { Text("Вручную", fontFamily = InterFontFamily) }
                )
            }

            if (joinMode == JoinMode.DISCOVERY) {
                DiscoveryRoomsSection(
                    appColors = appColors,
                    rooms = discoveredRooms,
                    selectedRoom = selectedRoom,
                    isOwnRoom = ::isOwnRoom,
                    onSelect = { room ->
                        selectedRoom = room
                        ip = room.ip
                        portInput = room.port.toString()
                        localError = null
                    }
                )
            } else {
                ManualConnectionSection(
                    appColors = appColors,
                    ip = ip,
                    onIpChange = {
                        ip = it
                        localError = null
                    },
                    port = portInput,
                    onPortChange = {
                        portInput = it
                        localError = null
                    },
                    hostHint = "$hostIp:$hostPort"
                )
            }

            val errorText = localError ?: viewModel.error
            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    localError = null

                    when (joinMode) {
                        JoinMode.DISCOVERY -> {
                            val room = selectedRoom
                            if (room == null) {
                                localError = "Выберите комнату из списка"
                                return@Button
                            }
                            if (isOwnRoom(room)) {
                                localError = "Нельзя подключиться к своей комнате"
                                return@Button
                            }
                            viewModel.join(room.ip, room.port, nickname, room.displayName)
                        }

                        JoinMode.MANUAL -> {
                            val targetIp = ip.trim()
                            if (targetIp.isBlank()) {
                                localError = "Введите IP-адрес сервера"
                                return@Button
                            }
                            val targetPort = validatePort(portInput.trim())
                            if (targetPort == null) {
                                localError = "Порт должен быть числом от 1 до 65535"
                                return@Button
                            }
                            if (isOwnEndpoint(targetIp, targetPort)) {
                                localError = "Нельзя подключиться к своей комнате"
                                return@Button
                            }
                            viewModel.join(targetIp, targetPort, nickname, "$targetIp:$targetPort")
                        }
                    }
                },
                enabled = !viewModel.isLoading,
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
private fun DiscoveryRoomsSection(
    appColors: AppColors,
    rooms: List<DiscoveredRoom>,
    selectedRoom: DiscoveredRoom?,
    isOwnRoom: (DiscoveredRoom) -> Boolean,
    onSelect: (DiscoveredRoom) -> Unit
) {
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
        if (rooms.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = Purple,
                strokeWidth = 1.5.dp
            )
        }
    }

    if (rooms.isEmpty()) {
        Text(
            text = "Идёт поиск...",
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = appColors.textSecondary.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rooms, key = { "${it.serviceName}-${it.ip}-${it.port}" }) { room ->
            val ownRoom = isOwnRoom(room)
            val selected = selectedRoom == room
            val alpha = if (ownRoom) 0.55f else 1f

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) appColors.accent.copy(alpha = 0.12f) else appColors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !ownRoom) { onSelect(room) }
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(Color.Transparent)
                ) {
                    Text(
                        text = room.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = appColors.textPrimary.copy(alpha = alpha)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${room.ip}:${room.port}",
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        color = appColors.textSecondary.copy(alpha = alpha)
                    )
                    if (ownRoom) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ваша комната",
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualConnectionSection(
    appColors: AppColors,
    ip: String,
    onIpChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    hostHint: String
) {
    Text(
        text = "Ручное подключение",
        fontSize = 13.sp,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        color = appColors.textSecondary
    )

    OutlinedTextField(
        value = ip,
        onValueChange = onIpChange,
        label = { Text("IP-адрес", fontFamily = InterFontFamily, color = appColors.textSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = textFieldColors(appColors),
        singleLine = true
    )

    Spacer(Modifier.height(4.dp))

    OutlinedTextField(
        value = port,
        onValueChange = onPortChange,
        label = { Text("Порт", fontFamily = InterFontFamily, color = appColors.textSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = textFieldColors(appColors),
        singleLine = true
    )

    Spacer(Modifier.height(4.dp))
    HorizontalDivider(color = appColors.textSecondary.copy(alpha = 0.2f))
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Ваш host endpoint: $hostHint",
        fontSize = 12.sp,
        fontFamily = InterFontFamily,
        color = appColors.textSecondary.copy(alpha = 0.8f)
    )
}

@Composable
private fun textFieldColors(appColors: AppColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple,
    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
    focusedTextColor = appColors.textPrimary,
    unfocusedTextColor = appColors.textPrimary,
    cursorColor = appColors.accent
)

@RequiresApi(Build.VERSION_CODES.O)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.runtime.Composable
private fun JoinRoomScreenPreview() {
    com.example.studcampapp.ui.theme.AppTheme {
        JoinRoomScreen(onBack = {}, onJoined = {})
    }
}
