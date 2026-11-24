package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthPasswordSignInRequestDto(
    val email: String,
    val password: String,
)
