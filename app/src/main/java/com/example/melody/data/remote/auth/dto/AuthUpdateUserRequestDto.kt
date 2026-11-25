package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthUpdateUserRequestDto(
    val password: String,
) {
    override fun toString(): String = "AuthUpdateUserRequestDto(password=[REDACTED])"
}
