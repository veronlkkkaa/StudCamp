package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.session.SessionStore
import com.example.studcampapp.network.dto.JoinResponse
import com.example.studcampapp.network.dto.UploadResponse
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostModuleTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun health_returnsOkStatus() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\":\"ok\""))
    }

    @Test
    fun join_withValidPayload_returnsSessionAndState() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val response = client.post("/join") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login": "user_1",
                  "firstName": "Ivan",
                  "lastName": null,
                  "middleName": null,
                  "avatarUrl": null,
                  "phone": null,
                  "email": null
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        val joinResponse = runBlocking { json.decodeFromString<JoinResponse>(body) }
        assertEquals("user_1", joinResponse.user.login)
        assertEquals(1, joinResponse.state.users.size)
        assertTrue(joinResponse.sessionId.isNotBlank())
    }

    @Test
    fun join_withBlankLogin_returnsGuestUser() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val response = client.post("/join") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"   "}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val joinResponse = runBlocking { json.decodeFromString<JoinResponse>(response.bodyAsText()) }
        assertEquals("guest1", joinResponse.user.login)
    }

    @Test
    fun join_withInvalidPayload_returnsBadRequest() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val response = client.post("/join") {
            contentType(ContentType.Application.Json)
            setBody("{not-valid-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid join payload"))
    }

    @Test
    fun upload_then_download_file_returnsStoredContent() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val joinResponse = runBlocking {
            json.decodeFromString<JoinResponse>(
                client.post("/join") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                          "login": "uploader",
                          "firstName": null,
                          "lastName": null,
                          "middleName": null,
                          "avatarUrl": null,
                          "phone": null,
                          "email": null
                        }
                        """.trimIndent()
                    )
                }.bodyAsText()
            )
        }

        val uploadResponseRaw = client.post("/files/upload") {
            headers.append(HttpHeaders.Authorization, "Session ${joinResponse.sessionId}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = "hello-file".toByteArray(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"test.txt\"")
                                append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                            }
                        )
                    }
                )
            )
        }

        assertEquals(HttpStatusCode.OK, uploadResponseRaw.status)
        val uploadResponse = runBlocking {
            json.decodeFromString<UploadResponse>(uploadResponseRaw.bodyAsText())
        }

        val downloadResponse = client.get("/files/${uploadResponse.fileInfo.id}")
        assertEquals(HttpStatusCode.OK, downloadResponse.status)
        assertEquals("hello-file", downloadResponse.bodyAsText())
    }

    @Test
    fun download_unknown_file_returns404() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val response = client.get("/files/missing")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun auth_registerAndLogin_withSameNickname_returnsConflict() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login": "auth_user",
                  "firstName": "Auth",
                  "lastName": null,
                  "middleName": null,
                  "avatarUrl": null,
                  "phone": null,
                  "email": null
                }
                """.trimIndent()
            )
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login": "auth_user",
                  "firstName": "AuthUpdated",
                  "lastName": null,
                  "middleName": null,
                  "avatarUrl": null,
                  "phone": null,
                  "email": null
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, registerResponse.status)
        assertEquals(HttpStatusCode.Conflict, loginResponse.status)
        assertTrue(loginResponse.bodyAsText().contains("Nickname is already in use"))
    }

    @Test
    fun auth_register_withDifferentCaseLogins_allowsBoth() = testApplication {
        application {
            hostModule(SessionStore())
        }

        val firstResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"Petya","firstName":null,"lastName":null,"middleName":null,"avatarUrl":null,"phone":null,"email":null}""")
        }
        val secondResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"petya","firstName":null,"lastName":null,"middleName":null,"avatarUrl":null,"phone":null,"email":null}""")
        }

        assertEquals(HttpStatusCode.OK, firstResponse.status)
        assertEquals(HttpStatusCode.OK, secondResponse.status)

        val first = runBlocking { json.decodeFromString<JoinResponse>(firstResponse.bodyAsText()) }
        val second = runBlocking { json.decodeFromString<JoinResponse>(secondResponse.bodyAsText()) }
        assertTrue(first.user.id != second.user.id)
    }
}
