package com.example.studcampapp.ui.feature.chat

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.User
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChatScreen(
    onLeave: () -> Unit
) {
    val fakeMessages = listOf(
        ChatMessage(1, User("id1", "Гость 1"), "Привет всем!", LocalDateTime.now()),
        ChatMessage(2, User("id2", "Гость 2"), "Привет!", LocalDateTime.now()),
        ChatMessage(3, User("id3", "Гость 3"), "Скидываю файл", LocalDateTime.now())
    )

    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateOf(fakeMessages.toMutableList()) }

    fun sendMessage() {
        if (inputText.isNotEmpty()) {
            messages.value = (messages.value + ChatMessage(
                id = messages.value.size + 1,
                user = User("id4","Я"),
                text = inputText,
                time = LocalDateTime.now(),
                fileInfo = null
            )).toMutableList()
            inputText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLeave) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Выйти",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Комната",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.value) { message ->
                MessageBubble(message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Сообщение...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() })
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { sendMessage() },
                enabled = inputText.isNotEmpty()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            text = message.user.login,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
