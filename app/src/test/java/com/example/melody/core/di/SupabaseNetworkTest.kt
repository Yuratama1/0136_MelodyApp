package com.example.melody.core.di

import com.example.melody.core.config.SupabaseConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class SupabaseNetworkTest {
    @Test
    fun interceptorAddsPublishableKeyForMatchingSupabaseOriginOnlyAsApikey() {
        val publishableKey = "sb_publishable_test-only-value"
        val config = SupabaseConfig("https://melody-test.supabase.co", publishableKey)
        val observedRequest = interceptRequest(config, "https://melody-test.supabase.co/rest/v1/")

        assertEquals(publishableKey, observedRequest.header("apikey"))
        assertNull(observedRequest.header("Authorization"))
    }

    @Test
    fun requestToDifferentHostDoesNotReceivePublishableKey() {
        val config = configuredConfig()

        val observedRequest = interceptRequest(config, "https://example.com/rest/v1/")

        assertNull(observedRequest.header("apikey"))
        assertNull(observedRequest.header("Authorization"))
    }

    @Test
    fun requestToDifferentPortDoesNotReceivePublishableKey() {
        val config = SupabaseConfig(
            projectUrl = "https://melody-test.supabase.co:8443",
            publishableKey = TEST_PUBLISHABLE_KEY,
        )

        val observedRequest = interceptRequest(config, "https://melody-test.supabase.co:8444/rest/v1/")

        assertNull(observedRequest.header("apikey"))
    }

    @Test
    fun requestOverHttpDoesNotReceivePublishableKey() {
        val config = configuredConfig()

        val observedRequest = interceptRequest(config, "http://melody-test.supabase.co/rest/v1/")

        assertNull(observedRequest.header("apikey"))
    }

    @Test
    fun crossOriginRedirectFollowUpDoesNotCarryPublishableKey() {
        val heldCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(heldCertificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(heldCertificate.certificate)
            .build()
        val server = MockWebServer()
        server.protocols = listOf(Protocol.HTTP_1_1)
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()

        try {
            val config = SupabaseConfig(
                projectUrl = "https://localhost:${server.port}",
                publishableKey = TEST_PUBLISHABLE_KEY,
            )
            val destinationUrl = server.url("/redirect-target")
                .newBuilder()
                .host("127.0.0.1")
                .build()
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", destinationUrl),
            )
            server.enqueue(MockResponse().setBody("{}"))

            val client = OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
                .addNetworkInterceptor(SupabaseApiKeyInterceptor(config))
                .build()
            val response = client.newCall(Request.Builder().url(server.url("/rest/v1/")).build()).execute()
            assertEquals(200, response.code)
            response.close()

            val initialRequest = server.takeRequest()
            val destinationRequest = server.takeRequest()
            assertEquals("localhost", initialRequest.requestUrl?.host)
            assertEquals(TEST_PUBLISHABLE_KEY, initialRequest.getHeader("apikey"))
            assertEquals("127.0.0.1:${server.port}", destinationRequest.getHeader("Host"))
            assertNull(destinationRequest.getHeader("apikey"))
            assertNull(destinationRequest.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun missingConfigurationDoesNotAddApikeyHeader() {
        val config = SupabaseConfig("", "")
        val observedRequest = interceptRequest(config, "https://example.com/")

        assertNull(observedRequest.header("apikey"))
        assertNull(observedRequest.header("Authorization"))
    }

    @Test
    fun hiltProvidersBuildJsonHttpClientAndRetrofitWithConfiguredUrl() {
        val config = SupabaseConfig(
            projectUrl = "https://melody-test.supabase.co",
            publishableKey = "sb_publishable_test-only-value",
        )
        val json = NetworkModule.provideJson()
        val interceptor = NetworkModule.provideSupabaseApiKeyInterceptor(config)
        val client = NetworkModule.provideOkHttpClient(interceptor)
        val retrofit = NetworkModule.provideRetrofit(config, client, json)

        assertTrue(json.configuration.ignoreUnknownKeys)
        assertEquals("https://melody-test.supabase.co/", retrofit.baseUrl().toString())
        assertTrue(client.networkInterceptors.contains(interceptor))
        assertFalse(client.interceptors.contains(interceptor))
    }

    private fun interceptRequest(config: SupabaseConfig, url: String): Request = interceptRequest(
        config,
        Request.Builder().url(url).build(),
    )

    private fun interceptRequest(config: SupabaseConfig, request: Request): Request {
        val observedRequest = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(SupabaseApiKeyInterceptor(config))
            .addInterceptor { chain ->
                observedRequest.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()

        client.newCall(request).execute().close()
        return observedRequest.get()
    }

    private fun configuredConfig() = SupabaseConfig(
        projectUrl = "https://melody-test.supabase.co",
        publishableKey = TEST_PUBLISHABLE_KEY,
    )

    private companion object {
        const val TEST_PUBLISHABLE_KEY = "sb_publishable_test-only-value"
    }
}
