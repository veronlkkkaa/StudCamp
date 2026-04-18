package com.example.studcampapp.backend.session

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
    private val sessionsById = LinkedHashMap<String, String>()
    private val messages = ArrayDeque<ChatMessage>()
    private var nextMessageId = 1

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

    suspend fun addFileMessage(sessionId: String, fileId: String): ChatMessage? {
        val fileInfo = FileInfo(
            id = fileId,
            fileName = fileId,
            size = 0,
            fileUrl = "/files/$fileId"
        )
        return addMessage(
            sessionId = sessionId,
            text = "Shared file: $fileId",
            fileInfo = fileInfo
        )
    }

    suspend fun leave(sessionId: String): User? = mutex.withLock {
        val userId = sessionsById.remove(sessionId) ?: return@withLock null
        val hasOtherSessions = sessionsById.values.any { it == userId }
        if (hasOtherSessions) {
            return@withLock null
        }
        usersById.remove(userId)
    }

    private fun roomStateUnsafe(): RoomState {
        return RoomState(
            users = usersById.values.toList(),
            messages = messages.toList()
        )
    }
}