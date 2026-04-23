package com.example.studcampapp.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

object ChatClient {
    private const val RECONNECT_DELAY_MS = 1_000L
    private const val NSD_REDISCOVERY_FAILURES_THRESHOLD = 3
    private const val NSD_REDISCOVERY_TIMEOUT_MS = 10_000L

    private data class OutboxEntry(
        val clientMsgId: String,
        val event: WsClientEvent,
        val enqueuedAt: Long,
        val attemptCount: Int = 0
    )

    val messages = mutableStateListOf<ChatMessage>()
    val participants = mutableStateListOf<User>()
    var myUser by mutableStateOf<User?>(null)
        private set
    var connectionError by mutableStateOf<String?>(null)
        private set
    var lastServerError by mutableStateOf<String?>(null)
        private set
    var uploadProgress by mutableStateOf<Float?>(null)
        private set
    var isHostClosed by mutableStateOf(false)
        private set
    var isConnected by mutableStateOf(false)
        private set
    var sessionInvalidated by mutableStateOf(false)
        private set
    var currentRoomName by mutableStateOf("")
        private set
    var currentRoomId: String = ""
        private set

    var onRoomRenamed: ((String) -> Unit)? = null

    private var sessionId: String? = null
    private var serverIp: String = ""
    private var serverPort: Int = HostConnectionConfig.DEFAULT_PORT
    private var connectJob: Job? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val outbox = mutableStateListOf<OutboxEntry>()
    private var tempIdCounter = 0

    val baseUrl: String get() = "http://$serverIp:$serverPort"
    fun getAuthHeader(): String? = sessionId?.let { "Session $it" }

    fun initContext(context: Context) {
        appContext = context.applicationContext
    }

    private val wsJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(wsJson) }
        install(WebSockets) {
            pingInterval = 15_000L
        }
    }

    suspend fun join(ip: String, port: Int, login: String): Result<Unit> = runCatching {
        serverIp = ip
        serverPort = port
        Log.d("StudCampWS", "client: join $ip:$port login=$login")
        val httpResponse = httpClient.post("http://$ip:$port/join") {
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
        }
        if (httpResponse.status.value !in 200..299) {
            val errorMap = runCatching { httpResponse.body<Map<String, String>>() }.getOrNull()
            val serverMsg = errorMap?.get("error") ?: ""
            Log.w("StudCampWS", "client: join failed HTTP ${httpResponse.status.value} msg=$serverMsg")
            throw Exception(serverMsg.ifBlank { "HTTP ${httpResponse.status.value}" })
        }
        val response: JoinResponse = httpResponse.body()
        sessionId = response.sessionId
        Log.d("StudCampWS", "client: join OK sessionId=${response.sessionId}")
        myUser = response.user
        currentRoomId = response.state.id
        withContext(Dispatchers.Main) {
            currentRoomName = response.state.name
            messages.clear()
            messages.addAll(response.state.messages)
            participants.clear()
            participants.addAll(response.state.users)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun connect() {
        val sid = sessionId ?: return
        connectJob?.cancel()
        connectJob = scope.launch {
            var consecutiveFailures = 0
            while (sessionId == sid && !isHostClosed) {
                Log.d("StudCampWS", "client: connecting to $serverIp:$serverPort, sessionId=$sid, attempt=$consecutiveFailures")
                var sessionExpired = false
                val wsResult = runCatching {
                    httpClient.webSocket("ws://$serverIp:$serverPort/ws?sessionId=$sid") {
                        withContext(Dispatchers.Main) { connectionError = null; isConnected = true }
                        Log.d("StudCampWS", "client: WS connected")
                        consecutiveFailures = 0
                        launch {
                            try {
                                while (isActive) {
                                    val entry = outbox.firstOrNull()
                                    if (entry == null) {
                                        delay(100)
                                        continue
                                    }
                                    val payload = runCatching {
                                        wsJson.encodeToString(WsClientEvent.serializer(), entry.event)
                                    }
                                    if (payload.isFailure) {
                                        Log.e("StudCampWS", "client: outbox encode failed, dropping ${entry.clientMsgId}", payload.exceptionOrNull())
                                        outbox.remove(entry)
                                        continue
                                    }
                                    val sendResult = runCatching { send(Frame.Text(payload.getOrThrow())) }
                                    if (sendResult.isSuccess) {
                                        Log.d("StudCampWS", "client: outbox sent ${entry.event::class.simpleName} clientMsgId=${entry.clientMsgId}")
                                        val idx = outbox.indexOf(entry)
                                        if (idx >= 0) outbox[idx] = entry.copy(attemptCount = entry.attemptCount + 1)
                                        delay(500)
                                    } else {
                                        Log.w("StudCampWS", "client: outbox send failed, will retry after reconnect", sendResult.exceptionOrNull())
                                        break
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.e("StudCampWS", "client: outbox worker died", e)
                            }
                        }
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val event = runCatching {
                                wsJson.decodeFromString(WsServerEvent.serializer(), frame.readText())
                            }.getOrNull() ?: continue
                            withContext(Dispatchers.Main) { handleEvent(event) }
                        }
                        val reason = closeReason.await()
                        if (reason?.code == CloseReason.Codes.VIOLATED_POLICY.code) {
                            sessionExpired = true
                        }
                    }
                }

                withContext(Dispatchers.Main) { isConnected = false }

                if (sessionExpired) {
                    Log.w("StudCampWS", "client: session invalidated by server")
                    withContext(Dispatchers.Main) { sessionInvalidated = true }
                    break
                }

                if (sessionId != sid || isHostClosed) break

                val wsError = wsResult.exceptionOrNull()
                if (wsError is CancellationException) break

                if (wsResult.isFailure) {
                    consecutiveFailures++
                    if (consecutiveFailures >= NSD_REDISCOVERY_FAILURES_THRESHOLD) {
                        Log.d("StudCampWS", "client: starting rediscovery after $consecutiveFailures failures")
                        val ctx = appContext
                        val roomId = currentRoomId
                        if (ctx != null && roomId.isNotBlank()) {
                            val found = tryRediscoverHost(ctx, roomId)
                            if (found != null) {
                                serverIp = found.first
                                serverPort = found.second
                                consecutiveFailures = 0
                                continue
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    connectionError = mapNetworkError(wsError ?: IllegalStateException("WebSocket disconnected"))
                }

                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendMessage(text: String, fileInfo: FileInfo? = null) {
        lastServerError = null
        val user = myUser ?: return
        val clientMsgId = UUID.randomUUID().toString()
        val tempId = --tempIdCounter
        val tempMsg = ChatMessage(
            id = tempId,
            user = user,
            text = text,
            time = java.time.LocalDateTime.now(),
            status = MessageStatus.Sending,
            fileInfo = fileInfo,
            clientMsgId = clientMsgId,
            isPending = true
        )
        messages.add(tempMsg)
        val event = WsClientEvent.SendMessage(text = text, fileInfo = fileInfo, clientMsgId = clientMsgId)
        outbox.add(OutboxEntry(clientMsgId = clientMsgId, event = event, enqueuedAt = System.currentTimeMillis()))
        Log.d("StudCampWS", "client: outbox enqueue clientMsgId=$clientMsgId event=${event::class.simpleName}")
        Log.d("StudCampWS", "client: outbox size=${outbox.size}")
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
            Log.d("StudCampFile", "client: upload start, name=$fileName size=${bytes.size} to $baseUrl")
            val response: UploadResponse = httpClient.post("$baseUrl/files/upload") {
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
            Log.d("StudCampFile", "client: upload OK, id=${response.fileInfo.id}, url=${response.fileInfo.fileUrl}")
            response.fileInfo
        }.also { result ->
            if (result.isFailure) {
                Log.e("StudCampFile", "client: upload failed", result.exceptionOrNull())
                withContext(Dispatchers.Main) { uploadProgress = null }
            }
        }
    }

    fun downloadFile(context: Context, fileInfo: FileInfo): Long {
        val url = when {
            fileInfo.fileUrl.startsWith("http") -> fileInfo.fileUrl
            fileInfo.fileUrl.startsWith("/") -> "$baseUrl${fileInfo.fileUrl}"
            else -> "$baseUrl/${fileInfo.fileUrl}"
        }
        val request = DownloadManager.Request(url.toUri())
            .setTitle(fileInfo.fileName)
            .setDescription("Скачивание файла")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileInfo.fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        sessionId?.let { request.addRequestHeader("Authorization", "Session $it") }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    suspend fun downloadToCache(context: Context, fileInfo: FileInfo): Result<java.io.File> = runCatching {
        val url = when {
            fileInfo.fileUrl.startsWith("http") -> fileInfo.fileUrl
            fileInfo.fileUrl.startsWith("/") -> "$baseUrl${fileInfo.fileUrl}"
            else -> "$baseUrl/${fileInfo.fileUrl}"
        }
        val cacheFile = java.io.File(context.cacheDir, fileInfo.fileName)
        if (cacheFile.exists() && cacheFile.length() > 0) return@runCatching cacheFile
        withContext(Dispatchers.IO) {
            val bytes: ByteArray = httpClient.get(url) {
                sessionId?.let { header(HttpHeaders.Authorization, "Session $it") }
            }.body()
            cacheFile.writeBytes(bytes)
        }
        cacheFile
    }

    suspend fun renameRoom(newName: String): Result<Unit> = runCatching {
        val sid = sessionId ?: throw IllegalStateException("Not connected")
        val response = httpClient.post("http://$serverIp:$serverPort/room/rename") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, "Session $sid")
            setBody(mapOf("name" to newName))
        }
        if (response.status.value !in 200..299) {
            val errorMap = runCatching { response.body<Map<String, String>>() }.getOrNull()
            throw Exception(errorMap?.get("error") ?: "HTTP ${response.status.value}")
        }
    }

    fun updateLogin(login: String) {
        val current = myUser ?: return
        val updated = current.copy(login = login)
        myUser = updated
        val idx = participants.indexOfFirst { it.id == current.id }
        if (idx >= 0) participants[idx] = updated
    }

    private suspend fun tryRediscoverHost(context: Context, roomId: String): Pair<String, Int>? {
        val targetServiceName = "${NsdDiscovery.SERVICE_NAME_PREFIX}$roomId"
        NsdDiscovery.start(context)
        val deadline = System.currentTimeMillis() + NSD_REDISCOVERY_TIMEOUT_MS
        var found: com.example.studcampapp.model.DiscoveredRoom? = null
        while (System.currentTimeMillis() < deadline) {
            found = withContext(Dispatchers.Main) {
                NsdDiscovery.rooms.firstOrNull { it.serviceName == targetServiceName }
            }
            if (found != null) break
            delay(500L)
        }
        NsdDiscovery.stop()
        return found?.let { it.ip to it.port }
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        sessionId = null
        myUser = null
        currentRoomId = ""
        tempIdCounter = 0
        outbox.clear()
        scope.launch(Dispatchers.Main) {
            currentRoomName = ""
            messages.clear()
            participants.clear()
            connectionError = null
            lastServerError = null
            isHostClosed = false
            isConnected = false
            sessionInvalidated = false
        }
    }

    private fun mapNetworkError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            e is java.net.ConnectException ||
            msg.contains("Connection refused", ignoreCase = true) -> "Не удалось подключиться к серверу"
            e is java.net.SocketTimeoutException ||
            msg.contains("timed out", ignoreCase = true) -> "Время подключения истекло"
            e is java.net.UnknownHostException -> "Неверный адрес сервера"
            else -> "Соединение прервано"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleEvent(event: WsServerEvent) {
        val extra = if (event is WsServerEvent.NewMessage) " hasFileInfo=${event.message.fileInfo != null}" else ""
        Log.d("StudCampWS", "client: received ${event::class.simpleName}$extra")
        when (event) {
            is WsServerEvent.RoomStateEvent -> {
                messages.clear()
                messages.addAll(event.state.messages)
                participants.clear()
                participants.addAll(event.state.users)
            }
            is WsServerEvent.NewMessage -> {
                val incomingMsg = event.message
                val clientMsgId = incomingMsg.clientMsgId

                if (clientMsgId != null) {
                    val outboxIdx = outbox.indexOfFirst { it.clientMsgId == clientMsgId }
                    if (outboxIdx >= 0) {
                        Log.d("StudCampWS", "client: outbox ack received clientMsgId=$clientMsgId")
                        outbox.removeAt(outboxIdx)
                        Log.d("StudCampWS", "client: outbox size=${outbox.size}")
                    }
                    val pendingIdx = messages.indexOfFirst { it.isPending && it.clientMsgId == clientMsgId }
                    if (pendingIdx >= 0) {
                        messages[pendingIdx] = incomingMsg
                        return
                    }
                }

                if (messages.none { it.id == incomingMsg.id }) {
                    messages.add(incomingMsg)
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
            is WsServerEvent.RoomRenamed -> {
                currentRoomName = event.name
                onRoomRenamed?.invoke(event.name)
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
            is WsServerEvent.Error -> {
                Log.e("StudCampWS", "client: received Error from server: code=${event.code} message=${event.message}")
                val pendingIdx = messages.indexOfLast { it.isPending && it.user.id == myUser?.id }
                if (pendingIdx >= 0) messages.removeAt(pendingIdx)
                lastServerError = "${event.code}: ${event.message}"
            }
            else -> {}
        }
    }
}
