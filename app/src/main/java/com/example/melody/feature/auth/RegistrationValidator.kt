package com.example.melody.feature.auth

object RegistrationValidator {
    fun validate(displayName: String, email: String, password: String, confirmPassword: String): String? = when {
        displayName.isBlank() -> DISPLAY_NAME_REQUIRED
        email.isBlank() -> EMAIL_REQUIRED
        !EMAIL_PATTERN.matches(email.trim()) -> EMAIL_INVALID
        password.isBlank() -> PASSWORD_REQUIRED
        password.length < MIN_PASSWORD_LENGTH -> PASSWORD_TOO_SHORT
        confirmPassword.isBlank() -> CONFIRM_PASSWORD_REQUIRED
        confirmPassword != password -> PASSWORDS_DO_NOT_MATCH
        else -> null
    }

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private const val MIN_PASSWORD_LENGTH = 8
    const val DISPLAY_NAME_REQUIRED = "Nama tampilan wajib diisi."
    const val EMAIL_REQUIRED = "Email wajib diisi."
    const val EMAIL_INVALID = "Masukkan alamat email yang valid."
    const val PASSWORD_REQUIRED = "Password wajib diisi."
    const val PASSWORD_TOO_SHORT = "Password minimal 8 karakter."
    const val CONFIRM_PASSWORD_REQUIRED = "Konfirmasi password wajib diisi."
    const val PASSWORDS_DO_NOT_MATCH = "Password dan konfirmasi password tidak sama."
}
