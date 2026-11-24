package com.example.melody.core.di

import com.example.melody.core.config.SupabaseConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class SupabaseApiKeyInterceptor(
    private val config: SupabaseConfig,
) : Interceptor {
    private val supabaseOrigin: HttpUrl? = config
        .takeIf { it.isConfigured }
        ?.projectUrl
        ?.toHttpUrlOrNull()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestUrl = request.url
        val configuredRequest = if (
            supabaseOrigin != null &&
            requestUrl.scheme == supabaseOrigin.scheme &&
            requestUrl.host == supabaseOrigin.host &&
            requestUrl.port == supabaseOrigin.port
        ) {
            request.newBuilder()
                .header(API_KEY_HEADER, config.publishableKey)
                .build()
        } else {
            request.newBuilder()
                .removeHeader(API_KEY_HEADER)
                .build()
        }

        return chain.proceed(configuredRequest)
    }

    private companion object {
        const val API_KEY_HEADER = "apikey"
    }
}
