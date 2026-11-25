package com.example.melody.data.session

import kotlinx.coroutines.flow.StateFlow

interface AuthSessionManager {
    val authState: StateFlow<AuthState>

    suspend fun restoreSession()

    suspend fun persistLoginSession(session: AuthSession)

    suspend fun getValidAccessToken(): String?
}
