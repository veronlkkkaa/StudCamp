package com.example.studcampapp.backend.session

import android.annotation.SuppressLint
import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.RoomState
import com.example.studcampapp.model.User
import com.example.studcampapp.network.dto.JoinRequest
import com.example.studcampapp.network.dto.JoinResponse
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionStore {
    private companion object {
        const val MAX_MESSAGES = 500
    }

    private val mutex = Mutex()
    private val usersById = LinkedHashMap<String, User>()
    private val userIdByLoginKey = LinkedHashMap<String, String>()
    private val sessionsById = LinkedHashMap<String, String>()
    private val messages = ArrayDeque<ChatMessage>()
    private var nextMessageId = 1

    suspend fun join(request: JoinRequest): JoinResponse = mutex.withLock {
        val normalizedLogin = request.login.trim()
        val loginKey = normalizedLogin.lowercase()
        val existingUserId = userIdByLoginKey[loginKey]

        val user = if (existingUserId == null) {
            val createdUser = User(
                id = UUID.randomUUID().toString(),
                login = normalizedLogin,
                firstName = request.firstName,
                lastName = request.lastName,
                middleName = request.middleName,
                avatarUrl = request.avatarUrl,
                phone = request.phone,
                email = request.email
            )
            usersById[createdUser.id] = createdUser
            userIdByLoginKey[loginKey] = createdUser.id
            createdUser
        } else {
            val existing = usersById[existingUserId] ?: User(
                id = existingUserId,
                login = normalizedLogin,
                firstName = null,
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
            val updated = existing.copy(
                login = normalizedLogin,
                firstName = request.firstName ?: existing.firstName,
                lastName = request.lastName ?: existing.lastName,
                middleName = request.middleName ?: existing.middleName,
                avatarUrl = request.avatarUrl ?: existing.avatarUrl,
                phone = request.phone ?: existing.phone,
                email = request.email ?: existing.email
            )
            usersById[updated.id] = updated
            userIdByLoginKey[loginKey] = updated.id
            updated
        }

        val sessionId = UUID.randomUUID().toString()
        sessionsById[sessionId] = user.id

        JoinResponse(
            sessionId = sessionId,
            user = user,
            state = roomStateUnsafe()
        )
    }

    suspend fun getUserIdBySessionId(sessionId: String): String? = mutex.withLock {
        sessionsById[sessionId]
    }

    suspend fun getUserBySessionId(sessionId: String): User? = mutex.withLock {
        val userId = sessionsById[sessionId] ?: return@withLock null
        usersById[userId]
    }

    suspend fun getRoomState(): RoomState = mutex.withLock {
        roomStateUnsafe()
    }

    @SuppressLint("NewApi")
    suspend fun addMessage(sessionId: String, text: String, fileInfo: FileInfo? = null): ChatMessage? = mutex.withLock {
        val userId = sessionsById[sessionId] ?: return@withLock null
        val user = usersById[userId] ?: return@withLock null

        val message = ChatMessage(
            id = nextMessageId++,
            user = user,
            text = text,
            time = LocalDateTime.now(),
            fileInfo = fileInfo
        )
        messages.addLast(message)
        if (messages.size > MAX_MESSAGES) {
            messages.removeFirst()
        }
        message
    }

    suspend fun leave(sessionId: String): User? = mutex.withLock {
        val userId = sessionsById.remove(sessionId) ?: return@withLock null
        val hasOtherSessions = sessionsById.values.any { it == userId }
        if (hasOtherSessions) {
            return@withLock null
        }

        val removedUser = usersById.remove(userId)
        if (removedUser != null) {
            userIdByLoginKey.remove(removedUser.login.lowercase())
        }
        removedUser
    }

    suspend fun clear() = mutex.withLock {
        usersById.clear()
        userIdByLoginKey.clear()
        sessionsById.clear()
        messages.clear()
        nextMessageId = 1
    }

    private fun roomStateUnsafe(): RoomState {
        return RoomState(
            users = usersById.values.toList(),
            messages = messages.toList()
        )
    }
}