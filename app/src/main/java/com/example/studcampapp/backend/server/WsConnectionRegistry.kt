package com.example.studcampapp.backend.server

import com.example.studcampapp.model.ws.WsServerEvent
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class WsConnectionRegistry {
    private val mutex = Mutex()
    private val sessions = LinkedHashMap<String, WebSocketServerSession>()

    suspend fun register(sessionId: String, session: WebSocketServerSession) {
        mutex.withLock {
            sessions[sessionId] = session
        }
    }

    suspend fun unregister(sessionId: String) {
        mutex.withLock {
            sessions.remove(sessionId)
        }
    }

    suspend fun sendTo(sessionId: String, event: WsServerEvent, json: Json) {
        val payload = json.encodeToString(WsServerEvent.serializer(), event)
        val session = mutex.withLock { sessions[sessionId] } ?: return
        runCatching {
            session.send(Frame.Text(payload))
        }
    }

    suspend fun broadcast(event: WsServerEvent, json: Json) {
        val payload = json.encodeToString(WsServerEvent.serializer(), event)
        val snapshot = mutex.withLock { sessions.values.toList() }
        snapshot.forEach { session ->
            runCatching {
                session.send(Frame.Text(payload))
            }
        }
    }
}


