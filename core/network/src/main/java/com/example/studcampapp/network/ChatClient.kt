package com.example.studcampapp.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.MessageStatus
import com.example.studcampapp.model.RoomState
import com.example.studcampapp.model.User
import com.example.studcampapp.network.dto.JoinRequest
import com.example.studcampapp.network.dto.JoinResponse
import com.example.studcampapp.network.dto.UploadResponse
import com.example.studcampapp.network.ws.WsClientEvent
import com.example.studcampapp.network.ws.WsServerEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object ChatClient {
    val messages = mutableStateListOf<ChatMessage>()
    val participants = mutableStateListOf<User>()
    var myUser by mutableStateOf<User?>(null)
        private set
    var connectionError by mutableStateOf<String?>(null)
        private set
    var uploadProgress by mutableStateOf<Float?>(null)
        private set
    var isHostClosed by mutableStateOf(false)
        private set

    private var sessionId: String? = null
    private var serverIp: String = ""
    private var serverPort: Int = 8080
    private var connectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingEvents = Channel<WsClientEvent>(Channel.UNLIMITED)
    private var tempIdCounter = 0

    val baseUrl: String get() = "http://$serverIp:$serverPort"
    fun getAuthHeader(): String? = sessionId?.let { "Session $it" }

    private val wsJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(wsJson) }
        install(WebSockets)
    }

    suspend fun join(ip: String, port: Int, login: String): Result<Unit> = runCatching {
        serverIp = ip
        serverPort = port
        val response: JoinResponse = httpClient.post("http://$ip:$port/join") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                JoinRequest(
                    login = login,
                    firstName = null,
                    lastName = null,
                    middleName = null,
                    phone = null,
                    email = null
                )
            )
        }.body()
        sessionId = response.sessionId
        myUser = response.user
        withContext(Dispatchers.Main) {
            messages.clear()
            messages.addAll(response.state.messages)
            participants.clear()
            participants.addAll(response.state.users)
        }
    }

    fun connect() {
        val sid = sessionId ?: return
        connectJob?.cancel()
        connectJob = scope.launch {
            runCatching {
                httpClient.webSocket("ws://$serverIp:$serverPort/ws?sessionId=$sid") {
                    withContext(Dispatchers.Main) { connectionError = null }
                    launch {
                        pendingEvents.consumeEach { event ->
                            send(Frame.Text(wsJson.encodeToString(WsClientEvent.serializer(), event)))
                        }
                    }
                    incoming.consumeEach { frame ->
                        if (frame !is Frame.Text) return@consumeEach
                        val event = runCatching {
                            wsJson.decodeFromString(WsServerEvent.serializer(), frame.readText())
                        }.getOrNull() ?: return@consumeEach
                        withContext(Dispatchers.Main) { handleEvent(event) }
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) { connectionError = e.message }
            }
        }
    }

    fun sendMessage(text: String, fileInfo: FileInfo? = null) {
        val user = myUser ?: return
        val tempId = --tempIdCounter
        val tempMsg = ChatMessage(
            id = tempId,
            user = user,
            text = text,
            time = java.time.LocalDateTime.now(),
            status = MessageStatus.Sending,
            fileInfo = fileInfo,
            isPending = true
        )
        scope.launch(Dispatchers.Main) { messages.add(tempMsg) }
        pendingEvents.trySend(WsClientEvent.SendMessage(text = text, fileInfo = fileInfo))
    }

    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String
    ): Result<FileInfo> {
        val sid = sessionId ?: return Result.failure(IllegalStateException("Not connected"))
        return runCatching {
            withContext(Dispatchers.Main) { uploadProgress = 0f }
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            }
            val response: UploadResponse = httpClient.post("$baseUrl/upload") {
                header(HttpHeaders.Authorization, "Session $sid")
                setBody(MultiPartFormDataContent(formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }))
                onUpload { sent, total ->
                    if (total > 0) withContext(Dispatchers.Main) {
                        uploadProgress = sent.toFloat() / total.toFloat()
                    }
                }
            }.body()
            withContext(Dispatchers.Main) { uploadProgress = null }
            response.fileInfo
        }.also { result ->
            if (result.isFailure) withContext(Dispatchers.Main) { uploadProgress = null }
        }
    }

    fun downloadFile(context: Context, fileInfo: FileInfo): Long {
        val url = if (fileInfo.fileUrl.startsWith("http")) fileInfo.fileUrl
                  else "$baseUrl${fileInfo.fileUrl}"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileInfo.fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileInfo.fileName)
        sessionId?.let { request.addRequestHeader("Authorization", "Session $it") }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        sessionId = null
        myUser = null
        tempIdCounter = 0
        scope.launch(Dispatchers.Main) {
            messages.clear()
            participants.clear()
            connectionError = null
            isHostClosed = false
        }
    }

    private fun handleEvent(event: WsServerEvent) {
        when (event) {
            is WsServerEvent.RoomStateEvent -> {
                messages.clear()
                messages.addAll(event.state.messages)
                participants.clear()
                participants.addAll(event.state.users)
            }
            is WsServerEvent.NewMessage -> {
                val pendingIdx = messages.indexOfFirst { msg ->
                    msg.isPending &&
                    msg.user.id == event.message.user.id &&
                    msg.text == event.message.text
                }
                if (pendingIdx >= 0) messages.removeAt(pendingIdx)
                if (messages.none { it.id == event.message.id }) {
                    messages.add(event.message)
                }
            }
            is WsServerEvent.UserJoined -> {
                if (participants.none { it.id == event.user.id }) {
                    participants.add(event.user)
                }
                if (event.user.id != myUser?.id) {
                    val systemUser = User(id = "system", login = "system")
                    messages.add(ChatMessage(
                        id = --tempIdCounter,
                        user = systemUser,
                        text = "@${event.user.login} присоединился к чату",
                        time = java.time.LocalDateTime.now(),
                        isSystem = true
                    ))
                }
            }
            is WsServerEvent.HostClosed -> {
                isHostClosed = true
                connectJob?.cancel()
                connectJob = null
            }
            is WsServerEvent.UserLeft -> {
                val user = participants.find { it.id == event.userId }
                participants.removeAll { it.id == event.userId }
                if (user != null) {
                    val systemUser = User(id = "system", login = "system")
                    messages.add(ChatMessage(
                        id = --tempIdCounter,
                        user = systemUser,
                        text = "@${user.login} покинул чат",
                        time = java.time.LocalDateTime.now(),
                        isSystem = true
                    ))
                }
            }
            else -> {}
        }
    }
}
