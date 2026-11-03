package io.biotrace.app.data.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HealthConnectAvailabilityTest {

    @Test
    fun testAvailabilityEnumValues() {
        val values = HealthConnectAvailability.entries
        assertEquals(3, values.size)
        assertNotNull(HealthConnectAvailability.valueOf("AVAILABLE"))
        assertNotNull(HealthConnectAvailability.valueOf("NOT_INSTALLED"))
        assertNotNull(HealthConnectAvailability.valueOf("NOT_SUPPORTED"))
    }

    @Test
    fun testSyncResultAddition() {
        val r1 = SyncResult(recordsSynced = 10, errors = emptyList())
        val r2 = SyncResult(recordsSynced = 5, errors = listOf("Error 1"))
        val combined = r1 + r2

        assertEquals(15, combined.recordsSynced)
        assertEquals(1, combined.errors.size)
        assertEquals("Error 1", combined.errors.first())
    }
}
