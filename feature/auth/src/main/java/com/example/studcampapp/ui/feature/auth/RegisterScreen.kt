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
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showWelcome by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showWelcome,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "register_transition"
    ) { isWelcome ->
        if (isWelcome) {
            RegisterWelcomeScreen(username = username, onFinished = onRegisterSuccess)
        } else {
            RegisterFormScreen(
                firstName = firstName, onFirstNameChange = { firstName = it },
                lastName = lastName, onLastNameChange = { lastName = it },
                username = username, onUsernameChange = { username = it },
                phone = phone, onPhoneChange = { phone = it },
                password = password, onPasswordChange = { password = it; passwordError = null },
                passwordError = passwordError,
                onRegister = {
                    if (firstName.isBlank() || lastName.isBlank() || username.isBlank()) return@RegisterFormScreen
                    if (!isPasswordValid(password)) {
                        passwordError = "Пароль должен содержать буквы и цифры"
                        return@RegisterFormScreen
                    }
                    UserStore.login(
                        User(
                            id = UUID.randomUUID().toString(),
                            login = username.trim().removePrefix("@"),
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
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
private fun RegisterWelcomeScreen(username: String, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(DarkBackground, DarkSurface))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Аккаунт создан,",
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
            CircularProgressIndicator(color = Wisteria, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun RegisterFormScreen(
    firstName: String, onFirstNameChange: (String) -> Unit,
    lastName: String, onLastNameChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordError: String?,
    onRegister: () -> Unit,
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
            text = "Регистрация",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Создайте новый аккаунт",
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Имя", fontFamily = InterFontFamily) },
                modifier = Modifier.weight(1f),
                shape = fieldShape,
                colors = fieldColors,
                singleLine = true
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Фамилия", fontFamily = InterFontFamily) },
                modifier = Modifier.weight(1f),
                shape = fieldShape,
                colors = fieldColors,
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { onUsernameChange(it.removePrefix("@")) },
            label = { Text("Никнейм", fontFamily = InterFontFamily) },
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
            onClick = onRegister,
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
                    "Зарегистрироваться",
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
