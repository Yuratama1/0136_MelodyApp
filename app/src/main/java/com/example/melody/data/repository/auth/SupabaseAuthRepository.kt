package com.example.melody.data.repository.auth

import com.example.melody.data.remote.auth.SupabaseAuthApi
import com.example.melody.data.remote.auth.dto.AuthApiErrorDto
import com.example.melody.data.remote.auth.dto.AuthPasswordSignInRequestDto
import com.example.melody.data.remote.auth.dto.AuthSignUpRequestDto
import com.example.melody.data.remote.auth.dto.AuthVerifyEmailRequestDto
import com.example.melody.data.session.AuthSession
import com.example.melody.data.session.AuthSessionManager
import com.example.melody.data.session.AuthSessionMapper
import com.example.melody.data.session.EpochClock
import com.example.melody.data.session.SessionPersistenceException
import com.example.melody.data.session.SessionResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

class SupabaseAuthRepository @Inject constructor(
    private val authApiProvider: Provider<SupabaseAuthApi>,
    private val json: Json,
    private val sessionManagerProvider: Provider<AuthSessionManager>,
    private val epochClock: EpochClock,
) : AuthRepository {
    override suspend fun login(email: String, password: String): AuthSession {
        try {
            val response = authApiProvider.get().signInWithPassword(
                AuthPasswordSignInRequestDto(email = email, password = password),
            )
            val session = AuthSessionMapper.fromLogin(response, epochClock.nowSeconds())
            sessionManagerProvider.get().persistLoginSession(session)
            return session
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: SessionResponseException) {
            throw AuthRepositoryException(exception.message ?: LOGIN_RESPONSE_INCOMPLETE)
        } catch (exception: SessionPersistenceException) {
            throw AuthRepositoryException(SESSION_SAVE_ERROR)
        } catch (exception: HttpException) {
            throw mapLoginHttpException(exception)
        } catch (exception: IOException) {
            throw AuthRepositoryException(CONNECTION_ERROR)
        } catch (_: Exception) {
            throw AuthRepositoryException(LOGIN_ERROR)
        }
    }

    override suspend fun register(displayName: String, email: String, password: String) {
        try {
            authApiProvider.get().signUp(
                AuthSignUpRequestDto(
                    email = email,
                    password = password,
                    data = buildJsonObject { put(DISPLAY_NAME_KEY, displayName) },
                ),
            )
            // Signup may return a user without a session while email confirmation is enabled.
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            throw mapException(exception, isVerification = false)
        }
    }

    override suspend fun verifyEmail(email: String, token: String) {
        try {
            authApiProvider.get().verifyEmail(
                AuthVerifyEmailRequestDto(
                    email = email,
                    token = token,
                    type = SIGNUP_VERIFICATION_TYPE,
                ),
            )
            // The returned token pair is intentionally discarded; T-07 does not establish a session.
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            throw mapException(exception, isVerification = true)
        }
    }

    private fun mapLoginHttpException(exception: HttpException): AuthRepositoryException {
        val detail = exception.response()?.errorBody()?.use { it.string() }
            ?.let { runCatching { json.decodeFromString<AuthApiErrorDto>(it) }.getOrNull() }
            ?.let { listOfNotNull(it.message, it.msg, it.error_description, it.error).joinToString(" ").lowercase() }
            .orEmpty()

        val message = when {
            listOf("email not confirmed", "email not verified", "confirm your email", "not confirmed")
                .any { it in detail } -> EMAIL_NOT_CONFIRMED
            listOf("invalid login credentials", "invalid_credentials", "invalid password")
                .any { it in detail } -> INVALID_CREDENTIALS
            else -> LOGIN_ERROR
        }
        return AuthRepositoryException(message)
    }

    private fun mapException(exception: Exception, isVerification: Boolean): AuthRepositoryException {
        if (exception is IOException) {
            return AuthRepositoryException(CONNECTION_ERROR)
        }

        val body = (exception as? HttpException)?.response()?.errorBody()?.use { it.string() }
        val authError = body?.let { runCatching { json.decodeFromString<AuthApiErrorDto>(it) }.getOrNull() }
        val detail = listOfNotNull(
            authError?.message,
            authError?.msg,
            authError?.error_description,
            authError?.error,
        ).firstOrNull()?.lowercase().orEmpty()

        val message = when {
            !isVerification && ("already registered" in detail || "already been registered" in detail) -> EMAIL_ALREADY_REGISTERED
            isVerification && ("invalid" in detail || "expired" in detail || "not found" in detail) -> INVALID_VERIFICATION_CODE
            isVerification -> VERIFICATION_ERROR
            else -> REGISTRATION_ERROR
        }
        return AuthRepositoryException(message)
    }

    private companion object {
        const val DISPLAY_NAME_KEY = "display_name"
        const val SIGNUP_VERIFICATION_TYPE = "signup"
        const val CONNECTION_ERROR = "Tidak dapat terhubung. Periksa koneksi internet dan coba lagi."
        const val EMAIL_ALREADY_REGISTERED = "Email sudah terdaftar. Silakan gunakan email lain."
        const val INVALID_VERIFICATION_CODE = "Kode verifikasi salah atau sudah kedaluwarsa."
        const val REGISTRATION_ERROR = "Registrasi gagal. Periksa kembali data Anda dan coba lagi."
        const val VERIFICATION_ERROR = "Verifikasi gagal. Periksa kode dan email Anda, lalu coba lagi."
        const val EMAIL_NOT_CONFIRMED = "Email belum diverifikasi. Verifikasi email sebelum masuk."
        const val INVALID_CREDENTIALS = "Email atau password salah. Periksa kembali lalu coba lagi."
        const val LOGIN_RESPONSE_INCOMPLETE = "Respons login tidak lengkap. Silakan coba lagi."
        const val SESSION_SAVE_ERROR = "Login berhasil, tetapi sesi tidak dapat disimpan dengan aman. Coba lagi."
        const val LOGIN_ERROR = "Login gagal. Periksa data Anda dan coba lagi."
    }
}
