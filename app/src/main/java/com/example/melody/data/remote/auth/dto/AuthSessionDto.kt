package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Flexible because signup may return a user without tokens when email confirmation is required. */
@Serializable
data class AuthSessionDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: AuthUserDto? = null,
) {
    override fun toString(): String =
        "AuthSessionDto(tokenType=$tokenType, expiresIn=$expiresIn, userId=${user?.id})"
}
