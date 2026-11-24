package com.example.melody.data.repository.auth

import com.example.melody.data.remote.auth.SupabaseAuthApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SupabaseAuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: SupabaseAuthRepository
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
        repository = SupabaseAuthRepository(api, testJson)
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

    private companion object {
        val testJson = Json { ignoreUnknownKeys = true }
    }
}
