package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.session.SessionStore
import com.example.studcampapp.model.dto.JoinResponse
import com.example.studcampapp.model.ws.WsClientEvent
import com.example.studcampapp.model.ws.WsServerEvent
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HostWebSocketTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    @Test
    fun ws_sendMessage_broadcastsNewMessageToAllClients() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val sessionA = join("alice")
        val sessionB = join("bob")

        val wsClient = createClient {
            install(WebSockets)
        }

        val wsA = wsClient.webSocketSession("/ws?sessionId=${sessionA.sessionId}")
        val wsB = wsClient.webSocketSession("/ws?sessionId=${sessionB.sessionId}")

        wsA.send(Frame.Text(json.encodeToString(WsClientEvent.serializer(), WsClientEvent.SendMessage("hello"))))

        val eventA = readUntilNewMessage(wsA)
        val eventB = readUntilNewMessage(wsB)

        assertNotNull(eventA)
        assertNotNull(eventB)
        assertEquals("hello", eventA?.message?.text)
        assertEquals("hello", eventB?.message?.text)

        wsA.close()
        wsB.close()
    }

    @Test
    fun ws_disconnect_broadcastsUserLeft() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val sessionA = join("alice")
        val sessionB = join("bob")

        val wsClient = createClient {
            install(WebSockets)
        }

        val wsA = wsClient.webSocketSession("/ws?sessionId=${sessionA.sessionId}")
        val wsB = wsClient.webSocketSession("/ws?sessionId=${sessionB.sessionId}")

        wsB.close()
        val leftEvent = readUntilUserLeft(wsA)

        assertNotNull(leftEvent)
        assertEquals(sessionB.user.id, leftEvent?.userId)

        wsA.close()
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.join(login: String): JoinResponse {
        val response = client.post("/join") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login": "$login",
                  "firstName": null,
                  "lastName": null,
                  "middleName": null,
                  "avatarUrl": null,
                  "phone": null,
                  "email": null
                }
                """.trimIndent()
            )
        }

        return runBlocking {
            json.decodeFromString(response.bodyAsText())
        }
    }

    private suspend fun readUntilNewMessage(
        session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
    ): WsServerEvent.NewMessage? {
        return withTimeout(5_000) {
            var found: WsServerEvent.NewMessage? = null
            while (found == null) {
                val frame = session.incoming.receive()
                if (frame !is Frame.Text) continue
                val event = runCatching {
                    json.decodeFromString(WsServerEvent.serializer(), frame.readText())
                }.getOrNull() ?: continue

                if (event is WsServerEvent.NewMessage) {
                    found = event
                }
            }
            found
        }
    }

    private suspend fun readUntilUserLeft(
        session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
    ): WsServerEvent.UserLeft? {
        return withTimeout(5_000) {
            var found: WsServerEvent.UserLeft? = null
            while (found == null) {
                val frame = session.incoming.receive()
                if (frame !is Frame.Text) continue
                val event = runCatching {
                    json.decodeFromString(WsServerEvent.serializer(), frame.readText())
                }.getOrNull() ?: continue

                if (event is WsServerEvent.UserLeft) {
                    found = event
                }
            }
            found
        }
    }
}

