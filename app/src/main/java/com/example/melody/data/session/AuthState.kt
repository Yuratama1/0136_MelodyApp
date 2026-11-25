package com.example.melody.data.session

sealed interface AuthState {
    data object Unauthenticated : AuthState

    data class Authenticated(val session: AuthSession) : AuthState

    data class Error(val message: String) : AuthState
}
