package com.example.melody.data.remote.auth

import com.example.melody.core.config.SupabaseConfig
import com.example.melody.core.di.NetworkModule
import com.example.melody.data.remote.auth.dto.AuthApiErrorDto
import com.example.melody.data.remote.auth.dto.AuthRecoveryRequestDto
import com.example.melody.data.remote.auth.dto.AuthRefreshTokenRequestDto
import com.example.melody.data.remote.auth.dto.AuthSessionDto
import com.example.melody.data.remote.auth.dto.AuthSignUpRequestDto
import com.example.melody.data.remote.auth.dto.AuthSignUpResponseDto
import com.example.melody.data.remote.auth.dto.AuthPasswordSignInRequestDto
import com.example.melody.data.remote.auth.dto.AuthUpdateUserRequestDto
import com.example.melody.data.remote.auth.dto.AuthUserDto
import com.example.melody.data.remote.auth.dto.AuthVerifyEmailRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

class SupabaseAuthApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: SupabaseAuthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SupabaseAuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun signUpUsesSignupEndpointAndSerializesMetadata() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"user":{"id":"user-1"}}"""))
        val metadata = buildJsonObject { put("display_name", "Melody User") }

        val response = api.signUp(AuthSignUpRequestDto("test@example.com", "test-password", metadata))
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/signup", request.path)
        assertEquals("""{"email":"test@example.com","password":"test-password","data":{"display_name":"Melody User"}}""", request.body.readUtf8())
        assertEquals("user-1", response.user?.id)
        assertNull(response.accessToken)
    }

    @Test
    fun signUpAllowsUserWithoutSessionAndOmitsMissingOptionalMetadata() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"user-2","email":"test@example.com","extra":"ignored"}"""))

        val response = api.signUp(AuthSignUpRequestDto("test@example.com", "test-password"))

        assertEquals("/auth/v1/signup", server.takeRequest().path)
        assertEquals("user-2", response.asUserOrNull()?.id)
        assertNull(response.accessToken)
    }

    @Test
    fun signUpResponseSupportsTokenAndTopLevelUserShapes() {
        val userOnly = testJson.decodeFromString<AuthSignUpResponseDto>("""{"id":"user-7","email":"test@example.com","user_metadata":{"name":"Test"}}""")
        val withSession = testJson.decodeFromString<AuthSignUpResponseDto>("""{"access_token":"access-test","refresh_token":"refresh-test","token_type":"bearer","expires_in":3600,"user":{"id":"user-8"}}""")

        assertEquals("user-7", userOnly.asUserOrNull()?.id)
        assertNull(userOnly.asSessionOrNull())
        assertEquals("access-test", withSession.asSessionOrNull()?.accessToken)
        assertEquals("user-8", withSession.asUserOrNull()?.id)
    }

    @Test
    fun signInUsesPasswordGrantAndRequestBody() = runBlocking {
        server.enqueue(MockResponse().setBody(SESSION_JSON))

        val session = api.signInWithPassword(AuthPasswordSignInRequestDto("test@example.com", "test-password"))
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/token?grant_type=password", request.path)
        assertEquals("""{"email":"test@example.com","password":"test-password"}""", request.body.readUtf8())
        assertEquals("access-test", session.accessToken)
    }

    @Test
    fun refreshUsesRefreshTokenGrant() = runBlocking {
        server.enqueue(MockResponse().setBody(SESSION_JSON))

        api.refreshSession(AuthRefreshTokenRequestDto("refresh-test"))
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/token?grant_type=refresh_token", request.path)
        assertEquals("""{"refresh_token":"refresh-test"}""", request.body.readUtf8())
    }

    @Test
    fun verifyUsesVerifyEndpointAndOptionalRedirectBody() = runBlocking {
        server.enqueue(MockResponse().setBody(SESSION_JSON))

        api.verifyEmail(AuthVerifyEmailRequestDto("test@example.com", "verification-test", "signup", "melody://verified"))
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/v1/verify", request.path)
        assertEquals("""{"email":"test@example.com","token":"verification-test","type":"signup","redirect_to":"melody://verified"}""", request.body.readUtf8())
    }

    @Test
    fun recoveryWithoutRedirectSendsOnlyEmailInBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = api.requestPasswordRecovery(AuthRecoveryRequestDto("test@example.com"))
        val request = server.takeRequest()

        assertTrue(result.isSuccessful)
        assertEquals("POST", request.method)
        assertEquals("/auth/v1/recover", request.path)
        assertEquals("""{"email":"test@example.com"}""", request.body.readUtf8())
    }

    @Test
    fun recoverySendsRedirectAsEncodedQueryAndKeepsEmailInBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        val redirectTo = "melody://reset/path?source=email&name=Melody User"

        val result = api.requestPasswordRecovery(AuthRecoveryRequestDto("test@example.com"), redirectTo)
        val request = server.takeRequest()

        assertTrue(result.isSuccessful)
        assertEquals("POST", request.method)
        assertTrue(request.path.orEmpty().startsWith("/auth/v1/recover?redirect_to="))
        assertTrue(request.path.orEmpty().contains("%3A%2F%2F"))
        assertEquals(redirectTo, request.requestUrl?.queryParameter("redirect_to"))
        assertEquals("""{"email":"test@example.com"}""", request.body.readUtf8())
    }

    @Test
    fun updateUserUsesPutAndExplicitAuthorization() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"user-3","email":"test@example.com"}"""))

        val user = api.updateUserPassword("Bearer access-test", AuthUpdateUserRequestDto("new-test-password"))
        val request = server.takeRequest()

        assertEquals("PUT", request.method)
        assertEquals("/auth/v1/user", request.path)
        assertEquals("Bearer access-test", request.getHeader("Authorization"))
        assertEquals("""{"password":"new-test-password"}""", request.body.readUtf8())
        assertEquals("user-3", user.id)
    }

    @Test
    fun currentUserUsesGetAndExplicitAuthorization() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"user-4","email":"test@example.com"}"""))

        val user = api.getCurrentUser("Bearer access-test")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/auth/v1/user", request.path)
        assertEquals("Bearer access-test", request.getHeader("Authorization"))
        assertEquals("user-4", user.id)
    }

    @Test
    fun logoutDefaultsToLocalScopeAndUsesExplicitAuthorization() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = api.logout("Bearer access-test")
        val request = server.takeRequest()

        assertTrue(result.isSuccessful)
        assertEquals("POST", request.method)
        assertEquals("/auth/v1/logout?scope=local", request.path)
        assertEquals("Bearer access-test", request.getHeader("Authorization"))
        assertEquals("local", request.requestUrl?.queryParameter("scope"))
        assertFalse(request.path.orEmpty().contains("scope=global"))
    }

    @Test
    fun sessionAndUserDtosDecodeNullableFieldsMetadataAndUnknownProperties() {
        val session = testJson.decodeFromString<AuthSessionDto>("""{
            "access_token":"access-test","refresh_token":"refresh-test","token_type":"bearer","expires_in":3600,
            "user":{"id":"user-5","email":"test@example.com","email_confirmed_at":null,
            "confirmed_at":"2026-01-01T00:00:00Z","created_at":"2026-01-01T00:00:00Z",
            "updated_at":null,"user_metadata":{"display_name":"Melody"},"app_metadata":{"provider":"email"},"new_field":true},
            "new_session_field":"ignored"
        }""")
        val minimal = testJson.decodeFromString<AuthUserDto>("""{"id":"user-6","unrecognized":{"x":1}}""")

        assertEquals("access-test", session.accessToken)
        assertEquals("refresh-test", session.refreshToken)
        assertEquals("bearer", session.tokenType)
        assertEquals(3600L, session.expiresIn)
        assertEquals("Melody", session.user?.userMetadata?.get("display_name")?.toString()?.trim('"'))
        assertEquals("email", session.user?.appMetadata?.get("provider")?.toString()?.trim('"'))
        assertNull(session.user?.emailConfirmedAt)
        assertNull(session.user?.updatedAt)
        assertNull(minimal.email)
        assertNull(minimal.userMetadata)
    }

    @Test
    fun errorDtoDecodesAvailableFieldsAndIgnoresUnknownFields() {
        val error = testJson.decodeFromString<AuthApiErrorDto>("""{
            "message":"Invalid credentials","error":"invalid_grant","error_description":"Try again",
            "code":"400","error_code":"invalid_credentials","extra":"ignored"
        }""")

        assertEquals("Invalid credentials", error.message)
        assertEquals("invalid_grant", error.error)
        assertEquals("Try again", error.error_description)
        assertEquals("400", error.code)
        assertEquals("invalid_credentials", error.error_code)
        assertNull(error.msg)
    }

    @Test
    fun hiltProviderCreatesApiFromTheT05Retrofit() {
        val config = SupabaseConfig("https://melody-test.supabase.co", "sb_publishable_test-only-value")
        val interceptor = NetworkModule.provideSupabaseApiKeyInterceptor(config)
        val client = NetworkModule.provideOkHttpClient(interceptor)
        val retrofit = NetworkModule.provideRetrofit(config, client, NetworkModule.provideJson())

        val providedApi = NetworkModule.provideSupabaseAuthApi(retrofit)

        assertNotNull(providedApi)
        assertEquals(SupabaseAuthApi::class.java, providedApi.javaClass.interfaces.first())
        assertEquals("https://melody-test.supabase.co/", retrofit.baseUrl().toString())
        assertTrue(client.networkInterceptors.contains(interceptor))
        assertFalse(client.interceptors.contains(interceptor))
    }

    private companion object {
        val testJson = Json { ignoreUnknownKeys = true }
        const val SESSION_JSON = """{"access_token":"access-test","refresh_token":"refresh-test","token_type":"bearer","expires_in":3600,"user":{"id":"user-1","email":"test@example.com"}}"""
    }
}
