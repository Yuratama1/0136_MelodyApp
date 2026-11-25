package com.example.melody.data.repository.auth

import com.example.melody.data.remote.auth.SupabaseAuthApi
import com.example.melody.data.session.AuthSession
import com.example.melody.data.session.AuthSessionMapper
import com.example.melody.data.session.AuthSessionManager
import com.example.melody.data.session.AuthState
import com.example.melody.data.session.EpochClock
import javax.inject.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SupabaseAuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: SupabaseAuthRepository
    private lateinit var sessionManager: TestAuthSessionManager
    private var sessionManagerProviderAccesses = 0
    private var serverIsRunning = false

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        serverIsRunning = true
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SupabaseAuthApi::class.java)
        sessionManager = TestAuthSessionManager()
        sessionManagerProviderAccesses = 0
        repository = SupabaseAuthRepository(
            authApiProvider = Provider { api },
            json = testJson,
            sessionManagerProvider = Provider { sessionManagerProviderAccesses++; sessionManager },
            epochClock = object : EpochClock { override fun nowSeconds() = TEST_NOW },
        )
    }

    @After
    fun tearDown() {
        if (serverIsRunning) server.shutdown()
    }

    @Test
    fun registerMapsDisplayNameToMetadataAndAcceptsResponseWithoutSession() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"user-1","email":"melody@example.com"}"""))

        repository.register("Melody User", "melody@example.com", "password1")
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/signup", request.path)
        assertEquals(
            """{"email":"melody@example.com","password":"password1","data":{"display_name":"Melody User"}}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun loginUsesPasswordGrantAndPersistsOnlyConfirmedCompleteSession() = runBlocking {
        server.enqueue(MockResponse().setBody(VERIFIED_LOGIN_JSON))

        val session = repository.login("melody@example.com", "test-password")
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/token?grant_type=password", request.path)
        assertEquals(
            """{"email":"melody@example.com","password":"test-password"}""",
            request.body.readUtf8(),
        )
        assertEquals("user-1", session.userId)
        assertEquals("Melody", session.displayName)
        assertEquals(session, sessionManager.persistedSession)
    }

    @Test
    fun loginRejectsUnverifiedAccountWithoutPersistingSession() = runBlocking {
        server.enqueue(MockResponse().setBody(UNVERIFIED_LOGIN_JSON))

        val exception = runCatching { repository.login("melody@example.com", "test-password") }.exceptionOrNull()

        assertEquals(AuthSessionMapper.EMAIL_NOT_CONFIRMED, exception?.message)
        assertEquals(null, sessionManager.persistedSession)
    }

    @Test
    fun loginRejectsIncompleteSessionWithoutPersisting() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"access_token":"access-test","refresh_token":"refresh-test"}"""))

        val exception = runCatching { repository.login("melody@example.com", "test-password") }.exceptionOrNull()

        assertEquals(AuthSessionMapper.INCOMPLETE_RESPONSE, exception?.message)
        assertEquals(null, sessionManager.persistedSession)
    }

    @Test
    fun loginMapsInvalidCredentialsAndUnconfirmedEmailErrors() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"msg":"Invalid login credentials"}"""))
        val invalidCredentials = runCatching { repository.login("melody@example.com", "wrong-password") }.exceptionOrNull()
        assertEquals("Email atau password salah. Periksa kembali lalu coba lagi.", invalidCredentials?.message)

        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"msg":"Email not confirmed"}"""))
        val unconfirmed = runCatching { repository.login("melody@example.com", "test-password") }.exceptionOrNull()
        assertEquals("Email belum diverifikasi. Verifikasi email sebelum masuk.", unconfirmed?.message)
        assertEquals(null, sessionManager.persistedSession)
    }

    @Test
    fun verificationUsesSignupTypeAndDiscardsSessionResponse() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"access_token":"test-access","refresh_token":"test-refresh","token_type":"bearer","expires_in":3600,"user":{"id":"user-1"}}""",
            ),
        )

        repository.verifyEmail("melody@example.com", "test-verification-code")
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/verify", request.path)
        assertEquals(
            """{"email":"melody@example.com","token":"test-verification-code","type":"signup"}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun verificationFailureReturnsSafeUserFacingMessage() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"code":"otp_expired","message":"Token has expired"}"""),
        )

        val exception = runCatching {
            repository.verifyEmail("melody@example.com", "expired-test-code")
        }.exceptionOrNull()

        assertTrue(exception is AuthRepositoryException)
        assertEquals(
            "Kode verifikasi salah atau sudah kedaluwarsa.",
            exception?.message,
        )
    }

    @Test
    fun registrationFailureMapsAlreadyRegisteredEmail() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"msg":"User already registered"}"""),
        )

        val exception = runCatching {
            repository.register("Melody", "melody@example.com", "password1")
        }.exceptionOrNull()

        assertTrue(exception is AuthRepositoryException)
        assertEquals("Email sudah terdaftar. Silakan gunakan email lain.", exception?.message)
    }

    @Test
    fun networkFailureReturnsSafeConnectionMessage() = runBlocking {
        server.shutdown()
        serverIsRunning = false

        val exception = runCatching {
            repository.register("Melody", "melody@example.com", "password1")
        }.exceptionOrNull()

        assertTrue(exception is AuthRepositoryException)
        assertEquals(
            "Tidak dapat terhubung. Periksa koneksi internet dan coba lagi.",
            exception?.message,
        )
    }

    @Test
    fun recoveryRequestPostsOnlyEmailAndReturnsEnumerationSafeSuccess() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        repository.requestPasswordRecovery("melody@example.com")
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/recover", request.path)
        assertEquals("{\"email\":\"melody@example.com\"}", request.body.readUtf8())
        assertEquals(0, sessionManagerProviderAccesses)
        assertNull(sessionManager.persistedSession)
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun recoveryRequestRateLimitReturnsSafeMessage() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"message":"Too many requests"}"""))

        val exception = runCatching { repository.requestPasswordRecovery("melody@example.com") }.exceptionOrNull()

        assertEquals("Terlalu banyak permintaan. Tunggu beberapa saat sebelum mencoba lagi.", exception?.message)
        assertFalse(exception?.message.orEmpty().contains("melody@example.com"))
    }

    @Test
    fun recoveryRequestFailureDoesNotRevealWhetherEmailIsRegistered() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"User not found"}"""))

        val exception = runCatching { repository.requestPasswordRecovery("unknown@example.com") }.exceptionOrNull()

        assertEquals(
            "Permintaan pemulihan belum dapat diproses. Jika alamat email dapat menerima pemulihan, instruksi akan dikirim. Coba lagi nanti.",
            exception?.message,
        )
        assertFalse(exception?.message.orEmpty().contains("terdaftar", ignoreCase = true))
        assertFalse(exception?.message.orEmpty().contains("unknown@example.com"))
    }

    @Test
    fun recoveryVerifiesOtpAsRecoveryAndUpdatesPasswordWithTemporaryBearerOnly() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"access_token":"temporary-recovery-access"}"""))
        server.enqueue(MockResponse().setBody("""{"id":"user-1","email":"melody@example.com"}"""))

        repository.resetPassword("melody@example.com", "123456", "new-password-123")
        val verify = server.takeRequest()
        val update = server.takeRequest()

        assertEquals("/auth/v1/verify", verify.path)
        assertEquals(
            "{\"email\":\"melody@example.com\",\"token\":\"123456\",\"type\":\"recovery\"}",
            verify.body.readUtf8(),
        )
        assertEquals("PUT", update.method)
        assertEquals("/auth/v1/user", update.path)
        assertEquals("Bearer temporary-recovery-access", update.getHeader("Authorization"))
        val updateBody = update.body.readUtf8()
        assertEquals("{\"password\":\"new-password-123\"}", updateBody)
        assertFalse(updateBody.contains("confirm"))
        assertEquals(0, sessionManagerProviderAccesses)
        assertEquals(0, sessionManager.persistCalls)
        assertNull(sessionManager.persistedSession)
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun recoveryResponseWithoutAccessTokenDoesNotAttemptPasswordUpdate() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"refresh_token":"not-used"}"""))

        val exception = runCatching {
            repository.resetPassword("melody@example.com", "123456", "new-password-123")
        }.exceptionOrNull()

        assertEquals("Sesi pemulihan tidak valid. Minta email pemulihan baru.", exception?.message)
        assertEquals(1, server.requestCount)
        assertEquals(0, sessionManagerProviderAccesses)
        assertNull(sessionManager.persistedSession)
    }

    @Test
    fun expiredRecoveryOtpMapsToSafeMessageAndDoesNotUpdatePassword() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"code":"otp_expired","message":"Token has expired"}"""),
        )

        val exception = runCatching {
            repository.resetPassword("melody@example.com", "expired-otp", "new-password-123")
        }.exceptionOrNull()

        assertEquals(
            "Kode OTP salah atau sudah kedaluwarsa. Minta kode pemulihan baru jika diperlukan.",
            exception?.message,
        )
        assertEquals(1, server.requestCount)
        assertFalse(exception?.message.orEmpty().contains("expired-otp"))
    }

    @Test
    fun invalidRecoveryOtpMapsToSafeMessageAndDoesNotAttemptPasswordUpdate() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"code":"otp_not_found","message":"Invalid token"}"""),
        )

        val exception = runCatching {
            repository.resetPassword("melody@example.com", "invalid-otp", "new-password-123")
        }.exceptionOrNull()

        assertEquals(
            "Kode OTP salah atau sudah kedaluwarsa. Minta kode pemulihan baru jika diperlukan.",
            exception?.message,
        )
        assertEquals(1, server.requestCount)
        assertFalse(exception?.message.orEmpty().contains("invalid-otp"))
    }

    @Test
    fun passwordUpdateFailureDoesNotExposeTemporaryTokenOrPassword() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"access_token":"temporary-recovery-access"}"""))
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Permission denied"}"""))

        val exception = runCatching {
            repository.resetPassword("melody@example.com", "123456", "new-password-123")
        }.exceptionOrNull()

        assertEquals("Password baru tidak dapat diperbarui. Coba lagi.", exception?.message)
        assertFalse(exception?.message.orEmpty().contains("temporary-recovery-access"))
        assertFalse(exception?.message.orEmpty().contains("new-password-123"))
        assertEquals(0, sessionManagerProviderAccesses)
        assertEquals(0, sessionManager.persistCalls)
    }

    @Test
    fun recoveryRequestNetworkFailureReturnsSafeConnectionMessage() = runBlocking {
        server.shutdown()
        serverIsRunning = false

        val exception = runCatching { repository.requestPasswordRecovery("melody@example.com") }.exceptionOrNull()

        assertEquals(
            "Tidak dapat terhubung. Periksa koneksi internet dan coba lagi.",
            exception?.message,
        )
    }

    @Test
    fun passwordUpdateNetworkFailureReturnsSafeMessageAndDoesNotPersistRecoveryToken() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"access_token":"temporary-recovery-access"}"""))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))

        val exception = runCatching {
            repository.resetPassword("melody@example.com", "123456", "new-password-123")
        }.exceptionOrNull()

        assertEquals(
            "Tidak dapat terhubung. Periksa koneksi internet dan coba lagi.",
            exception?.message,
        )
        assertFalse(exception?.message.orEmpty().contains("temporary-recovery-access"))
        assertEquals(0, sessionManagerProviderAccesses)
        assertEquals(0, sessionManager.persistCalls)
        assertNull(sessionManager.persistedSession)
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
    }

    private companion object {
        val testJson = Json { ignoreUnknownKeys = true }
        const val TEST_NOW = 1_800_000_000L
        const val VERIFIED_LOGIN_JSON = """{"access_token":"access-test","refresh_token":"refresh-test","token_type":"bearer","expires_in":3600,"user":{"id":"user-1","email":"melody@example.com","email_confirmed_at":"2026-01-01T00:00:00Z","user_metadata":{"display_name":"Melody"}}}"""
        const val UNVERIFIED_LOGIN_JSON = """{"access_token":"access-test","refresh_token":"refresh-test","token_type":"bearer","expires_in":3600,"user":{"id":"user-1","email":"melody@example.com"}}"""
    }
}

private class TestAuthSessionManager : AuthSessionManager {
    private val state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = state
    var persistedSession: AuthSession? = null
    var persistCalls: Int = 0

    override suspend fun restoreSession() = Unit

    override suspend fun persistLoginSession(session: AuthSession) {
        persistCalls++
        persistedSession = session
        state.value = AuthState.Authenticated(session)
    }

    override suspend fun getValidAccessToken(): String? = persistedSession?.accessToken
}
