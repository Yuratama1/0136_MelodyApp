package com.example.melody.data.remote.auth.dto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCredentialRedactionTest {
    @Test
    fun requestAndSessionStringRepresentationsDoNotExposeSecrets() {
        val sensitiveValues = listOf(
            "test-password-secret" to AuthPasswordSignInRequestDto("user@example.com", "test-password-secret").toString(),
            "test-refresh-secret" to AuthRefreshTokenRequestDto("test-refresh-secret").toString(),
            "test-verification-secret" to AuthVerifyEmailRequestDto(
                "user@example.com",
                "test-verification-secret",
                "signup",
            ).toString(),
            "test-access-secret" to AuthSessionDto(accessToken = "test-access-secret").toString(),
        )

        sensitiveValues.forEach { (secret, representation) ->
            assertFalse(representation.contains(secret))
            assertTrue(representation.contains("REDACTED") || representation.contains("AuthSessionDto"))
        }
    }
}
