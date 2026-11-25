package com.example.melody.feature.auth

object LoginValidator {
    fun validate(email: String, password: String): String? = when {
        email.isBlank() -> EMAIL_REQUIRED
        !EMAIL_PATTERN.matches(email.trim()) -> EMAIL_INVALID
        password.isBlank() -> PASSWORD_REQUIRED
        else -> null
    }

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    const val EMAIL_REQUIRED = "Email wajib diisi."
    const val EMAIL_INVALID = "Masukkan alamat email yang valid."
    const val PASSWORD_REQUIRED = "Password wajib diisi."
}
