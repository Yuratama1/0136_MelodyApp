package com.example.melody.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationValidatorTest {
    @Test
    fun rejectsEachRequiredFieldWhenEmpty() {
        assertEquals(
            RegistrationValidator.DISPLAY_NAME_REQUIRED,
            RegistrationValidator.validate(" ", "melody@example.com", "password1", "password1"),
        )
        assertEquals(
            RegistrationValidator.EMAIL_REQUIRED,
            RegistrationValidator.validate("Melody", " ", "password1", "password1"),
        )
        assertEquals(
            RegistrationValidator.PASSWORD_REQUIRED,
            RegistrationValidator.validate("Melody", "melody@example.com", "", ""),
        )
        assertEquals(
            RegistrationValidator.CONFIRM_PASSWORD_REQUIRED,
            RegistrationValidator.validate("Melody", "melody@example.com", "password1", ""),
        )
    }

    @Test
    fun rejectsInvalidEmailAndShortPassword() {
        assertEquals(
            RegistrationValidator.EMAIL_INVALID,
            RegistrationValidator.validate("Melody", "not-an-email", "password1", "password1"),
        )
        assertEquals(
            RegistrationValidator.PASSWORD_TOO_SHORT,
            RegistrationValidator.validate("Melody", "melody@example.com", "short", "short"),
        )
    }

    @Test
    fun rejectsPasswordConfirmationMismatch() {
        assertEquals(
            RegistrationValidator.PASSWORDS_DO_NOT_MATCH,
            RegistrationValidator.validate("Melody", "melody@example.com", "password1", "password2"),
        )
    }

    @Test
    fun acceptsValidRegistrationData() {
        assertNull(
            RegistrationValidator.validate("Melody", "melody@example.com", "password1", "password1"),
        )
    }
}
