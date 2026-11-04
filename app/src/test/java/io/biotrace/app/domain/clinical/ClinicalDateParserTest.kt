package io.biotrace.app.domain.clinical

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ClinicalDateParserTest {

    @Test
    fun `parses standard ISO date format`() {
        val instant = ClinicalDateParser.parseToInstant("2026-10-28")
        val expected = LocalDate.of(2026, 10, 28).atTime(12, 0).toInstant(ZoneOffset.UTC)
        assertEquals(expected, instant)
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(instant))
    }

    @Test
    fun `parses DD-MM-YYYY and DD-slash-MM-slash-YYYY formats`() {
        val slashInstant = ClinicalDateParser.parseToInstant("28/10/2026")
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(slashInstant))

        val dashInstant = ClinicalDateParser.parseToInstant("28-10-2026")
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(dashInstant))
    }

    @Test
    fun `parses US format MM-slash-DD-slash-YYYY`() {
        val instant = ClinicalDateParser.parseToInstant("10/28/2026")
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(instant))
    }

    @Test
    fun `parses textual month formats`() {
        val dmyText = ClinicalDateParser.parseToInstant("28 Oct 2026")
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(dmyText))

        val mdyText = ClinicalDateParser.parseToInstant("October 28, 2026")
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(mdyText))
    }

    @Test
    fun `parses timestamps with hours and minutes`() {
        val dtInstant = ClinicalDateParser.parseToInstant("28/10/2026 09:47 AM")
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(dtInstant))
    }

    @Test
    fun `parses embedded dates within clinical text`() {
        val embedded = ClinicalDateParser.parseToInstant("Collected on: 2026-10-28 at central lab")
        assertEquals("2026-10-28", ClinicalDateParser.toIsoDateString(embedded))
    }

    @Test
    fun `falls back gracefully on empty or unparseable text`() {
        val fallback = Instant.ofEpochMilli(1700000000000L)
        val resultNull = ClinicalDateParser.parseToInstant(null, fallback)
        assertEquals(fallback, resultNull)

        val resultInvalid = ClinicalDateParser.parseToInstant("NotADate", fallback)
        assertEquals(fallback, resultInvalid)
    }
}
