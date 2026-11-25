package com.example.melody.data.repository.auth

import com.example.melody.data.session.AuthSession

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthSession

    suspend fun register(displayName: String, email: String, password: String)

    suspend fun verifyEmail(email: String, token: String)

    suspend fun requestPasswordRecovery(email: String)

    suspend fun resetPassword(email: String, otp: String, newPassword: String)
}
