package com.example.studcampapp.backend.server

import com.example.studcampapp.backend.session.SessionStore
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
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
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        )
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
    }
}

