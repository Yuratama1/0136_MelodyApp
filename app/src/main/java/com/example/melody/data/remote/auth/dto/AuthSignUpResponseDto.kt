package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Supports both token responses and the top-level user response returned when confirmation is enabled. */
@Serializable
data class AuthSignUpResponseDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: AuthUserDto? = null,
    val id: String? = null,
    val email: String? = null,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_metadata") val userMetadata: JsonObject? = null,
    @SerialName("app_metadata") val appMetadata: JsonObject? = null,
) {
    /** Returns the session representation when this signup response includes one. */
    fun asSessionOrNull(): AuthSessionDto? = if (
        accessToken == null && refreshToken == null && tokenType == null && expiresIn == null && user == null
    ) {
        null
    } else {
        AuthSessionDto(accessToken, refreshToken, tokenType, expiresIn, user)
    }

    /** Returns the created user from either response shape. */
    fun asUserOrNull(): AuthUserDto? = user ?: if (id == null && email == null) {
        null
    } else {
        AuthUserDto(id, email, emailConfirmedAt, confirmedAt, createdAt, updatedAt, userMetadata, appMetadata)
    }
}
