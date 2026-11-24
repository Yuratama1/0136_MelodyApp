package com.example.melody.core.config

import java.net.URI

data class SupabaseConfig(
    val projectUrl: String,
    val publishableKey: String,
) {
    val isConfigured: Boolean
        get() = validationError() == null

    fun validationError(): String? {
        if (projectUrl.isBlank()) {
            return "Supabase Project URL is missing. Set supabase.url in local.properties."
        }
        if (publishableKey.isBlank()) {
            return "Supabase Publishable Key is missing. Set supabase.publishableKey in local.properties."
        }
        if (!publishableKey.startsWith(PUBLISHABLE_KEY_PREFIX)) {
            return "Supabase key must be a Publishable Key; secret and service role keys are not supported in the app."
        }

        val uri = try {
            URI(projectUrl.trim())
        } catch (_: Exception) {
            return INVALID_URL_MESSAGE
        }

        if (!uri.isAbsolute || uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null) {
            return INVALID_URL_MESSAGE
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return "Supabase Project URL must use HTTPS."
        }

        return null
    }

    fun requireBaseUrl(): String {
        val error = validationError()
        check(error == null) { error ?: INVALID_URL_MESSAGE }

        val trimmedUrl = projectUrl.trim()
        return if (trimmedUrl.endsWith('/')) trimmedUrl else "$trimmedUrl/"
    }

    companion object {
        const val PUBLISHABLE_KEY_PREFIX = "sb_publishable_"
        private const val INVALID_URL_MESSAGE = "Supabase Project URL is not a valid HTTPS URL."
    }
}
