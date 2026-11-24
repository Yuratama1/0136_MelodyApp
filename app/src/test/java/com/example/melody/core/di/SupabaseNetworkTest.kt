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
    fun interceptorAddsPublishableKeyOnlyAsApikeyHeader() {
        val publishableKey = "sb_publishable_test-only-value"
        val config = SupabaseConfig("https://melody-test.supabase.co", publishableKey)
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

        client.newCall(Request.Builder().url("https://melody-test.supabase.co/rest/v1/").build())
            .execute()
            .close()

        assertEquals(publishableKey, observedRequest.get().header("apikey"))
        assertNull(observedRequest.get().header("Authorization"))
    }

    @Test
    fun incompleteConfigurationDoesNotAddApikeyHeader() {
        val config = SupabaseConfig("", "")
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

        client.newCall(Request.Builder().url("https://example.test/").build()).execute().close()

        assertNull(observedRequest.get().header("apikey"))
        assertNull(observedRequest.get().header("Authorization"))
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
}
