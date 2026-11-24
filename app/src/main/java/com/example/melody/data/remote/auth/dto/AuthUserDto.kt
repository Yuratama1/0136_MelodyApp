package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthUserDto(
    val id: String? = null,
    val email: String? = null,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_metadata") val userMetadata: JsonObject? = null,
    @SerialName("app_metadata") val appMetadata: JsonObject? = null,
)
