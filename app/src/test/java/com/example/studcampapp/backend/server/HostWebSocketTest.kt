package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.session.SessionStore
import com.example.studcampapp.model.dto.JoinResponse
import com.example.studcampapp.model.dto.UploadResponse
import com.example.studcampapp.model.ws.WsClientEvent
import com.example.studcampapp.model.ws.WsServerEvent
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
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

    @Test
    fun ws_ping_returnsPong() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val session = join("alice")
        val wsClient = createClient {
            install(WebSockets)
        }
        val ws = wsClient.webSocketSession("/ws?sessionId=${session.sessionId}")

        ws.send(Frame.Text(json.encodeToString(WsClientEvent.serializer(), WsClientEvent.Ping)))
        val pong = readUntilPong(ws)

        assertNotNull(pong)
        ws.close()
    }

    @Test
    fun ws_sendFile_broadcastsFileShared() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val sessionA = join("alice")
        val sessionB = join("bob")
        val uploaded = uploadFile(sessionA.sessionId)

        val wsClient = createClient {
            install(WebSockets)
        }

        val wsA = wsClient.webSocketSession("/ws?sessionId=${sessionA.sessionId}")
        val wsB = wsClient.webSocketSession("/ws?sessionId=${sessionB.sessionId}")

        wsA.send(
            Frame.Text(
                json.encodeToString(
                    WsClientEvent.serializer(),
                    WsClientEvent.SendFile(uploaded.fileInfo.id)
                )
            )
        )

        val fileEventA = readUntilFileShared(wsA)
        val fileEventB = readUntilFileShared(wsB)

        assertNotNull(fileEventA)
        assertNotNull(fileEventB)
        assertEquals(uploaded.fileInfo.id, fileEventA?.file?.id)
        assertEquals(uploaded.fileInfo.id, fileEventB?.file?.id)

        wsA.close()
        wsB.close()
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

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.uploadFile(sessionId: String): UploadResponse {
        val response = client.post("/files/upload") {
            headers.append(HttpHeaders.Authorization, "Session $sessionId")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = "ws-file".toByteArray(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"ws.txt\"")
                                append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                            }
                        )
                    }
                )
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

    private suspend fun readUntilPong(
        session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
    ): WsServerEvent.Pong? {
        return withTimeout(5_000) {
            var found: WsServerEvent.Pong? = null
            while (found == null) {
                val frame = session.incoming.receive()
                if (frame !is Frame.Text) continue
                val event = runCatching {
                    json.decodeFromString(WsServerEvent.serializer(), frame.readText())
                }.getOrNull() ?: continue

                if (event is WsServerEvent.Pong) {
                    found = event
                }
            }
            found
        }
    }

    private suspend fun readUntilFileShared(
        session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
    ): WsServerEvent.FileShared? {
        return withTimeout(5_000) {
            var found: WsServerEvent.FileShared? = null
            while (found == null) {
                val frame = session.incoming.receive()
                if (frame !is Frame.Text) continue
                val event = runCatching {
                    json.decodeFromString(WsServerEvent.serializer(), frame.readText())
                }.getOrNull() ?: continue

                if (event is WsServerEvent.FileShared) {
                    found = event
                }
            }
            found
        }
    }
}

