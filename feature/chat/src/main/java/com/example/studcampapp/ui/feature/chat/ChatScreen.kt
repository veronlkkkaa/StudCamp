package com.example.studcampapp.ui.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studcampapp.model.ChatMessage
import androidx.compose.ui.graphics.Color
import com.example.studcampapp.ui.theme.*
import java.time.LocalDateTime

@Composable
fun ChatScreen(onLeave: () -> Unit) {
    val fakeMessages = remember {
        mutableStateListOf(
            ChatMessage(1, "Гость 1", "Привет всем!", LocalDateTime.now()),
            ChatMessage(2, "Гость 2", "Привет!", LocalDateTime.now()),
            ChatMessage(3, "Гость 1", "Скидываю файл", LocalDateTime.now())
        )
    }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(fakeMessages.size) {
        if (fakeMessages.isNotEmpty()) {
            listState.animateScrollToItem(fakeMessages.size - 1)
        }
    }

    val bgColor = Color(0xFFF5F5F5)
    val surfaceColor = Color.White
    val textColor = Color(0xFF1A1A1A)
    val subtitleColor = Color(0xFF888888)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(bgColor)
    ) {
        // Топбар
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLeave) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Purple
                )
            }
            Text(
                text = "Комната",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = textColor
            )
        }

        // Сообщения
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(fakeMessages) { message ->
                MessageBubble(message)
            }
        }

        // Поле ввода
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Сообщение...",
                        fontFamily = InterFontFamily,
                        color = subtitleColor
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = Purple
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        fakeMessages.add(ChatMessage(fakeMessages.size + 1, "Я", inputText.trim(), LocalDateTime.now()))
                        inputText = ""
                    }
                })
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        fakeMessages.add(ChatMessage(fakeMessages.size + 1, "Я", inputText.trim(), LocalDateTime.now()))
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = if (inputText.isNotBlank()) Purple else subtitleColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isMe = message.author == "Я"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) Purple else Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!isMe) {
                Text(
                    text = message.author,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    color = Purple,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = message.text,
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = if (isMe) Color.White else Color(0xFF1A1A1A)
            )
        }
    }
}
