package com.example.studcampapp.ui.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.studcampapp.model.*
import com.example.studcampapp.model.User
import com.example.studcampapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private fun getFileNameFromUri(context: android.content.Context, uri: android.net.Uri): String {
    var name = "Файл"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && col >= 0) name = cursor.getString(col)
    }
    return name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onLeave: () -> Unit, onRoomInfo: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val fakeMessages = remember {
        mutableStateListOf(
            ChatMessage(1, User("id1", "Гость 1"), "Привет всем!", LocalDateTime.now().minusMinutes(10),
                status = MessageStatus.Read, readBy = listOf("Мария С.", "Дмитрий К.")),
            ChatMessage(2, User("id2", "Мария С."), "Привет!", LocalDateTime.now().minusMinutes(5),
                status = MessageStatus.Read, readBy = listOf("Гость 1")),
            ChatMessage(3, User("id3", "Гость 1"), "Скидываю файл", LocalDateTime.now().minusMinutes(2),
                status = MessageStatus.Sent)
        )
    }

    var inputText by remember { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<MessageAttachment?>(null) }
    var readReceiptMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val type = when {
            mimeType.startsWith("image/") -> AttachmentType.Image
            mimeType.startsWith("video/") -> AttachmentType.Video
            else -> AttachmentType.Document
        }
        pendingAttachment = MessageAttachment(uri, type, getFileNameFromUri(context, uri))
    }

    LaunchedEffect(fakeMessages.size) {
        if (fakeMessages.isNotEmpty()) listState.animateScrollToItem(fakeMessages.size - 1)
    }

    fun sendMessage() {
        if (inputText.isBlank() && pendingAttachment == null) return
        val idx = fakeMessages.size
        fakeMessages.add(
            ChatMessage(
                id = idx + 1,
                user = User("me", "Я"),
                text = inputText.trim(),
                time = LocalDateTime.now(),
                status = MessageStatus.Sending,
                attachment = pendingAttachment
            )
        )
        inputText = ""
        pendingAttachment = null
        coroutineScope.launch {
            delay(800)
            if (idx < fakeMessages.size) {
                fakeMessages[idx] = fakeMessages[idx].copy(status = MessageStatus.Sent)
            }
        }
    }

    val bgColor = Color(0xFFF5F5F5)
    val surfaceColor = Color.White
    val textColor = Color(0xFF1A1A1A)
    val subtitleColor = Color(0xFF888888)

    if (readReceiptMessage != null) {
        val msg = readReceiptMessage!!
        val allNames = RoomStore.currentRoom?.participants
            ?.filterNot { it.displayName == msg.author }
            ?.map { it.displayName } ?: emptyList()
        val notRead = allNames.filterNot { it in msg.readBy }

        ModalBottomSheet(onDismissRequest = { readReceiptMessage = null }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Статус сообщения",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = InterFontFamily,
                    color = textColor
                )
                Spacer(Modifier.height(16.dp))
                if (msg.readBy.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DoneAll, null, tint = Purple, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Прочитали (${msg.readBy.size})",
                            fontSize = 13.sp,
                            color = Purple,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    msg.readBy.forEach { name ->
                        Text(
                            "• $name",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 22.dp, top = 4.dp),
                            fontFamily = InterFontFamily,
                            color = textColor
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (notRead.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Done, null, tint = subtitleColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Не прочитали (${notRead.size})",
                            fontSize = 13.sp,
                            color = subtitleColor,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    notRead.forEach { name ->
                        Text(
                            "• $name",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 22.dp, top = 4.dp),
                            fontFamily = InterFontFamily,
                            color = subtitleColor
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLeave) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Purple)
            }
            TextButton(
                onClick = onRoomInfo,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = RoomStore.currentRoom?.name ?: "Комната",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = textColor
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(fakeMessages) { message ->
                MessageBubble(
                    message = message,
                    onLongPress = { readReceiptMessage = message }
                )
            }
        }

        val pending = pendingAttachment
        if (pending != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEE8FF))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (pending.type) {
                        AttachmentType.Image -> Icons.Default.Image
                        AttachmentType.Video -> Icons.Default.PlayArrow
                        AttachmentType.Document -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pending.fileName,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = { pendingAttachment = null },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, "Убрать", tint = subtitleColor, modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                filePickerLauncher.launch(arrayOf("image/*", "video/*", "application/*", "audio/*"))
            }) {
                Icon(Icons.Default.AttachFile, "Прикрепить файл", tint = Purple)
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text("Сообщение...", fontFamily = InterFontFamily, color = subtitleColor)
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
                keyboardActions = KeyboardActions(onSend = { sendMessage() })
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { sendMessage() },
                enabled = inputText.isNotBlank() || pendingAttachment != null
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Отправить",
                    tint = if (inputText.isNotBlank() || pendingAttachment != null) Purple
                    else subtitleColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, onLongPress: () -> Unit = {}) {
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
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
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
                Spacer(Modifier.height(2.dp))
            }

            val attachment = message.attachment
            if (attachment != null) {
                AttachmentView(attachment = attachment, isMe = isMe)
                if (message.text.isNotBlank()) Spacer(Modifier.height(4.dp))
            }

            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = if (isMe) Color.White else Color(0xFF1A1A1A)
                )
            }

            if (isMe) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (message.status) {
                        MessageStatus.Sending -> CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        MessageStatus.Sent -> Icon(
                            Icons.Default.Done,
                            null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        MessageStatus.Read -> Icon(
                            Icons.Default.DoneAll,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentView(attachment: MessageAttachment, isMe: Boolean) {
    val overlayBg = if (isMe) Color.White.copy(alpha = 0.15f) else Purple.copy(alpha = 0.08f)
    val iconTint = if (isMe) Color.White else Purple
    val textColor = if (isMe) Color.White else Color(0xFF1A1A1A)

    when (attachment.type) {
        AttachmentType.Image -> {
            AsyncImage(
                model = attachment.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        AttachmentType.Video -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(overlayBg)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = iconTint, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = attachment.fileName,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        AttachmentType.Document -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(overlayBg)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, null, tint = iconTint, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = attachment.fileName,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
