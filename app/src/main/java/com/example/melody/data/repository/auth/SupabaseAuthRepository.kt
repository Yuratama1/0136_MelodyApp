package com.example.melody.data.repository.auth

import com.example.melody.data.remote.auth.SupabaseAuthApi
import com.example.melody.data.remote.auth.dto.AuthApiErrorDto
import com.example.melody.data.remote.auth.dto.AuthPasswordSignInRequestDto
import com.example.melody.data.remote.auth.dto.AuthRecoveryRequestDto
import com.example.melody.data.remote.auth.dto.AuthSessionDto
import com.example.melody.data.remote.auth.dto.AuthSignUpRequestDto
import com.example.melody.data.remote.auth.dto.AuthUpdateUserRequestDto
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

    override suspend fun requestPasswordRecovery(email: String) {
        try {
            val response = authApiProvider.get().requestPasswordRecovery(AuthRecoveryRequestDto(email))
            if (!response.isSuccessful) {
                val error = response.errorBody()?.use { body ->
                    runCatching { json.decodeFromString<AuthApiErrorDto>(body.string()) }.getOrNull()
                }
                throw AuthRepositoryException(recoveryRequestError(response.code(), error))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: AuthRepositoryException) {
            throw exception
        } catch (_: IOException) {
            throw AuthRepositoryException(CONNECTION_ERROR)
        } catch (_: Exception) {
            throw AuthRepositoryException(RECOVERY_REQUEST_ERROR)
        }
    }

    override suspend fun resetPassword(email: String, otp: String, newPassword: String) {
        var temporaryAccessToken: String? = null
        try {
            temporaryAccessToken = verifyRecoveryToken(email, otp)
            try {
                authApiProvider.get().updateUserPassword(
                    authorization = "Bearer ${requireNotNull(temporaryAccessToken)}",
                    request = AuthUpdateUserRequestDto(password = newPassword),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IOException) {
                throw AuthRepositoryException(CONNECTION_ERROR)
            } catch (exception: HttpException) {
                throw AuthRepositoryException(passwordUpdateError(exception.code()))
            } catch (exception: AuthRepositoryException) {
                throw exception
            } catch (exception: Exception) {
                throw AuthRepositoryException(
                    if (exception.hasIoCause()) CONNECTION_ERROR else PASSWORD_UPDATE_ERROR,
                )
            }
        } finally {
            temporaryAccessToken = null
        }
    }

    private suspend fun verifyRecoveryToken(email: String, otp: String): String {
        val response: AuthSessionDto = try {
            authApiProvider.get().verifyEmail(
                AuthVerifyEmailRequestDto(email = email, token = otp, type = RECOVERY_VERIFICATION_TYPE),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            throw AuthRepositoryException(CONNECTION_ERROR)
        } catch (exception: HttpException) {
            throw AuthRepositoryException(recoveryVerificationError(exception))
        } catch (exception: Exception) {
            throw AuthRepositoryException(
                if (exception.hasIoCause()) CONNECTION_ERROR else RECOVERY_VERIFICATION_ERROR,
            )
        }

        return response.accessToken?.takeIf(String::isNotBlank)
            ?: throw AuthRepositoryException(RECOVERY_SESSION_INVALID)
    }

    private fun Exception.hasIoCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException) return true
            current = current.cause
        }
        return false
    }

    private fun recoveryRequestError(statusCode: Int, error: AuthApiErrorDto?): String =
        if (isRateLimit(statusCode, error)) RATE_LIMITED else RECOVERY_REQUEST_ERROR

    private fun recoveryVerificationError(exception: HttpException): String {
        val error = readAuthError(exception)
        return when {
            isRateLimit(exception.code(), error) -> RATE_LIMITED
            exception.code() in INVALID_CREDENTIAL_STATUS_CODES || isExpiredOtp(error) -> INVALID_RECOVERY_CODE
            else -> RECOVERY_VERIFICATION_ERROR
        }
    }

    private fun readAuthError(exception: HttpException): AuthApiErrorDto? =
        exception.response()?.errorBody()?.use { body ->
            runCatching { json.decodeFromString<AuthApiErrorDto>(body.string()) }.getOrNull()
        }

    private fun isRateLimit(statusCode: Int, error: AuthApiErrorDto?): Boolean {
        val detail = listOfNotNull(error?.message, error?.msg, error?.error, error?.error_description, error?.code, error?.error_code)
            .joinToString(" ").lowercase()
        return statusCode == HTTP_TOO_MANY_REQUESTS ||
            listOf("rate limit", "rate_limit", "too many", "over_email_send_rate_limit").any { it in detail }
    }

    private fun isExpiredOtp(error: AuthApiErrorDto?): Boolean {
        val detail = listOfNotNull(error?.message, error?.msg, error?.error, error?.error_description, error?.code, error?.error_code)
            .joinToString(" ").lowercase()
        return listOf("expired", "otp_expired", "invalid otp", "invalid token", "token has expired").any { it in detail }
    }

    private fun passwordUpdateError(statusCode: Int): String =
        if (statusCode == HTTP_TOO_MANY_REQUESTS) RATE_LIMITED else PASSWORD_UPDATE_ERROR

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
        const val RECOVERY_VERIFICATION_TYPE = "recovery"
        const val HTTP_TOO_MANY_REQUESTS = 429
        val INVALID_CREDENTIAL_STATUS_CODES = setOf(400, 422)
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
        const val RECOVERY_REQUEST_ERROR = "Permintaan pemulihan belum dapat diproses. Jika alamat email dapat menerima pemulihan, instruksi akan dikirim. Coba lagi nanti."
        const val RECOVERY_VERIFICATION_ERROR = "Kode pemulihan tidak dapat diverifikasi. Periksa kode dan coba lagi."
        const val RECOVERY_SESSION_INVALID = "Sesi pemulihan tidak valid. Minta email pemulihan baru."
        const val INVALID_RECOVERY_CODE = "Kode OTP salah atau sudah kedaluwarsa. Minta kode pemulihan baru jika diperlukan."
        const val PASSWORD_UPDATE_ERROR = "Password baru tidak dapat diperbarui. Coba lagi."
        const val RATE_LIMITED = "Terlalu banyak permintaan. Tunggu beberapa saat sebelum mencoba lagi."
    }
}
