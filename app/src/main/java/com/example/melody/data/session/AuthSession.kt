package com.example.melody.data.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("user_id") val userId: String,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("expires_at_epoch_seconds") val expiresAtEpochSeconds: Long,
) {
    override fun toString(): String =
        "AuthSession(userId=$userId, email=$email, expiresAtEpochSeconds=$expiresAtEpochSeconds)"
}
