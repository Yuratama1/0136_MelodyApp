package com.example.melody.data.session

import com.example.melody.data.remote.auth.dto.AuthSessionDto
import com.example.melody.data.remote.auth.dto.AuthUserDto
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthSessionMapperTest {
    @Test
    fun mapsVerifiedLoginSessionAndDisplayName() {
        val session = AuthSessionMapper.fromLogin(verifiedResponse(), NOW)

        assertEquals("access-test", session.accessToken)
        assertEquals("refresh-test", session.refreshToken)
        assertEquals("bearer", session.tokenType)
        assertEquals("user-test", session.userId)
        assertEquals("user@example.com", session.email)
        assertEquals("Melody User", session.displayName)
        assertEquals(NOW + 3600, session.expiresAtEpochSeconds)
        assertFalse(session.toString().contains("access-test"))
        assertFalse(session.toString().contains("refresh-test"))
    }

    @Test
    fun acceptsEitherVerificationTimestamp() {
        val response = verifiedResponse().copy(user = verifiedResponse().user?.copy(emailConfirmedAt = null))

        val session = AuthSessionMapper.fromLogin(response, NOW)

        assertEquals("user-test", session.userId)
    }

    @Test
    fun rejectsUnverifiedOrUnknownVerificationStatus() {
        val response = verifiedResponse().copy(
            user = verifiedResponse().user?.copy(emailConfirmedAt = null, confirmedAt = null),
        )

        val exception = assertThrows(SessionResponseException::class.java) {
            AuthSessionMapper.fromLogin(response, NOW)
        }

        assertEquals(AuthSessionMapper.EMAIL_NOT_CONFIRMED, exception.message)
    }

    @Test
    fun rejectsIncompleteLoginSession() {
        val exception = assertThrows(SessionResponseException::class.java) {
            AuthSessionMapper.fromLogin(verifiedResponse().copy(refreshToken = null), NOW)
        }

        assertEquals(AuthSessionMapper.INCOMPLETE_RESPONSE, exception.message)
    }

    @Test
    fun refreshPreservesExistingUserAndRefreshTokenWhenOmitted() {
        val previous = AuthSessionMapper.fromLogin(verifiedResponse(), NOW)
        val refreshed = AuthSessionMapper.afterRefresh(
            AuthSessionDto(accessToken = "access-new", expiresIn = 1800),
            previous,
            NOW + 300,
        )

        assertEquals("access-new", refreshed.accessToken)
        assertEquals("refresh-test", refreshed.refreshToken)
        assertEquals(previous.userId, refreshed.userId)
        assertEquals(previous.email, refreshed.email)
        assertEquals(previous.displayName, refreshed.displayName)
        assertEquals(NOW + 2100, refreshed.expiresAtEpochSeconds)
    }

    private fun verifiedResponse() = AuthSessionDto(
        accessToken = "access-test",
        refreshToken = "refresh-test",
        tokenType = "bearer",
        expiresIn = 3600,
        user = AuthUserDto(
            id = "user-test",
            email = "user@example.com",
            emailConfirmedAt = "2026-01-01T00:00:00Z",
            confirmedAt = "2026-01-01T00:00:00Z",
            userMetadata = buildJsonObject { put("display_name", "Melody User") },
        ),
    )

    private companion object {
        const val NOW = 1_800_000_000L
    }
}
