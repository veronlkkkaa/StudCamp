package com.example.studcampapp.feature.chat.ui

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.compose.foundation.clickable
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
import android.content.Intent
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
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

private fun getMimeTypeFromFileName(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf"  -> "application/pdf"
        "doc"  -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls"  -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt"  -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt"  -> "text/plain"
        "zip"  -> "application/zip"
        else   -> "application/octet-stream"
    }
}

private data class MediaViewState(
    val type: AttachmentType,
    val localUri: android.net.Uri?,
    val remoteUrl: String?,
    val authHeader: String?
)

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
    var mediaPreview by remember { mutableStateOf<MediaViewState?>(null) }
    var videoLoading by remember { mutableStateOf(false) }

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
                var showCloseDialog by remember { mutableStateOf(false) }

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
                        },
                        onMediaClick = { type, localUri, remoteUrl, fileInfo ->
                            if (type == AttachmentType.Video && localUri == null && fileInfo != null) {
                                coroutineScope.launch {
                                    videoLoading = true
                                    viewModel.downloadToCache(context, fileInfo)
                                        .onSuccess { file ->
                                            mediaPreview = MediaViewState(
                                                AttachmentType.Video,
                                                android.net.Uri.fromFile(file),
                                                null, null
                                            )
                                        }
                                    videoLoading = false
                                }
                            } else {
                                mediaPreview = MediaViewState(type, localUri, remoteUrl, viewModel.getAuthHeader())
                            }
                        },
                        onDocumentOpen = { fileInfo ->
                            coroutineScope.launch {
                                viewModel.downloadToCache(context, fileInfo)
                                    .onSuccess { file ->
                                        val uri = FileProvider.getUriForFile(
                                            context, "${context.packageName}.fileprovider", file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, getMimeTypeFromFileName(file.name))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching {
                                            context.startActivity(
                                                Intent.createChooser(intent, "Открыть с помощью")
                                            )
                                        }
                                    }
                            }
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
            IconButton(
                onClick = { if (canSend) sendMessage() },
                enabled = canSend
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = if (canSend) Purple else Purple.copy(alpha = 0.3f))
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

    if (videoLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    mediaPreview?.let { preview ->
        when (preview.type) {
            AttachmentType.Image -> FullscreenImageViewer(
                localUri = preview.localUri,
                remoteUrl = preview.remoteUrl,
                authHeader = preview.authHeader,
                onDismiss = { mediaPreview = null }
            )
            AttachmentType.Video -> FullscreenVideoPlayer(
                localUri = preview.localUri,
                remoteUrl = preview.remoteUrl,
                authHeader = preview.authHeader,
                onDismiss = { mediaPreview = null }
            )
            else -> {}
        }
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
    onDownload: () -> Unit = {},
    onMediaClick: (type: AttachmentType, localUri: android.net.Uri?, remoteUrl: String?, fileInfo: FileInfo?) -> Unit = { _, _, _, _ -> },
    onDocumentOpen: suspend (FileInfo) -> Unit = {}
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
                    onDownload = onDownload,
                    onMediaClick = onMediaClick,
                    onDocumentOpen = onDocumentOpen
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
    onDownload: () -> Unit = {},
    onMediaClick: (type: AttachmentType, localUri: android.net.Uri?, remoteUrl: String?, fileInfo: FileInfo?) -> Unit = { _, _, _, _ -> },
    onDocumentOpen: suspend (FileInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val docScope = rememberCoroutineScope()
    var isDocLoading by remember { mutableStateOf(false) }
    val overlayBg = if (isMe) Color.White.copy(alpha = 0.15f) else Purple.copy(alpha = 0.08f)
    val iconTint = if (isMe) Color.White else Purple
    val textColor = if (isMe) Color.White else LocalAppColors.current.chatBubbleOtherText

    val displayType = attachment?.type ?: attachmentTypeFromName(fileInfo?.fileName ?: "")
    val displayName = attachment?.fileName ?: fileInfo?.fileName ?: "Файл"
    val canDownload = fileInfo != null && downloadProgress == null

    val remoteUrl: String? = fileInfo?.let {
        when {
            it.fileUrl.startsWith("http") -> it.fileUrl
            it.fileUrl.startsWith("/") -> "$baseUrl${it.fileUrl}"
            else -> "$baseUrl/${it.fileUrl}"
        }
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
            Box(
                modifier = Modifier.clickable {
                    onMediaClick(AttachmentType.Image, attachment?.uri, remoteUrl, fileInfo)
                }
            ) {
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
            val isVideo = displayType == AttachmentType.Video
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(overlayBg)
                    .then(when {
                        isVideo -> Modifier.clickable {
                            onMediaClick(AttachmentType.Video, attachment?.uri, remoteUrl, fileInfo)
                        }
                        fileInfo != null -> Modifier.clickable(enabled = !isDocLoading) {
                            docScope.launch {
                                isDocLoading = true
                                onDocumentOpen(fileInfo)
                                isDocLoading = false
                            }
                        }
                        else -> Modifier
                    })
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
                        if (!isVideo && isDocLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp).padding(4.dp),
                                color = iconTint,
                                strokeWidth = 2.dp
                            )
                        } else {
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

@Composable
private fun FullscreenImageViewer(
    localUri: android.net.Uri?,
    remoteUrl: String?,
    authHeader: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val imageRequest = remember(localUri, remoteUrl, authHeader) {
                ImageRequest.Builder(context)
                    .data(localUri ?: remoteUrl)
                    .crossfade(true)
                    .apply { authHeader?.let { h -> addHeader("Authorization", h) } }
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {})
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Close, null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun FullscreenVideoPlayer(
    localUri: android.net.Uri?,
    remoteUrl: String?,
    authHeader: String?,
    onDismiss: () -> Unit
) {
    var isError by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isError) {
                Text("Не удалось воспроизвести видео", color = Color.White, fontFamily = InterFontFamily)
            } else {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnErrorListener { _, _, _ -> isError = true; true }
                            localUri?.let { setVideoURI(it) }
                            setOnPreparedListener { it.start() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Close, null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
