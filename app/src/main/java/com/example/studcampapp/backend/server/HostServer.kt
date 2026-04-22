package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.file.FileStore
import com.example.studcampapp.backend.session.NicknameOccupiedException
import com.example.studcampapp.backend.session.SessionStore
import com.example.studcampapp.network.HostConnectionConfig
import com.example.studcampapp.network.dto.JoinRequest
import com.example.studcampapp.network.dto.UploadResponse
import com.example.studcampapp.network.ws.WsClientEvent
import com.example.studcampapp.network.ws.WsServerEvent
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.core.readBytes
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class HostServer(
    private val sessionStore: SessionStore,
    private val fileStore: FileStore,
    private val nsdPublisher: NsdPublisher,
    private var roomName: String,
    private val host: String = "0.0.0.0",
    private val port: Int = HostConnectionConfig.DEFAULT_PORT
) {
    private var engine: ApplicationEngine? = null
    private val connectionRegistry = WsConnectionRegistry()
    private val wsJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    @Synchronized
    fun start() {
        if (engine != null) return

        val startedEngine = embeddedServer(
            factory = CIO,
            host = host,
            port = port,
            module = {
                hostModule(
                    sessionStore = sessionStore,
                    fileStore = fileStore,
                    connectionRegistry = connectionRegistry,
                    wsJson = wsJson,
                    onRenameRoom = { newName ->
                        roomName = newName
                        sessionStore.setRoomName(newName)
                        nsdPublisher.stop()
                        nsdPublisher.start(serviceName = newName, port = port)
                        connectionRegistry.broadcast(WsServerEvent.RoomRenamed(newName), wsJson)
                    }
                )
            }
        ).start(wait = false)

        engine = startedEngine
        nsdPublisher.start(serviceName = roomName, port = port)
        println("HostServer started on $host:$port")
    }

    @Synchronized
    fun stop() {
        runBlocking {
            connectionRegistry.broadcast(WsServerEvent.HostClosed, wsJson)
            connectionRegistry.closeAll(CloseReason(CloseReason.Codes.NORMAL, "Host stopped"))
            sessionStore.clear()
            fileStore.clear()
            nsdPublisher.stop()
        }

        engine?.stop(gracePeriodMillis = 1_000, timeoutMillis = 2_000)
        engine = null
    }
}

fun Application.hostModule(
    sessionStore: SessionStore,
    fileStore: FileStore = FileStore(File(System.getProperty("java.io.tmpdir"), "lyra-files-${UUID.randomUUID()}")),
    connectionRegistry: WsConnectionRegistry = WsConnectionRegistry(),
    wsJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    },
    onRenameRoom: suspend (String) -> Unit = {}
) {
    val maxFileSizeBytes = 100L * 1024L * 1024L

    install(ContentNegotiation) {
        json(wsJson)
    }
    install(WebSockets)

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/auth/register") {
            handleJoinOrAuth(call, sessionStore)
        }

        post("/auth/login") {
            handleJoinOrAuth(call, sessionStore)
        }

        // Backward-compatible alias for older clients.
        post("/join") {
            handleJoinOrAuth(call, sessionStore)
        }

        post("/room/rename") {
            val sessionId = extractSessionId(call)
            if (sessionId.isNullOrBlank() || sessionStore.getUserBySessionId(sessionId) == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid session"))
                return@post
            }
            val body = runCatching { call.receive<Map<String, String>>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid body"))
                return@post
            }
            val newName = body["name"]?.trim()
            if (newName.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "name must not be blank"))
                return@post
            }
            onRenameRoom(newName)
            call.respond(HttpStatusCode.OK, mapOf("name" to newName))
        }

        post("/files/upload") {
            val sessionId = extractSessionId(call)
            if (sessionId.isNullOrBlank() || sessionStore.getUserBySessionId(sessionId) == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid session"))
                return@post
            }

            var uploadedFileInfo: com.example.studcampapp.model.FileInfo? = null
            var tooLarge = false

            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem && uploadedFileInfo == null) {
                    val bytes = part.provider().readBytes()
                    if (bytes.size.toLong() > maxFileSizeBytes) {
                        tooLarge = true
                    } else {
                        val originalName = part.originalFileName ?: "upload.bin"
                        val mimeType = part.contentType?.toString() ?: "application/octet-stream"
                        uploadedFileInfo = fileStore.saveFile(
                            originalName = originalName,
                            mimeType = mimeType,
                            bytes = bytes
                        )
                    }
                }
                part.dispose()
            }

            if (tooLarge) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "File is larger than 100MB"))
                return@post
            }

            val fileInfo = uploadedFileInfo
            if (fileInfo == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file in multipart payload"))
                return@post
            }

            connectionRegistry.broadcast(WsServerEvent.FileShared(fileInfo), wsJson)
            call.respond(HttpStatusCode.OK, UploadResponse(fileInfo))
        }

        get("/files/{id}") {
            val fileId = call.parameters["id"]
            if (fileId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "file id is required"))
                return@get
            }

            val stored = fileStore.getStoredFile(fileId)
            if (stored == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
                return@get
            }

            val contentType = runCatching { ContentType.parse(stored.mimeType) }
                .getOrDefault(ContentType.Application.OctetStream)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, stored.fileInfo.fileName).toString()
            )
            call.response.header(HttpHeaders.ContentType, contentType.toString())
            call.respondFile(stored.file, configure = {
                // content type set through headers for broader compatibility
            })
        }

        wsRoute(
            sessionStore = sessionStore,
            fileStore = fileStore,
            connectionRegistry = connectionRegistry,
            wsJson = wsJson
        )
    }
}

private fun Route.wsRoute(
    sessionStore: SessionStore,
    fileStore: FileStore,
    connectionRegistry: WsConnectionRegistry,
    wsJson: Json
) {
    webSocket("/ws") {
        val sessionId = call.request.queryParameters["sessionId"]
        if (sessionId.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "sessionId is required"))
            return@webSocket
        }

        val user = sessionStore.getUserBySessionId(sessionId)
        if (user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "invalid sessionId"))
            return@webSocket
        }
        val userId = user.id

        connectionRegistry.register(sessionId, userId, this)
        connectionRegistry.sendTo(
            sessionId,
            WsServerEvent.RoomStateEvent(sessionStore.getRoomState()),
            wsJson
        )
        connectionRegistry.broadcast(WsServerEvent.UserJoined(user), wsJson)

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                val parseResult = runCatching {
                    wsJson.decodeFromString(WsClientEvent.serializer(), frame.readText())
                }
                if (parseResult.isFailure) {
                    connectionRegistry.sendTo(
                        sessionId,
                        WsServerEvent.Error("INVALID_EVENT", "Unable to parse client event"),
                        wsJson
                    )
                    continue
                }
                val clientEvent = parseResult.getOrThrow()

                when (clientEvent) {
                    WsClientEvent.Ping -> {
                        connectionRegistry.sendTo(sessionId, WsServerEvent.Pong, wsJson)
                    }

                    is WsClientEvent.SendMessage -> {
                        if (clientEvent.text.isBlank()) {
                            connectionRegistry.sendTo(
                                sessionId,
                                WsServerEvent.Error("EMPTY_MESSAGE", "message text must not be blank"),
                                wsJson
                            )
                            continue
                        }

                        val message = sessionStore.addMessage(
                            sessionId = sessionId,
                            text = clientEvent.text,
                            fileInfo = clientEvent.fileInfo
                        )

                        if (message == null) {
                            connectionRegistry.sendTo(
                                sessionId,
                                WsServerEvent.Error("INVALID_SESSION", "session is not active"),
                                wsJson
                            )
                            continue
                        }

                        connectionRegistry.broadcast(WsServerEvent.NewMessage(message), wsJson)
                    }

                    is WsClientEvent.SendFile -> {
                        val fileInfo = fileStore.getFileInfo(clientEvent.fileId)
                        if (fileInfo == null) {
                            connectionRegistry.sendTo(
                                sessionId,
                                WsServerEvent.Error("FILE_NOT_FOUND", "Unknown fileId"),
                                wsJson
                            )
                            continue
                        }

                        val message = sessionStore.addMessage(
                            sessionId = sessionId,
                            text = "Shared file: ${fileInfo.fileName}",
                            fileInfo = fileInfo
                        )
                        if (message == null) {
                            connectionRegistry.sendTo(
                                sessionId,
                                WsServerEvent.Error("INVALID_SESSION", "session is not active"),
                                wsJson
                            )
                            continue
                        }

                        connectionRegistry.broadcast(WsServerEvent.FileShared(fileInfo), wsJson)
                        connectionRegistry.broadcast(WsServerEvent.NewMessage(message), wsJson)
                    }
                }
            }
        } finally {
            connectionRegistry.unregister(sessionId)
            val leftUser = sessionStore.leave(sessionId)
            if (leftUser != null) {
                connectionRegistry.broadcast(WsServerEvent.UserLeft(leftUser.id), wsJson)
            }
        }
    }
}

private suspend fun handleJoinOrAuth(call: ApplicationCall, sessionStore: SessionStore) {
    val request = runCatching { call.receive<JoinRequest>() }
        .getOrElse {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid join payload"))
            return
        }

    val response = runCatching { sessionStore.join(request) }
        .getOrElse { error ->
            if (error is NicknameOccupiedException) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to (error.message ?: "Nickname is already in use")))
                return
            }
            throw error
        }

    call.respond(HttpStatusCode.OK, response)
}

private fun extractSessionId(call: ApplicationCall): String? {
    call.request.queryParameters["sessionId"]?.let { return it }
    val header = call.request.headers[HttpHeaders.Authorization] ?: return null
    return when {
        header.startsWith("Bearer ", ignoreCase = true) -> header.removePrefix("Bearer ").trim()
        header.startsWith("Session ", ignoreCase = true) -> header.removePrefix("Session ").trim()
        else -> null
    }
}
