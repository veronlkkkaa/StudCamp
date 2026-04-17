package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.session.SessionStore
import com.example.studcampapp.model.ws.WsClientEvent
import com.example.studcampapp.model.ws.WsServerEvent
import com.example.studcampapp.model.dto.JoinRequest
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json

class HostServer(
    private val sessionStore: SessionStore,
    private val host: String = "0.0.0.0",
    private val port: Int = 8080
) {
    private var engine: ApplicationEngine? = null

    @Synchronized
    fun start() {
        if (engine != null) return

        engine = embeddedServer(
            factory = CIO,
            host = host,
            port = port,
            module = {
                hostModule(sessionStore)
            }
        ).start(wait = false)
    }

    @Synchronized
    fun stop() {
        engine?.stop(gracePeriodMillis = 1_000, timeoutMillis = 2_000)
        engine = null
    }
}

fun Application.hostModule(sessionStore: SessionStore) {
    val wsJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }
    val connectionRegistry = WsConnectionRegistry()

    install(ContentNegotiation) {
        json(wsJson)
    }
    install(WebSockets)

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/join") {
            val request = runCatching { call.receive<JoinRequest>() }
                .getOrElse {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid join payload"))
                    return@post
                }

            if (request.login.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "login must not be blank"))
                return@post
            }

            val response = sessionStore.join(request)
            call.respond(HttpStatusCode.OK, response)
        }

        wsRoute(
            sessionStore = sessionStore,
            connectionRegistry = connectionRegistry,
            wsJson = wsJson
        )
    }
}

private fun Route.wsRoute(
    sessionStore: SessionStore,
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

        connectionRegistry.register(sessionId, this)
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
                        connectionRegistry.sendTo(
                            sessionId,
                            WsServerEvent.Error(
                                "FILE_NOT_READY",
                                "Upload API is not connected to WebSocket flow yet"
                            ),
                            wsJson
                        )
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

