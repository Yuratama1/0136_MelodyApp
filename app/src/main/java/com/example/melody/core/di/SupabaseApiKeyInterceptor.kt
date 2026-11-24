package com.example.melody.core.di

import com.example.melody.core.config.SupabaseConfig
import okhttp3.Interceptor
import okhttp3.Response

class SupabaseApiKeyInterceptor(
    private val config: SupabaseConfig,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configuredRequest = if (config.isConfigured) {
            request.newBuilder()
                .header(API_KEY_HEADER, config.publishableKey)
                .build()
        } else {
            request
        }

        return chain.proceed(configuredRequest)
    }

    private companion object {
        const val API_KEY_HEADER = "apikey"
    }
}
