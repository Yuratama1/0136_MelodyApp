package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AuthSignUpRequestDto(
    val email: String,
    val password: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val data: JsonObject? = null,
)
