package com.example.melody.data.session

import com.example.melody.data.remote.auth.SupabaseAuthApi
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
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

class AuthSessionManagerTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: FakeAuthSessionStorage
    private lateinit var clock: FakeEpochClock
    private lateinit var manager: AuthSessionManagerImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        storage = FakeAuthSessionStorage()
        clock = FakeEpochClock(NOW)
        manager = newManager()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    @Test
    fun restoresStillValidSessionWithoutNetworkRefresh() = runBlocking {
        val session = session(expiresAt = NOW + 600)
        storage.savedSession = session

        manager.restoreSession()

        assertEquals(AuthState.Authenticated(session), manager.authState.value)
        assertEquals(0, server.requestCount)
        assertEquals(session.accessToken, manager.getValidAccessToken())
    }

    @Test
    fun refreshesExpiredSessionAndSavesNewTokensAtomically() = runBlocking {
        storage.savedSession = session(expiresAt = NOW + 30)
        server.enqueue(MockResponse().setBody(REFRESHED_SESSION_JSON))

        manager.restoreSession()
        val refreshed = (manager.authState.value as AuthState.Authenticated).session
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/token?grant_type=refresh_token", request.path)
        assertEquals("""{"refresh_token":"refresh-old"}""", request.body.readUtf8())
        assertEquals("access-new", refreshed.accessToken)
        assertEquals("refresh-new", refreshed.refreshToken)
        assertEquals("user-1", refreshed.userId)
        assertEquals("user@example.com", refreshed.email)
        assertEquals("Melody", refreshed.displayName)
        assertEquals(NOW + 3600, refreshed.expiresAtEpochSeconds)
        assertEquals(refreshed, storage.savedSession)
    }

    @Test
    fun invalidRefreshTokenClearsLocalSession() = runBlocking {
        storage.savedSession = session(expiresAt = NOW - 1)
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"invalid_grant","error_description":"Invalid refresh token"}"""),
        )

        manager.restoreSession()

        assertEquals(AuthState.Unauthenticated, manager.authState.value)
        assertNull(storage.savedSession)
        assertEquals(1, storage.clearCount)
    }

    @Test
    fun temporaryNetworkFailurePreservesStoredSessionForRetry() = runBlocking {
        val expired = session(expiresAt = NOW - 1)
        storage.savedSession = expired
        server.shutdown()

        manager.restoreSession()

        assertTrue(manager.authState.value is AuthState.Error)
        assertEquals(expired, storage.savedSession)
        assertEquals(0, storage.clearCount)
    }

    @Test
    fun concurrentAccessTokenRequestsShareOneRefresh() = runBlocking {
        storage.savedSession = session(expiresAt = NOW + 20)
        server.enqueue(
            MockResponse()
                .setBody(REFRESHED_SESSION_JSON)
                .setBodyDelay(250, TimeUnit.MILLISECONDS),
        )

        val accessTokens = (1..8).map {
            async(Dispatchers.Default) { manager.getValidAccessToken() }
        }.awaitAll()

        assertEquals(List(8) { "access-new" }, accessTokens)
        assertEquals(1, server.requestCount)
        assertEquals("access-new", (manager.authState.value as AuthState.Authenticated).session.accessToken)
    }

    @Test
    fun validAccessTokenIsAvailableForOtherRepositories() = runBlocking {
        val session = session(expiresAt = NOW + 600)
        storage.savedSession = session

        assertEquals("access-old", manager.getValidAccessToken())
        assertEquals(AuthState.Authenticated(session), manager.authState.value)
    }

    @Test
    fun incompleteRefreshKeepsOldLocalSessionAndPublishesError() = runBlocking {
        val expired = session(expiresAt = NOW - 1)
        storage.savedSession = expired
        server.enqueue(MockResponse().setBody("""{"access_token":"access-new"}"""))

        manager.restoreSession()

        assertTrue(manager.authState.value is AuthState.Error)
        assertEquals(expired, storage.savedSession)
        assertFalse(storage.clearCount > 0)
    }

    private fun newManager(): AuthSessionManagerImpl {
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SupabaseAuthApi::class.java)
        return AuthSessionManagerImpl(Provider { api }, storage, clock)
    }

    private fun session(expiresAt: Long) = AuthSession(
        accessToken = "access-old",
        refreshToken = "refresh-old",
        tokenType = "bearer",
        userId = "user-1",
        email = "user@example.com",
        displayName = "Melody",
        expiresAtEpochSeconds = expiresAt,
    )

    private class FakeAuthSessionStorage : AuthSessionStorage {
        var savedSession: AuthSession? = null
        var clearCount = 0

        override suspend fun read(): AuthSession? = savedSession
        override suspend fun write(session: AuthSession) { savedSession = session }
        override suspend fun clear() { savedSession = null; clearCount++ }
    }

    private class FakeEpochClock(private val currentSeconds: Long) : EpochClock {
        override fun nowSeconds(): Long = currentSeconds
    }

    private companion object {
        const val NOW = 1_800_000_000L
        val json = Json { ignoreUnknownKeys = true }
        const val REFRESHED_SESSION_JSON = """{"access_token":"access-new","refresh_token":"refresh-new","token_type":"bearer","expires_in":3600}"""
    }
}
