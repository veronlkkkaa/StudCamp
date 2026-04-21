package com.example.studcampapp.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.feature.profile.ui.ProfileViewModel
import com.example.studcampapp.ui.theme.*

@Composable
fun EditProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val appColors = LocalAppColors.current
    val user = viewModel.currentUser ?: return
    val isGuest = viewModel.isGuest

    var username by remember { mutableStateOf(user.login) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Purple,
        unfocusedBorderColor = Purple.copy(alpha = 0.4f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = appColors.textPrimary,
        unfocusedTextColor = appColors.textPrimary,
        cursorColor = appColors.accent,
        focusedLabelColor = appColors.accent,
        unfocusedLabelColor = appColors.textSecondary
    )
    val fieldShape = RoundedCornerShape(16.dp)

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
                text = "Редактировать профиль",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = appColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.removePrefix("@"); usernameError = null },
                label = { Text("Никнейм", fontFamily = InterFontFamily) },
                isError = usernameError != null,
                supportingText = usernameError?.let { { Text(it, fontFamily = InterFontFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = fieldColors,
                singleLine = true
            )

            if (!isGuest) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Номер телефона", fontFamily = InterFontFamily) },
                    placeholder = { Text("+7 999 999 99 99", fontFamily = InterFontFamily, color = appColors.textSecondary.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors,
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (username.isBlank()) {
                        usernameError = "Никнейм не может быть пустым"
                        return@Button
                    }
                    viewModel.updateProfile(
                        login = username.trim(),
                        phone = if (isGuest) "" else phone.trim()
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
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
                    Text(
                        "Сохранить",
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
