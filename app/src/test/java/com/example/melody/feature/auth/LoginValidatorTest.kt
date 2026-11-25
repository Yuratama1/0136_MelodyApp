package com.example.melody.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginValidatorTest {
    @Test
    fun rejectsMissingOrInvalidEmail() {
        assertEquals(LoginValidator.EMAIL_REQUIRED, LoginValidator.validate("", "password"))
        assertEquals(LoginValidator.EMAIL_INVALID, LoginValidator.validate("invalid-email", "password"))
    }

    @Test
    fun rejectsEmptyPasswordAndAcceptsValidCredentials() {
        assertEquals(LoginValidator.PASSWORD_REQUIRED, LoginValidator.validate("user@example.com", " "))
        assertNull(LoginValidator.validate("user@example.com", "any-password"))
    }
}
