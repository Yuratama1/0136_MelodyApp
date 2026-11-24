package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AuthVerifyEmailRequestDto(
    val email: String,
    val token: String,
    val type: String,
    @SerialName("redirect_to")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val redirectTo: String? = null,
)
