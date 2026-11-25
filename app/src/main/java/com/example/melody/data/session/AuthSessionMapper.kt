package com.example.melody.data.session

import com.example.melody.data.remote.auth.dto.AuthSessionDto
import com.example.melody.data.remote.auth.dto.AuthUserDto
import kotlinx.serialization.json.JsonPrimitive

object AuthSessionMapper {
    fun fromLogin(response: AuthSessionDto, nowEpochSeconds: Long): AuthSession {
        val user = response.user ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        if (user.emailConfirmedAt.isNullOrBlank() && user.confirmedAt.isNullOrBlank()) {
            throw SessionResponseException(EMAIL_NOT_CONFIRMED)
        }

        val accessToken = response.accessToken.requiredOrNull() ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val refreshToken = response.refreshToken.requiredOrNull() ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val tokenType = response.tokenType.requiredOrNull() ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val userId = user.id.requiredOrNull() ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val email = user.email.requiredOrNull() ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val expiresIn = response.expiresIn?.takeIf { it > 0 } ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val expiresAt = runCatching { Math.addExact(nowEpochSeconds, expiresIn) }
            .getOrElse { throw SessionResponseException(INCOMPLETE_RESPONSE) }

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            userId = userId,
            email = email,
            displayName = user.displayNameOrNull(),
            expiresAtEpochSeconds = expiresAt,
        )
    }

    fun afterRefresh(response: AuthSessionDto, previous: AuthSession, nowEpochSeconds: Long): AuthSession {
        val accessToken = response.accessToken.requiredOrNull() ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val expiresIn = response.expiresIn?.takeIf { it > 0 } ?: throw SessionResponseException(INCOMPLETE_RESPONSE)
        val expiresAt = runCatching { Math.addExact(nowEpochSeconds, expiresIn) }
            .getOrElse { throw SessionResponseException(INCOMPLETE_RESPONSE) }
        val user = response.user?.mergeWith(previous)

        return AuthSession(
            accessToken = accessToken,
            refreshToken = response.refreshToken.requiredOrNull() ?: previous.refreshToken,
            tokenType = response.tokenType.requiredOrNull() ?: previous.tokenType,
            userId = user?.id.requiredOrNull() ?: previous.userId,
            email = user?.email.requiredOrNull() ?: previous.email,
            displayName = user?.displayNameOrNull() ?: previous.displayName,
            expiresAtEpochSeconds = expiresAt,
        )
    }

    private fun AuthUserDto.displayNameOrNull(): String? =
        (userMetadata?.get(DISPLAY_NAME_KEY) as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

    private fun AuthUserDto.mergeWith(previous: AuthSession): AuthUserDto = copy(
        id = id.requiredOrNull() ?: previous.userId,
        email = email.requiredOrNull() ?: previous.email,
        userMetadata = userMetadata ?: previous.displayName?.let { displayName ->
            kotlinx.serialization.json.buildJsonObject { put(DISPLAY_NAME_KEY, JsonPrimitive(displayName)) }
        },
    )

    private fun String?.requiredOrNull(): String? = this?.takeIf { it.isNotBlank() }

    private const val DISPLAY_NAME_KEY = "display_name"
    const val INCOMPLETE_RESPONSE = "Respons login tidak lengkap. Silakan coba lagi."
    const val EMAIL_NOT_CONFIRMED = "Email belum diverifikasi. Verifikasi email sebelum masuk."
}

class SessionResponseException(message: String) : Exception(message)
