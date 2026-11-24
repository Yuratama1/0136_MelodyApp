package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthRecoveryRequestDto(
    val email: String,
)
