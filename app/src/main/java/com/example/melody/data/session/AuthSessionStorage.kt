package com.example.melody.data.session

interface AuthSessionStorage {
    suspend fun read(): AuthSession?

    suspend fun write(session: AuthSession)

    suspend fun clear()
}
