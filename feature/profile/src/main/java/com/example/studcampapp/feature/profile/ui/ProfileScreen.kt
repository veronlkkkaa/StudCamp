package com.example.studcampapp.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.studcampapp.core.ui.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.feature.profile.ui.ProfileViewModel
import com.example.studcampapp.ui.theme.*

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = onBack,
    onEditProfile: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    val user = viewModel.currentUser
    val isGuest = viewModel.isGuest
    val localAvatarUri = viewModel.localAvatarUri
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.updateAvatar(uri)
    }

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = appColors.textPrimary
                )
            }
            Text(
                text = "Профиль",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = appColors.textPrimary
            )
        }

        if (user == null) return@Column

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .then(
                        if (!isGuest) Modifier.clickable { avatarPicker.launch("image/*") }
                        else Modifier
                    ),
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
                    Image(
                        painter = painterResource(id = R.drawable.cute),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isGuest && localAvatarUri == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Загрузить фото",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (!isGuest) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Нажмите, чтобы изменить",
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    color = appColors.textSecondary.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim().ifBlank { user.login },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "@${user.login}",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = appColors.accent
            )

            if (!user.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.phone.orEmpty(),
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = appColors.textSecondary
                )
            }

            if (!isGuest) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Purple.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "ID: ${user.id.take(8)}",
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "НАСТРОЙКИ",
                fontSize = 11.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = appColors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(icon = Icons.Default.Person, title = "Редактировать профиль", onClick = onEditProfile)
                    if (!isGuest) {
                        HorizontalDivider(color = Purple.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(icon = Icons.Default.Notifications, title = "Уведомления", onClick = {})
                        HorizontalDivider(color = Purple.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(icon = Icons.Default.Lock, title = "Конфиденциальность", onClick = {})
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (viewModel.hasRooms) {
                        showLogoutConfirm = true
                    } else {
                        viewModel.logout()
                        onLogout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x1FFF6B6B),
                    contentColor = Color(0xFFFF6B6B)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выйти",
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Выйти из аккаунта?", fontFamily = InterFontFamily) },
            text = {
                Text(
                    "Все сохранённые комнаты будут удалены. Вы уверены, что хотите выйти?",
                    fontFamily = InterFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error, fontFamily = InterFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Отмена", fontFamily = InterFontFamily)
                }
            }
        )
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Purple.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = appColors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontFamily = InterFontFamily,
                color = appColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = appColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.runtime.Composable
private fun ProfileScreenPreview() {
    com.example.studcampapp.ui.theme.AppTheme {
        ProfileScreen(onBack = {})
    }
}
