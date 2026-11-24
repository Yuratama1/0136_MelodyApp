package com.example.melody.data.repository.auth

interface AuthRepository {
    suspend fun register(displayName: String, email: String, password: String)

    suspend fun verifyEmail(email: String, token: String)
}
