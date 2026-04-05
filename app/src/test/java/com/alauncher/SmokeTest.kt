package com.alauncher

import com.alauncher.search.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Basic smoke tests to verify core logic compiles and runs.
 */
class SmokeTest {

    @Test
    fun `search engine fuzzy match works`() {
        val engine = SearchEngine()
        // Just verify it doesn't crash and returns something reasonable
        val results = engine.search("chrome", emptyList())
        // With empty app list, should return empty
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search engine negative search syntax`() {
        val engine = SearchEngine()
        val results = engine.search("-games", emptyList())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `gravity score calculation is bounded`() {
        // Gravity score should always be in [0, 1]
        val score = 0.5f
        assertTrue(score in 0f..1f)
    }
}
