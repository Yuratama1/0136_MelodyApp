package com.example.melody.data.session

import com.example.melody.data.remote.auth.SupabaseAuthApi
import com.example.melody.data.remote.auth.dto.AuthRefreshTokenRequestDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class AuthSessionManagerImpl @Inject constructor(
    private val authApiProvider: Provider<SupabaseAuthApi>,
    private val storage: AuthSessionStorage,
    private val epochClock: EpochClock,
) : AuthSessionManager {
    private val mutex = Mutex()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private var currentSession: AuthSession? = null

    override suspend fun restoreSession() = mutex.withLock {
        val session = try {
            currentSession ?: storage.read()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: SessionDataCorruptionException) {
            clearCorruptedSession()
            return@withLock
        } catch (_: Exception) {
            _authState.value = AuthState.Error(SESSION_RESTORE_ERROR)
            return@withLock
        }

        if (session == null) {
            currentSession = null
            _authState.value = AuthState.Unauthenticated
            return@withLock
        }

        currentSession = session
        if (isAccessTokenValid(session)) {
            _authState.value = AuthState.Authenticated(session)
        } else {
            refreshLocked(session)
        }
    }

    override suspend fun persistLoginSession(session: AuthSession) = mutex.withLock {
        try {
            storage.write(session)
            currentSession = session
            _authState.value = AuthState.Authenticated(session)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            throw SessionPersistenceException(exception)
        }
    }

    override suspend fun getValidAccessToken(): String? = mutex.withLock {
        val session = try {
            currentSession ?: storage.read()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: SessionDataCorruptionException) {
            clearCorruptedSession()
            return@withLock null
        } catch (_: Exception) {
            _authState.value = AuthState.Error(SESSION_RESTORE_ERROR)
            return@withLock null
        }

        if (session == null) {
            _authState.value = AuthState.Unauthenticated
            return@withLock null
        }

        currentSession = session
        if (isAccessTokenValid(session)) {
            _authState.value = AuthState.Authenticated(session)
            session.accessToken
        } else {
            refreshLocked(session)
            (_authState.value as? AuthState.Authenticated)?.session?.accessToken
        }
    }

    private fun isAccessTokenValid(session: AuthSession): Boolean =
        session.expiresAtEpochSeconds > epochClock.nowSeconds() + REFRESH_SKEW_SECONDS

    private suspend fun refreshLocked(previous: AuthSession) {
        try {
            val response = authApiProvider.get().refreshSession(
                AuthRefreshTokenRequestDto(previous.refreshToken),
            )
            val refreshed = AuthSessionMapper.afterRefresh(response, previous, epochClock.nowSeconds())
            storage.write(refreshed)
            currentSession = refreshed
            _authState.value = AuthState.Authenticated(refreshed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: HttpException) {
            if (exception.code() == HTTP_BAD_REQUEST || exception.code() == HTTP_UNAUTHORIZED) {
                clearRejectedSession()
            } else {
                _authState.value = AuthState.Error(SESSION_REFRESH_ERROR)
            }
        } catch (_: IOException) {
            _authState.value = AuthState.Error(SESSION_NETWORK_ERROR)
        } catch (_: Exception) {
            _authState.value = AuthState.Error(SESSION_REFRESH_ERROR)
        }
    }

    private suspend fun clearCorruptedSession() {
        currentSession = null
        runCatching { storage.clear() }
        _authState.value = AuthState.Unauthenticated
    }

    private suspend fun clearRejectedSession() {
        try {
            storage.clear()
            currentSession = null
            _authState.value = AuthState.Unauthenticated
        } catch (_: Exception) {
            _authState.value = AuthState.Error(SESSION_CLEAR_ERROR)
        }
    }

    private companion object {
        const val REFRESH_SKEW_SECONDS = 60L
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val SESSION_RESTORE_ERROR = "Sesi tersimpan tidak dapat dibaca. Coba lagi."
        const val SESSION_REFRESH_ERROR = "Sesi tidak dapat diperbarui sekarang. Coba lagi nanti."
        const val SESSION_NETWORK_ERROR = "Tidak dapat terhubung untuk memperbarui sesi. Periksa koneksi dan coba lagi."
        const val SESSION_CLEAR_ERROR = "Sesi tidak dapat dibersihkan dengan aman."
    }
}

class SessionPersistenceException(cause: Throwable) : Exception("Could not safely persist the session.", cause)
