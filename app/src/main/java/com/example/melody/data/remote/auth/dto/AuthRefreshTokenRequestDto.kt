package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRefreshTokenRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)
