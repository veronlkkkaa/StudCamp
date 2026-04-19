package com.example.studcampapp.backend.server

import com.example.studcampapp.network.ws.WsServerEvent
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class WsConnectionRegistry {
    private data class Connection(
        val userId: String,
        val session: WebSocketServerSession
    )

    private val mutex = Mutex()
    private val sessions = LinkedHashMap<String, Connection>()

    suspend fun register(sessionId: String, userId: String, session: WebSocketServerSession) {
        mutex.withLock {
            sessions[sessionId] = Connection(userId = userId, session = session)
        }
    }

    suspend fun unregister(sessionId: String) {
        mutex.withLock {
            sessions.remove(sessionId)
        }
    }

    suspend fun sendTo(sessionId: String, event: WsServerEvent, json: Json) {
        val payload = json.encodeToString(WsServerEvent.serializer(), event)
        val connection = mutex.withLock { sessions[sessionId] } ?: return
        val sent = runCatching {
            connection.session.send(Frame.Text(payload))
        }.isSuccess
        if (!sent) {
            unregister(sessionId)
        }
    }

    suspend fun broadcast(event: WsServerEvent, json: Json) {
        val payload = json.encodeToString(WsServerEvent.serializer(), event)
        val snapshot = mutex.withLock { sessions.toMap() }
        snapshot.forEach { (sessionId, connection) ->
            val sent = runCatching {
                connection.session.send(Frame.Text(payload))
            }.isSuccess
            if (!sent) {
                unregister(sessionId)
            }
        }
    }

    suspend fun closeAll(reason: CloseReason) {
        val snapshot = mutex.withLock { sessions.toMap() }
        snapshot.forEach { (sessionId, connection) ->
            runCatching { connection.session.close(reason) }
            unregister(sessionId)
        }
    }
}
