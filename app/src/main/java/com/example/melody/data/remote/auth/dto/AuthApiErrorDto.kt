package com.example.melody.data.remote.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthApiErrorDto(
    val message: String? = null,
    val msg: String? = null,
    val error: String? = null,
    val error_description: String? = null,
    val code: String? = null,
    val error_code: String? = null,
)
