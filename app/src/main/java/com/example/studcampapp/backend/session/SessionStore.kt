package com.example.studcampapp.backend.session

import android.util.Log
import com.example.studcampapp.model.ChatMessage
import com.example.studcampapp.model.FileInfo
import com.example.studcampapp.model.RoomState
import com.example.studcampapp.model.User
import com.example.studcampapp.network.dto.JoinRequest
import com.example.studcampapp.network.dto.JoinResponse
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NicknameOccupiedException(message: String) : IllegalStateException(message)

class SessionStore {
    private companion object {
        const val MAX_MESSAGES = 500
    }

    private data class SessionEntry(
        val userId: String,
        val disconnectedAt: Instant? = null
    )

    private val mutex = Mutex()
    private val usersById = LinkedHashMap<String, User>()
    private val sessionsById = LinkedHashMap<String, SessionEntry>()
    private val messages = ArrayDeque<ChatMessage>()
    private var nextMessageId = 1
    private var nextGuestNumber = 1
    private var roomName: String = ""
    private var roomId: String = ""

    fun setInitialRoomState(name: String, id: String) {
        roomName = name
        roomId = id
    }

    suspend fun setRoomName(name: String) = mutex.withLock {
        roomName = name
    }

    suspend fun join(request: JoinRequest): JoinResponse = mutex.withLock {
        val login = request.login.trim().ifBlank { generateGuestLoginUnsafe() }
        ensureNicknameIsAvailableUnsafe(login)

        val user = User(
            id = UUID.randomUUID().toString(),
            login = login,
            firstName = request.firstName,
            lastName = request.lastName,
            middleName = request.middleName,
            avatarUrl = request.avatarUrl,
            phone = request.phone,
            email = request.email
        )
        val sessionId = UUID.randomUUID().toString()

        usersById[user.id] = user
        sessionsById[sessionId] = SessionEntry(userId = user.id)

        JoinResponse(
            sessionId = sessionId,
            user = user,
            state = roomStateUnsafe()
        )
    }

    suspend fun getUserIdBySessionId(sessionId: String): String? = mutex.withLock {
        sessionsById[sessionId]?.userId
    }

    suspend fun getUserBySessionId(sessionId: String): User? = mutex.withLock {
        val entry = sessionsById[sessionId] ?: return@withLock null
        usersById[entry.userId]
    }

    suspend fun getRoomState(): RoomState = mutex.withLock {
        roomStateUnsafe()
    }

    suspend fun addMessage(sessionId: String, text: String, fileInfo: FileInfo? = null): ChatMessage? = mutex.withLock {
        Log.d("StudCampSession", "addMessage: sessionId=$sessionId textLen=${text.length} hasFileInfo=${fileInfo != null} fileId=${fileInfo?.id}")
        val userId = sessionsById[sessionId]?.userId ?: return@withLock null
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
        Log.d("StudCampSession", "addMessage OK messageId=${message.id}")
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

    suspend fun markDisconnected(sessionId: String) = mutex.withLock {
        val entry = sessionsById[sessionId] ?: return@withLock
        Log.d("StudCampSession", "markDisconnected session=$sessionId")
        sessionsById[sessionId] = entry.copy(disconnectedAt = Instant.now())
    }

    // Returns true if the session was in disconnected state (= it's a reconnect).
    suspend fun markReconnected(sessionId: String): Boolean = mutex.withLock {
        val entry = sessionsById[sessionId] ?: return@withLock false
        if (entry.disconnectedAt == null) return@withLock false
        sessionsById[sessionId] = entry.copy(disconnectedAt = null)
        Log.d("StudCampSession", "markReconnected session=$sessionId wasDisconnected=true")
        true
    }

    suspend fun sweepExpiredSessions(graceMillis: Long): List<User> = mutex.withLock {
        val now = Instant.now()
        val toRemove = sessionsById.entries.filter { (_, entry) ->
            val disconnectedAt = entry.disconnectedAt ?: return@filter false
            java.time.Duration.between(disconnectedAt, now).toMillis() > graceMillis
        }
        Log.d("StudCampSession", "sweepExpiredSessions: checking ${sessionsById.size} sessions, ${toRemove.size} expired")
        val removedUsers = mutableListOf<User>()
        for ((sessionId, entry) in toRemove) {
            sessionsById.remove(sessionId)
            val stillHasSessions = sessionsById.values.any { it.userId == entry.userId }
            if (!stillHasSessions) {
                val user = usersById.remove(entry.userId)
                if (user != null) {
                    removedUsers.add(user)
                    Log.d("StudCampSession", "swept user=${user.login} userId=${user.id}")
                }
            }
        }
        removedUsers
    }

    suspend fun leave(sessionId: String): User? = mutex.withLock {
        val entry = sessionsById.remove(sessionId) ?: return@withLock null
        val hasOtherSessions = sessionsById.values.any { it.userId == entry.userId }
        if (hasOtherSessions) return@withLock null
        usersById.remove(entry.userId)
    }

    suspend fun clear() = mutex.withLock {
        usersById.clear()
        sessionsById.clear()
        messages.clear()
        nextMessageId = 1
        nextGuestNumber = 1
        roomName = ""
        roomId = ""
    }

    private fun ensureNicknameIsAvailableUnsafe(login: String) {
        if (usersById.values.any { it.login == login }) {
            throw NicknameOccupiedException("Nickname is already in use")
        }
    }

    private fun generateGuestLoginUnsafe(): String {
        while (true) {
            val candidate = "guest$nextGuestNumber"
            nextGuestNumber += 1
            if (usersById.values.none { it.login == candidate }) {
                return candidate
            }
        }
    }

    private fun roomStateUnsafe(): RoomState {
        return RoomState(
            users = usersById.values.toList(),
            messages = messages.toList(),
            name = roomName,
            id = roomId
        )
    }
}
