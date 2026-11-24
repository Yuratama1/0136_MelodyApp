package com.example.melody.core.di

import com.example.melody.core.config.SupabaseConfig
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
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
        assertTrue(client.interceptors.contains(interceptor))
    }

    private fun interceptRequest(config: SupabaseConfig, url: String): Request {
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

        client.newCall(Request.Builder().url(url).build()).execute().close()
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
