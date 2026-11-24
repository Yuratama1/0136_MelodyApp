package com.example.melody.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseConfigTest {
    @Test
    fun validConfigurationIsRecognizedAndBaseUrlHasTrailingSlash() {
        val config = validConfig()

        assertTrue(config.isConfigured)
        assertNull(config.validationError())
        assertEquals("https://melody-test.supabase.co/", config.requireBaseUrl())
    }

    @Test
    fun emptyProjectUrlIsReportedWithoutExposingTheKey() {
        val config = validConfig(projectUrl = "")

        assertFalse(config.isConfigured)
        assertTrue(config.validationError().orEmpty().contains("Project URL is missing"))
        assertFalse(config.validationError().orEmpty().contains(config.publishableKey))
    }

    @Test
    fun emptyPublishableKeyIsReported() {
        val config = validConfig(publishableKey = "")

        assertFalse(config.isConfigured)
        assertTrue(config.validationError().orEmpty().contains("Publishable Key is missing"))
    }

    @Test
    fun malformedUrlIsRejectedWithoutIncludingItsValue() {
        val invalidUrl = "not a url"
        val config = validConfig(projectUrl = invalidUrl)

        assertFalse(config.isConfigured)
        assertTrue(config.validationError().orEmpty().contains("not a valid HTTPS URL"))
        assertFalse(config.validationError().orEmpty().contains(invalidUrl))
    }

    @Test
    fun hostedHttpUrlIsRejected() {
        val config = validConfig(projectUrl = "http://melody-test.supabase.co")

        assertFalse(config.isConfigured)
        assertTrue(config.validationError().orEmpty().contains("must use HTTPS"))
    }

    @Test
    fun nonPublishableKeyIsRejectedWithoutEchoingItsValue() {
        val invalidKey = "not-a-publishable-key"
        val config = validConfig(publishableKey = invalidKey)

        assertFalse(config.isConfigured)
        assertTrue(config.validationError().orEmpty().contains("secret and service role keys"))
        assertFalse(config.validationError().orEmpty().contains(invalidKey))
    }

    private fun validConfig(
        projectUrl: String = "https://melody-test.supabase.co",
        publishableKey: String = "sb_publishable_test-only-value",
    ) = SupabaseConfig(projectUrl, publishableKey)
}
