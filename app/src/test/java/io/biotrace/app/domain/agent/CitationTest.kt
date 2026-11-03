package io.biotrace.app.domain.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CitationTest {

    @Test
    fun testCitationFormattingWithBaseline() {
        val now = Instant.parse("2026-11-03T10:00:00Z")
        val citation = Citation(
            id = "hr_123",
            timestamp = now,
            value = 72.0,
            metric = "HEART_RATE",
            baseline = "Avg: 70 bpm"
        )

        val formatted = citation.format()
        assertTrue(formatted.contains("Record #hr_123"))
        assertTrue(formatted.contains("72.0"))
        assertTrue(formatted.contains("Baseline: Avg: 70 bpm"))
    }

    @Test
    fun testCitationFormattingWithoutBaseline() {
        val now = Instant.parse("2026-11-03T10:00:00Z")
        val citation = Citation(
            id = "steps_456",
            timestamp = now,
            value = 8500.0,
            metric = "STEPS"
        )

        val formatted = citation.format()
        assertEquals("[Record #steps_456: 2026-11-03T10:00:00Z, 8500.0]", formatted)
    }
}
