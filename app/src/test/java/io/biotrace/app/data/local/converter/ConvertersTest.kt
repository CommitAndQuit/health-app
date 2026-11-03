package io.biotrace.app.data.local.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testInstantConverter() {
        val now = Instant.parse("2026-11-03T10:15:30.000Z")
        val epochMilli = converters.fromInstant(now)

        assertEquals(now.toEpochMilli(), epochMilli)

        val restored = converters.toInstant(epochMilli)
        assertEquals(now, restored)
    }

    @Test
    fun testNullInstant() {
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
    }
}
