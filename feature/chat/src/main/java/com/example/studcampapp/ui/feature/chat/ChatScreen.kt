package com.example.studcampapp.ui.feature.chat

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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

private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String {
    var name = "Файл"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && col >= 0) name = cursor.getString(col)
    }
    return name
}

private fun attachmentTypeFromName(fileName: String): AttachmentType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif") -> AttachmentType.Image
        in listOf("mp4", "avi", "mov", "mkv", "webm", "3gp") -> AttachmentType.Video
        else -> AttachmentType.Document
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onLeave: () -> Unit, onRoomInfo: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val messages = ChatClient.messages
    val myUserId = ChatClient.myUser?.id
    val uploadProgress = ChatClient.uploadProgress

    var inputText by remember { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<MessageAttachment?>(null) }
    var pickerActive by remember { mutableStateOf(false) }

    BackHandler(enabled = pickerActive) { pickerActive = false }
    val downloadProgress = remember { mutableStateMapOf<Int, Float>() }
    val listState = rememberLazyListState()

    // Pending download waiting for permission
    var pendingDownload by remember { mutableStateOf<Pair<Int, FileInfo>?>(null) }

    fun startDownload(msgId: Int, fileInfo: FileInfo) {
        val dmId = ChatClient.downloadFile(context, fileInfo)
        coroutineScope.launch {
            downloadProgress[msgId] = 0f
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (true) {
                val cursor = dm.query(DownloadManager.Query().setFilterById(dmId))
                var done = false
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val dlBytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (total > 0) downloadProgress[msgId] = dlBytes.toFloat() / total.toFloat()
                        done = status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED
                    }
                }
                if (done) { downloadProgress.remove(msgId); break }
                delay(300)
            }
        }
    }

    val writePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingDownload?.let { (msgId, fi) -> startDownload(msgId, fi) }
        }
        pendingDownload = null
    }

    fun requestDownload(msgId: Int, fileInfo: FileInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) {
                pendingDownload = msgId to fileInfo
                writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        startDownload(msgId, fileInfo)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pickerActive = false
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val type = when {
            mimeType.startsWith("image/") -> AttachmentType.Image
            mimeType.startsWith("video/") -> AttachmentType.Video
            else -> AttachmentType.Document
        }
        pendingAttachment = MessageAttachment(uri, type, getFileNameFromUri(context, uri))
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage() {
        val text = inputText.trim()
        val attachment = pendingAttachment
        if (text.isBlank() && attachment == null) return
        inputText = ""
        pendingAttachment = null
        coroutineScope.launch {
            if (attachment != null) {
                val mimeType = context.contentResolver.getType(attachment.uri) ?: "application/octet-stream"
                val result = ChatClient.uploadFile(context, attachment.uri, attachment.fileName, mimeType)
                ChatClient.sendMessage(text, result.getOrNull())
            } else {
                ChatClient.sendMessage(text)
            }
        }
    }

    val isHostClosed = ChatClient.isHostClosed

    val bgColor = Color(0xFFF5F5F5)
    val surfaceColor = Color.White
    val textColor = Color(0xFF1A1A1A)
    val subtitleColor = Color(0xFF888888)

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = RoomStore.currentRoom.name,
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
            items(messages, key = { it.id }) { message ->
                if (message.isSystem) {
                    SystemMessageItem(text = message.text)
                } else {
                    MessageBubble(
                        message = message,
                        isMe = message.user.id == myUserId,
                        downloadProgress = downloadProgress[message.id],
                        onDownload = {
                            message.fileInfo?.let { requestDownload(message.id, it) }
                        }
                    )
                }
            }
        }

        // Pending attachment / upload progress bar
        val pending = pendingAttachment
        if (pending != null || uploadProgress != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEE8FF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (pending?.type) {
                            AttachmentType.Image -> Icons.Default.Image
                            AttachmentType.Video -> Icons.Default.PlayArrow
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uploadProgress != null) "Загрузка..." else pending?.fileName ?: "",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = textColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uploadProgress == null) {
                        IconButton(
                            onClick = { pendingAttachment = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, "Убрать",
                                tint = subtitleColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                if (uploadProgress != null) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Purple,
                        trackColor = Purple.copy(alpha = 0.2f)
                    )
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
            IconButton(
                onClick = {
                    pickerActive = true
                    filePickerLauncher.launch(arrayOf("image/*", "video/*", "application/*", "audio/*"))
                },
                enabled = uploadProgress == null
            ) {
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
            val canSend = (inputText.isNotBlank() || pendingAttachment != null) && uploadProgress == null
            IconButton(onClick = { sendMessage() }, enabled = canSend) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Отправить",
                    tint = if (canSend) Purple else subtitleColor.copy(alpha = 0.4f)
                )
            }
        }
    } // end Column

    AnimatedVisibility(
        visible = isHostClosed,
        enter = fadeIn() + scaleIn(
            initialScale = 0.92f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    ) {
        HostClosedOverlay(onLeave = {
            ChatClient.disconnect()
            onLeave()
        })
    }

    } // end Box
}

@Composable
private fun HostClosedOverlay(onLeave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF9B59B6), Color(0xFF2D1B69))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Сессия завершена",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Хост закрыл комнату.\nВсе участники были отключены.",
                fontSize = 15.sp,
                fontFamily = InterFontFamily,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onLeave,
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
                            Brush.horizontalGradient(
                                colors = listOf(
                                    PurpleVibrant,
                                    Purple
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Вернуться к чатам",
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

@Composable
fun SystemMessageItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            color = Color(0xFF999999),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEEEEEE))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean = false,
    downloadProgress: Float? = null,
    onLongPress: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
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

            val hasFile = message.attachment != null || message.fileInfo != null
            if (hasFile) {
                AttachmentView(
                    attachment = message.attachment,
                    fileInfo = message.fileInfo,
                    isMe = isMe,
                    downloadProgress = downloadProgress,
                    onDownload = onDownload
                )
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
                    Icon(
                        Icons.Default.Done,
                        null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentView(
    attachment: MessageAttachment?,
    fileInfo: FileInfo?,
    isMe: Boolean,
    downloadProgress: Float? = null,
    onDownload: () -> Unit = {}
) {
    val overlayBg = if (isMe) Color.White.copy(alpha = 0.15f) else Purple.copy(alpha = 0.08f)
    val iconTint = if (isMe) Color.White else Purple
    val textColor = if (isMe) Color.White else Color(0xFF1A1A1A)

    val displayType = attachment?.type ?: attachmentTypeFromName(fileInfo?.fileName ?: "")
    val displayName = attachment?.fileName ?: fileInfo?.fileName ?: "Файл"
    val canDownload = fileInfo != null && downloadProgress == null

    val imageModel: Any? = when {
        attachment != null -> attachment.uri
        fileInfo != null -> {
            if (fileInfo.fileUrl.startsWith("http")) fileInfo.fileUrl
            else "${ChatClient.baseUrl}${fileInfo.fileUrl}"
        }
        else -> null
    }

    when (displayType) {
        AttachmentType.Image -> {
            Box {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                if (fileInfo != null) {
                    IconButton(
                        onClick = onDownload,
                        enabled = canDownload,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            if (downloadProgress != null) Icons.Default.Downloading else Icons.Default.Download,
                            contentDescription = "Скачать",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    color = iconTint,
                    trackColor = iconTint.copy(alpha = 0.2f)
                )
            }
        }

        AttachmentType.Video, AttachmentType.Document -> {
            val icon = if (displayType == AttachmentType.Video) Icons.Default.PlayArrow else Icons.Default.Description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(overlayBg)
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = displayName,
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (fileInfo != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier.size(32.dp),
                            enabled = canDownload
                        ) {
                            Icon(
                                if (downloadProgress != null) Icons.Default.Downloading else Icons.Default.Download,
                                contentDescription = "Скачать",
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (downloadProgress != null) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = iconTint,
                        trackColor = iconTint.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
