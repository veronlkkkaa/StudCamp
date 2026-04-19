package com.example.studcampapp.backend.session

import com.example.studcampapp.network.dto.JoinRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun clear_removesAllSessionsUsersAndMessages() = runBlocking {
        val store = SessionStore()
        val joinResponse = store.join(
            JoinRequest(
                login = "clear_test",
                firstName = null,
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )
        store.addMessage(joinResponse.sessionId, "msg")

        store.clear()

        assertNull(store.getUserIdBySessionId(joinResponse.sessionId))
        val state = store.getRoomState()
        assertEquals(0, state.users.size)
        assertEquals(0, state.messages.size)
    }

    @Test
    fun join_withSameLogin_returnsNicknameConflict() = runBlocking {
        val store = SessionStore()

        store.join(
            JoinRequest(
                login = "same_user",
                firstName = "First",
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )

        val secondResult = runCatching {
            store.join(
                JoinRequest(
                    login = "same_user",
                    firstName = "Updated",
                    lastName = null,
                    middleName = null,
                    avatarUrl = null,
                    phone = null,
                    email = null
                )
            )
        }

        assertTrue(secondResult.isFailure)
        assertTrue(secondResult.exceptionOrNull() is NicknameOccupiedException)
    }

    @Test
    fun join_withDifferentLetterCase_allowsBothUsers() = runBlocking {
        val store = SessionStore()

        val first = store.join(
            JoinRequest(
                login = "Petya",
                firstName = null,
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )
        val second = store.join(
            JoinRequest(
                login = "petya",
                firstName = null,
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )

        assertTrue(first.user.id != second.user.id)
        assertEquals("Petya", first.user.login)
        assertEquals("petya", second.user.login)
    }

    @Test
    fun join_withBlankLogin_assignsGuestNickname() = runBlocking {
        val store = SessionStore()

        val first = store.join(
            JoinRequest(
                login = "   ",
                firstName = null,
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )
        val second = store.join(
            JoinRequest(
                login = "",
                firstName = null,
                lastName = null,
                middleName = null,
                avatarUrl = null,
                phone = null,
                email = null
            )
        )

        assertEquals("guest1", first.user.login)
        assertEquals("guest2", second.user.login)
    }
}
