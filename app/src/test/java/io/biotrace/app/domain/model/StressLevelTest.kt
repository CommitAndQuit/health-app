package io.biotrace.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StressLevelTest {

    @Test
    fun `fromScore maps scores 1 to 29 to RELAXED`() {
        assertEquals(StressLevel.RELAXED, StressLevel.fromScore(1))
        assertEquals(StressLevel.RELAXED, StressLevel.fromScore(15))
        assertEquals(StressLevel.RELAXED, StressLevel.fromScore(29))
    }

    @Test
    fun `fromScore maps scores 30 to 59 to NORMAL`() {
        assertEquals(StressLevel.NORMAL, StressLevel.fromScore(30))
        assertEquals(StressLevel.NORMAL, StressLevel.fromScore(45))
        assertEquals(StressLevel.NORMAL, StressLevel.fromScore(54))
        assertEquals(StressLevel.NORMAL, StressLevel.fromScore(59))
    }

    @Test
    fun `fromScore maps scores 60 to 79 to MEDIUM`() {
        assertEquals(StressLevel.MEDIUM, StressLevel.fromScore(60))
        assertEquals(StressLevel.MEDIUM, StressLevel.fromScore(70))
        assertEquals(StressLevel.MEDIUM, StressLevel.fromScore(79))
    }

    @Test
    fun `fromScore maps scores 80 to 99 to HIGH`() {
        assertEquals(StressLevel.HIGH, StressLevel.fromScore(80))
        assertEquals(StressLevel.HIGH, StressLevel.fromScore(88))
        assertEquals(StressLevel.HIGH, StressLevel.fromScore(99))
    }

    @Test
    fun `fromLevel maps level integer 1, 2, 3, 4 accurately`() {
        assertEquals(StressLevel.RELAXED, StressLevel.fromLevel(1))
        assertEquals(StressLevel.NORMAL, StressLevel.fromLevel(2))
        assertEquals(StressLevel.MEDIUM, StressLevel.fromLevel(3))
        assertEquals(StressLevel.HIGH, StressLevel.fromLevel(4))
    }

    @Test
    fun `fromLevel with unknown level falls back to score classification`() {
        assertEquals(StressLevel.RELAXED, StressLevel.fromLevel(0, score = 20))
        assertEquals(StressLevel.MEDIUM, StressLevel.fromLevel(99, score = 65))
        assertEquals(StressLevel.NORMAL, StressLevel.fromLevel(0, score = 0))
    }

    @Test
    fun `StressLevel properties have expected clinical badges and colors`() {
        assertEquals("Relaxed", StressLevel.RELAXED.title)
        assertEquals("1–29", StressLevel.RELAXED.rangeDescription)

        assertEquals("Normal", StressLevel.NORMAL.title)
        assertEquals("30–59", StressLevel.NORMAL.rangeDescription)

        assertEquals("Medium", StressLevel.MEDIUM.title)
        assertEquals("60–79", StressLevel.MEDIUM.rangeDescription)

        assertEquals("High", StressLevel.HIGH.title)
        assertEquals("80–99", StressLevel.HIGH.rangeDescription)
    }
}
