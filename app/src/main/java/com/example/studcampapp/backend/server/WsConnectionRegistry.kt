package com.example.studcampapp.backend.server

import android.util.Log
import com.example.studcampapp.network.ws.WsServerEvent
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class WsConnectionRegistry {
    private data class Connection(
        val userId: String,
        val session: WebSocketServerSession
    )

    private val mutex = Mutex()
    private val sessions = LinkedHashMap<String, Connection>()

    suspend fun register(sessionId: String, userId: String, session: WebSocketServerSession) {
        Log.d("StudCampBroadcast", "register session=$sessionId")
        mutex.withLock {
            sessions[sessionId] = Connection(userId = userId, session = session)
        }
    }

    suspend fun unregister(sessionId: String) {
        Log.d("StudCampBroadcast", "unregister session=$sessionId")
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
        Log.d("StudCampBroadcast", "broadcast ${event::class.simpleName} to ${snapshot.size} sessions: ${snapshot.keys}")
        coroutineScope {
            snapshot.forEach { (sessionId, connection) ->
                launch {
                    val sent = runCatching {
                        withTimeout(5_000L) {
                            connection.session.send(Frame.Text(payload))
                        }
                    }.isSuccess
                    if (!sent) {
                        Log.w("StudCampBroadcast", "failed to send to $sessionId, unregistering")
                        unregister(sessionId)
                    }
                }
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
