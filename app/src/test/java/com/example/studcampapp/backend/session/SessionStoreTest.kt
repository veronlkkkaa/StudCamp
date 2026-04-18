package com.example.studcampapp.backend.session

import com.example.studcampapp.model.dto.JoinRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStoreTest {

    @Test
    fun join_createsSessionAndReturnsUserInState() = runBlocking {
        val store = SessionStore()

        val response = store.join(
            JoinRequest(
                login = "user_1",
                firstName = "Ivan",
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )

        assertNotNull(response.sessionId)
        assertEquals("user_1", response.user.login)
        assertEquals(1, response.state.users.size)
        assertEquals(response.user.id, response.state.users.first().id)

        val storedUserId = store.getUserIdBySessionId(response.sessionId)
        assertEquals(response.user.id, storedUserId)
    }

    @Test
    fun getUserIdBySessionId_returnsNullForUnknownSession() = runBlocking {
        val store = SessionStore()

        val userId = store.getUserIdBySessionId("unknown-session-id")

        assertNull(userId)
    }

    @Test
    fun addMessage_keepsOnlyLast500Messages() = runBlocking {
        val store = SessionStore()
        val joinResponse = store.join(
            JoinRequest(
                login = "user_1",
                firstName = null,
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )

        repeat(501) { index ->
            store.addMessage(
                sessionId = joinResponse.sessionId,
                text = "msg-${index + 1}"
            )
        }

        val state = store.getRoomState()
        assertEquals(500, state.messages.size)
        assertEquals(2, state.messages.first().id)
        assertEquals(501, state.messages.last().id)
    }
}

