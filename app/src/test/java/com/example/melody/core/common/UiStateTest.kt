package com.example.melody.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class UiStateTest {
    @Test
    fun loadingAndEmptyStatesCanBeUsedWithTypedResults() {
        val loading: UiState<String> = UiState.Loading
        val empty: UiState<String> = UiState.Empty

        assertSame(UiState.Loading, loading)
        assertSame(UiState.Empty, empty)
    }

    @Test
    fun successStateRetainsItsData() {
        val state: UiState<List<String>> = UiState.Success(listOf("Melody"))

        assertEquals(listOf("Melody"), (state as UiState.Success).data)
    }

    @Test
    fun errorStateRetainsMessageAndCause() {
        val cause = IllegalStateException("Network unavailable")
        val state = UiState.Error(message = "Unable to load data", cause = cause)

        assertEquals("Unable to load data", state.message)
        assertSame(cause, state.cause)
    }
}
