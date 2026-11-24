package com.example.melody.data.repository.auth

import com.example.melody.data.remote.auth.SupabaseAuthApi
import com.example.melody.data.remote.auth.dto.AuthApiErrorDto
import com.example.melody.data.remote.auth.dto.AuthSignUpRequestDto
import com.example.melody.data.remote.auth.dto.AuthVerifyEmailRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class SupabaseAuthRepository @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val json: Json,
) : AuthRepository {
    override suspend fun register(displayName: String, email: String, password: String) {
        try {
            authApi.signUp(
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
            authApi.verifyEmail(
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
    }
}
