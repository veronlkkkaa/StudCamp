package com.example.studcampapp.feature.chat.ui

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
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
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studcampapp.feature.chat.ui.ChatViewModel
import com.example.studcampapp.model.*
import com.example.studcampapp.model.User
import com.example.studcampapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String {
    if (uri.scheme == "file") return uri.lastPathSegment ?: "Файл"
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
        in listOf("mp3", "aac", "m4a", "ogg", "wav", "amr", "opus") -> AttachmentType.Audio
        else -> AttachmentType.Document
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    onLeave: () -> Unit,
    onRoomInfo: () -> Unit,
    isHost: Boolean = false,
    onCloseRoom: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val messages = viewModel.messages
    val myUserId = viewModel.myUser?.id
    val uploadProgress = viewModel.uploadProgress
    val connectionError = viewModel.connectionError

    var inputText by remember { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<MessageAttachment?>(null) }
    var pickerActive by remember { mutableStateOf(false) }

    BackHandler(enabled = pickerActive) { pickerActive = false }
    val downloadProgress = remember { mutableStateMapOf<Int, Float>() }
    val listState = rememberLazyListState()

    // Pending download waiting for permission
    var pendingDownload by remember { mutableStateOf<Pair<Int, FileInfo>?>(null) }

    fun startDownload(msgId: Int, fileInfo: FileInfo) {
        val dmId = viewModel.downloadFile(context, fileInfo)
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
                delay(300
                )
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
        // Persist read permission so the URI stays accessible on IO threads / after process resume
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val type = when {
            mimeType.startsWith("image/") -> AttachmentType.Image
            mimeType.startsWith("video/") -> AttachmentType.Video
            mimeType.startsWith("audio/") -> AttachmentType.Audio
            else -> AttachmentType.Document
        }
        pendingAttachment = MessageAttachment(uri, type, getFileNameFromUri(context, uri))
    }

    // Voice recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var audioRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<java.io.File?>(null) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* next press will start recording */ }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder?.runCatching { stop(); release() }
            audioRecorder = null
        }
    }

    fun startRecording() {
        val file = java.io.File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        recordingFile = file
        try {
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                     else @Suppress("DEPRECATION") MediaRecorder()
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            audioRecorder = mr
            isRecording = true
        } catch (_: Exception) {
            recordingFile = null
        }
    }

    fun stopRecording(send: Boolean) {
        val file = recordingFile
        audioRecorder?.runCatching { stop(); release() }
        audioRecorder = null
        isRecording = false
        recordingSeconds = 0
        recordingFile = null
        if (send && file != null && file.exists() && file.length() > 0) {
            pendingAttachment = MessageAttachment(
                uri = android.net.Uri.fromFile(file),
                type = AttachmentType.Audio,
                fileName = file.name
            )
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (true) { delay(1000L); recordingSeconds++ }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        val attachment = pendingAttachment
        if (text.isBlank() && attachment == null) return
        inputText = ""
        pendingAttachment = null
        if (attachment != null) {
            val mimeType = context.contentResolver.getType(attachment.uri)
                ?: when (attachment.type) {
                    AttachmentType.Audio    -> "audio/mp4"
                    AttachmentType.Image    -> "image/jpeg"
                    AttachmentType.Video    -> "video/mp4"
                    AttachmentType.Document -> "application/octet-stream"
                }
            viewModel.uploadAndSend(context, text, attachment, mimeType)
        } else {
            viewModel.sendMessage(text)
        }
    }

    val isHostClosed = viewModel.isHostClosed

    val appColors = LocalAppColors.current
    val bgColor = appColors.chatBackground
    val surfaceColor = appColors.chatSurface
    val textColor = appColors.chatText
    val subtitleColor = appColors.chatSubtitle

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
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = viewModel.roomName,
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
            if (isHost) {
                var showRenameDialog by remember { mutableStateOf(false) }
                var showCloseDialog by remember { mutableStateOf(false) }

                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Переименовать комнату", tint = Purple)
                }
                if (showRenameDialog) {
                    var newRoomName by remember { mutableStateOf(viewModel.roomName) }
                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Переименовать комнату", fontFamily = InterFontFamily) },
                        text = {
                            OutlinedTextField(
                                value = newRoomName,
                                onValueChange = { newRoomName = it },
                                label = { Text("Название", fontFamily = InterFontFamily) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Purple,
                                    unfocusedBorderColor = Purple.copy(alpha = 0.4f),
                                    cursorColor = Purple
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showRenameDialog = false
                                    if (newRoomName.isNotBlank()) viewModel.renameRoom(newRoomName.trim())
                                }
                            ) {
                                Text("Сохранить", color = Purple, fontFamily = InterFontFamily)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialog = false }) {
                                Text("Отмена", fontFamily = InterFontFamily)
                            }
                        }
                    )
                }

                IconButton(onClick = { showCloseDialog = true }) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть комнату", tint = Purple)
                }
                if (showCloseDialog) {
                    AlertDialog(
                        onDismissRequest = { showCloseDialog = false },
                        title = { Text("Закрыть комнату?", fontFamily = InterFontFamily) },
                        text = { Text("Все участники будут отключены.", fontFamily = InterFontFamily) },
                        confirmButton = {
                            TextButton(onClick = { showCloseDialog = false; onCloseRoom() }) {
                                Text("Закрыть", color = MaterialTheme.colorScheme.error, fontFamily = InterFontFamily)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCloseDialog = false }) {
                                Text("Отмена", fontFamily = InterFontFamily)
                            }
                        }
                    )
                }
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
                        baseUrl = viewModel.baseUrl,
                        authHeader = viewModel.getAuthHeader(),
                        onDownload = {
                            message.fileInfo?.let { requestDownload(message.id, it) }
                        }
                    )
                }
            }
        }

        if (!connectionError.isNullOrBlank()) {
            Text(
                text = connectionError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
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

        if (isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { stopRecording(send = false) }) {
                    Icon(Icons.Default.Delete, "Отмена", tint = subtitleColor)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Spacer(Modifier.width(8.dp))
                val mins = recordingSeconds / 60
                val secs = recordingSeconds % 60
                Text(
                    text = "$mins:${secs.toString().padStart(2, '0')}",
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily,
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Отпустите для отправки",
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    color = subtitleColor
                )
            }
        } else {
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
                if (canSend) {
                    IconButton(onClick = { sendMessage() }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = Purple)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        val granted = context.checkSelfPermission(
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@detectTapGestures
                                        }
                                        startRecording()
                                        tryAwaitRelease()
                                        stopRecording(send = true)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, "Голосовое", tint = Purple)
                    }
                }
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
            viewModel.disconnect()
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
            .background(ChatOverlayDark),
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
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            color = appColors.chatSystemText,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(appColors.chatSystemBg)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean = false,
    downloadProgress: Float? = null,
    baseUrl: String = "",
    authHeader: String? = null,
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
                .background(if (isMe) Purple else LocalAppColors.current.chatBubbleOther)
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
                    baseUrl = baseUrl,
                    authHeader = authHeader,
                    onDownload = onDownload
                )
                if (message.text.isNotBlank()) Spacer(Modifier.height(4.dp))
            }

            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = if (isMe) Color.White else LocalAppColors.current.chatBubbleOtherText
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
    baseUrl: String = "",
    authHeader: String? = null,
    onDownload: () -> Unit = {}
) {
    val context = LocalContext.current
    val overlayBg = if (isMe) Color.White.copy(alpha = 0.15f) else Purple.copy(alpha = 0.08f)
    val iconTint = if (isMe) Color.White else Purple
    val textColor = if (isMe) Color.White else LocalAppColors.current.chatBubbleOtherText

    val displayType = attachment?.type ?: attachmentTypeFromName(fileInfo?.fileName ?: "")
    val displayName = attachment?.fileName ?: fileInfo?.fileName ?: "Файл"
    val canDownload = fileInfo != null && downloadProgress == null

    val remoteUrl: String? = fileInfo?.let {
        if (it.fileUrl.startsWith("http")) it.fileUrl else "$baseUrl${it.fileUrl}"
    }

    when (displayType) {
        AttachmentType.Image -> {
            val imageRequest = remember(attachment, fileInfo, authHeader) {
                ImageRequest.Builder(context)
                    .data(attachment?.uri ?: remoteUrl)
                    .crossfade(true)
                    .apply { authHeader?.let { h -> addHeader("Authorization", h) } }
                    .build()
            }
            Box {
                AsyncImage(
                    model = imageRequest,
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

        AttachmentType.Audio -> {
            AudioPlayerView(
                localUri = attachment?.uri,
                remoteUrl = remoteUrl,
                isMe = isMe,
                fileInfo = fileInfo,
                authHeader = authHeader,
                onDownload = onDownload
            )
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

private fun formatAudioDuration(ms: Int): String {
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
private fun AudioPlayerView(
    localUri: android.net.Uri?,
    remoteUrl: String?,
    isMe: Boolean,
    fileInfo: FileInfo?,
    authHeader: String? = null,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val iconTint = if (isMe) Color.White else Purple
    val overlayBg = if (isMe) Color.White.copy(alpha = 0.15f) else Purple.copy(alpha = 0.08f)
    val labelColor = if (isMe) Color.White.copy(alpha = 0.7f) else Color(0xFF888888)

    var prepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var durationMs by remember { mutableStateOf(0) }

    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    fun togglePlay() {
        if (prepared) {
            if (isPlaying) { player.pause(); isPlaying = false }
            else {
                player.start(); isPlaying = true
                scope.launch {
                    while (player.isPlaying) {
                        if (durationMs > 0) progress = player.currentPosition.toFloat() / durationMs
                        delay(80)
                    }
                    if (!player.isPlaying) { isPlaying = false; progress = 0f }
                }
            }
            return
        }
        try {
            player.reset()
            when {
                localUri != null -> player.setDataSource(localUri.path!!)
                remoteUrl != null -> {
                    val headers = authHeader
                        ?.let { mapOf("Authorization" to it) } ?: emptyMap()
                    player.setDataSource(context, android.net.Uri.parse(remoteUrl), headers)
                }
                else -> return
            }
            player.setOnPreparedListener { mp ->
                prepared = true
                durationMs = mp.duration
                mp.start()
                isPlaying = true
                scope.launch {
                    while (player.isPlaying) {
                        if (durationMs > 0) progress = player.currentPosition.toFloat() / durationMs
                        delay(80)
                    }
                    isPlaying = false; progress = 0f
                }
            }
            player.setOnCompletionListener { isPlaying = false; progress = 0f; prepared = false }
            player.prepareAsync()
        } catch (_: Exception) {}
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(overlayBg)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { togglePlay() }, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = iconTint,
                trackColor = iconTint.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (durationMs > 0) formatAudioDuration(durationMs) else "Голосовое",
                fontSize = 11.sp,
                fontFamily = InterFontFamily,
                color = labelColor
            )
        }
        if (fileInfo != null) {
            IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Download, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
        }
    }
}
