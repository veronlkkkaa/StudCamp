package com.example.studcampapp.ui.feature.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studcampapp.model.User
import com.example.studcampapp.model.UserStore
import com.example.studcampapp.ui.theme.*
import kotlinx.coroutines.delay
import java.util.UUID

private fun isPasswordValid(password: String) =
    password.any { it.isLetter() } && password.any { it.isDigit() }

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showWelcome by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showWelcome,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "auth_transition"
    ) { isWelcome ->
        if (isWelcome) {
            WelcomeScreen(username = username, onFinished = onLoginSuccess)
        } else {
            FormScreen(
                username = username, onUsernameChange = { username = it },
                phone = phone, onPhoneChange = { phone = it },
                password = password, onPasswordChange = { password = it; passwordError = null },
                passwordError = passwordError,
                onLogin = {
                    if (username.isBlank()) return@FormScreen
                    if (!isPasswordValid(password)) {
                        passwordError = "Пароль должен содержать буквы и цифры"
                        return@FormScreen
                    }
                    UserStore.login(
                        User(
                            id = UUID.randomUUID().toString(),
                            firstName = "",
                            lastName = "",
                            username = username.trim().removePrefix("@"),
                            phone = phone.trim()
                        )
                    )
                    showWelcome = true
                },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun WelcomeScreen(username: String, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(DarkBackground, DarkSurface))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Добро пожаловать,",
                fontSize = 22.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "@$username",
                fontSize = 36.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = Wisteria,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FormScreen(
    username: String, onUsernameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordError: String?,
    onLogin: () -> Unit,
    onBack: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Purple,
        unfocusedBorderColor = Purple.copy(alpha = 0.4f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = Wisteria,
        focusedLabelColor = Wisteria,
        unfocusedLabelColor = TextSecondary
    )
    val fieldShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Вход",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Заполните данные для входа в аккаунт",
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { onUsernameChange(it.removePrefix("@")) },
            label = { Text("Никнейм", fontFamily = InterFontFamily) },
            prefix = { Text("@", fontFamily = InterFontFamily, color = Wisteria, fontWeight = FontWeight.Bold) },
            modifier = Modifier.fillMaxWidth(),
            shape = fieldShape,
            colors = fieldColors,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Номер телефона", fontFamily = InterFontFamily) },
            placeholder = { Text("+7 999 999 99 99", fontFamily = InterFontFamily, color = TextSecondary.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = fieldShape,
            colors = fieldColors,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль", fontFamily = InterFontFamily) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = fieldShape,
            isError = passwordError != null,
            supportingText = {
                Text(
                    text = passwordError ?: "Минимум одна буква и одна цифра",
                    fontFamily = InterFontFamily,
                    color = if (passwordError != null) MaterialTheme.colorScheme.error
                            else TextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            },
            colors = fieldColors,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onLogin,
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
                Text(
                    "Войти",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("← Назад", fontFamily = InterFontFamily, color = Wisteria)
        }
    }
}
