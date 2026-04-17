package com.example.studcampapp.backend.session

import com.example.studcampapp.model.RoomState
import com.example.studcampapp.model.User
import com.example.studcampapp.model.dto.JoinRequest
import com.example.studcampapp.model.dto.JoinResponse
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionStore {
    private val mutex = Mutex()
    private val usersById = LinkedHashMap<String, User>()
    private val sessionsById = LinkedHashMap<String, String>()

    suspend fun join(request: JoinRequest): JoinResponse = mutex.withLock {
        val user = User(
            id = UUID.randomUUID().toString(),
            login = request.login,
            firstName = request.firstName,
            lastName = request.lastName,
            middleName = request.middleName,
            avatarUrl = request.avatarUrl,
            phone = request.phone,
            email = request.email
        )
        val sessionId = UUID.randomUUID().toString()

        usersById[user.id] = user
        sessionsById[sessionId] = user.id

        JoinResponse(
            sessionId = sessionId,
            user = user,
            state = RoomState(
                users = usersById.values.toList(),
                messages = emptyList()
            )
        )
    }

    suspend fun getUserIdBySessionId(sessionId: String): String? = mutex.withLock {
        sessionsById[sessionId]
    }
}

