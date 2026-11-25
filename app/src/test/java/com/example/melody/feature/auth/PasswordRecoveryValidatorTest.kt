package com.example.melody.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordRecoveryValidatorTest {
    @Test
    fun validatesRecoveryEmail() {
        assertEquals(PasswordRecoveryValidator.EMAIL_REQUIRED, PasswordRecoveryValidator.validateEmail(" "))
        assertEquals(PasswordRecoveryValidator.EMAIL_INVALID, PasswordRecoveryValidator.validateEmail("not-an-email"))
        assertNull(PasswordRecoveryValidator.validateEmail("melody@example.com"))
    }

    @Test
    fun validatesOtpPasswordLengthAndConfirmation() {
        assertEquals(
            PasswordRecoveryValidator.OTP_REQUIRED,
            PasswordRecoveryValidator.validateReset("melody@example.com", "", "password1", "password1"),
        )
        assertEquals(
            PasswordRecoveryValidator.PASSWORD_REQUIRED,
            PasswordRecoveryValidator.validateReset("melody@example.com", "123456", "", ""),
        )
        assertEquals(
            PasswordRecoveryValidator.PASSWORD_TOO_SHORT,
            PasswordRecoveryValidator.validateReset("melody@example.com", "123456", "short", "short"),
        )
        assertEquals(
            PasswordRecoveryValidator.CONFIRM_PASSWORD_REQUIRED,
            PasswordRecoveryValidator.validateReset("melody@example.com", "123456", "password1", ""),
        )
        assertEquals(
            PasswordRecoveryValidator.PASSWORDS_DO_NOT_MATCH,
            PasswordRecoveryValidator.validateReset("melody@example.com", "123456", "password1", "password2"),
        )
        assertNull(
            PasswordRecoveryValidator.validateReset("melody@example.com", "123456", "password1", "password1"),
        )
    }
}
