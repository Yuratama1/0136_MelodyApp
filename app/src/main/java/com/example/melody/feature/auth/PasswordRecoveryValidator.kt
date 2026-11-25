package com.example.melody.feature.auth

object PasswordRecoveryValidator {
    fun validateEmail(email: String): String? = when {
        email.isBlank() -> EMAIL_REQUIRED
        !EMAIL_PATTERN.matches(email.trim()) -> EMAIL_INVALID
        else -> null
    }

    fun validateReset(email: String, otp: String, password: String, confirmPassword: String): String? = when {
        validateEmail(email) != null -> validateEmail(email)
        otp.isBlank() -> OTP_REQUIRED
        password.isBlank() -> PASSWORD_REQUIRED
        password.length < MIN_PASSWORD_LENGTH -> PASSWORD_TOO_SHORT
        confirmPassword.isBlank() -> CONFIRM_PASSWORD_REQUIRED
        confirmPassword != password -> PASSWORDS_DO_NOT_MATCH
        else -> null
    }

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private const val MIN_PASSWORD_LENGTH = 8

    const val EMAIL_REQUIRED = "Email wajib diisi."
    const val EMAIL_INVALID = "Masukkan alamat email yang valid."
    const val OTP_REQUIRED = "Kode OTP wajib diisi."
    const val PASSWORD_REQUIRED = "Password baru wajib diisi."
    const val PASSWORD_TOO_SHORT = "Password baru minimal 8 karakter."
    const val CONFIRM_PASSWORD_REQUIRED = "Konfirmasi password baru wajib diisi."
    const val PASSWORDS_DO_NOT_MATCH = "Password baru dan konfirmasi password tidak sama."
}
