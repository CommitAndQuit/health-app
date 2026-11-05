package io.biotrace.app.data.gadgetbridge

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import io.biotrace.app.data.local.dao.StressDao
import io.biotrace.app.domain.model.StressLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GadgetbridgeStressDataSourceTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var stressDao: StressDao
    private lateinit var dataSource: GadgetbridgeStressDataSource

    @Before
    fun setup() {
        context = mock()
        prefs = mock()
        prefsEditor = mock()
        stressDao = mock()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(prefsEditor)
        whenever(prefsEditor.putString(any(), any())).thenReturn(prefsEditor)
        whenever(prefsEditor.remove(any())).thenReturn(prefsEditor)

        dataSource = GadgetbridgeStressDataSource(context, stressDao)
    }

    @Test
    fun `parseRow correctly parses real-world Honor MagicWatch 2 stress sample`() {
        val row = RawGadgetbridgeStressRow(
            timestamp = 1793769529000L,
            startTime = 1793769472000L,
            stress = 54,
            level = 2,
            deviceId = 1,
            deviceName = "HONOR MagicWatch 2-38D",
            deviceModel = "Hebe-B19S"
        )

        val entity = GadgetbridgeStressParser.parseRow(row)

        assertNotNull(entity)
        assertEquals(1793769529000L, entity!!.timestampMillis)
        assertEquals(1793769472000L, entity.startTimeMillis)
        assertEquals(54, entity.stress)
        assertEquals(2, entity.level)
        assertEquals(1, entity.deviceId)
        assertEquals(StressLevel.NORMAL, entity.stressLevel)
        assertEquals(57L, entity.durationSeconds) // 1793769529000 - 1793769472000 = 57000ms = 57s
        assertEquals("HONOR MagicWatch 2-38D", entity.deviceName)
        assertEquals("Hebe-B19S", entity.deviceModel)
    }

    @Test
    fun `parseRow applies fallback device labels when device table is unjoined`() {
        val row = RawGadgetbridgeStressRow(
            timestamp = 1793769529000L,
            startTime = 1793769472000L,
            stress = 24,
            level = 1,
            deviceId = 1,
            deviceName = null,
            deviceModel = null
        )

        val entity = GadgetbridgeStressParser.parseRow(row)

        assertNotNull(entity)
        assertEquals("HONOR MagicWatch 2", entity!!.deviceName)
        assertEquals("Hebe-B19S", entity.deviceModel)
        assertEquals(StressLevel.RELAXED, entity.stressLevel)
    }

    @Test
    fun `parseRow filters out non-positive stress scores`() {
        val rowZero = RawGadgetbridgeStressRow(
            timestamp = 1793769529000L,
            startTime = 1793769472000L,
            stress = 0,
            level = 0,
            deviceId = 1
        )
        val rowNegative = RawGadgetbridgeStressRow(
            timestamp = 1793769529000L,
            startTime = 1793769472000L,
            stress = -1,
            level = 0,
            deviceId = 1
        )

        assertNull(GadgetbridgeStressParser.parseRow(rowZero))
        assertNull(GadgetbridgeStressParser.parseRow(rowNegative))
    }

    @Test
    fun `parseRows accurately processes 252 stress samples`() {
        val rawBatch = (1..252).map { index ->
            val score = (index % 99) + 1
            val level = when {
                score in 1..29 -> 1
                score in 30..59 -> 2
                score in 60..79 -> 3
                else -> 4
            }
            RawGadgetbridgeStressRow(
                timestamp = 1793760000000L + (index * 60000L),
                startTime = 1793760000000L + (index * 60000L) - 58000L,
                stress = score,
                level = level,
                deviceId = 1
            )
        }

        val parsed = GadgetbridgeStressParser.parseRows(rawBatch)

        assertEquals(252, parsed.size)
        assertTrue(parsed.all { it.stress in 1..99 })
        assertTrue(parsed.all { it.durationSeconds == 58L })
    }

    @Test
    fun `refreshFromSavedUri returns null when no URI has been saved`() = runTest {
        whenever(prefs.getString("saved_db_uri", null)).thenReturn(null)

        val result = dataSource.refreshFromSavedUri()
        assertNull(result)
        assertFalse(dataSource.hasSavedUri())
    }

    @Test
    fun `clearSavedUri removes the key from SharedPreferences`() {
        dataSource.clearSavedUri()
        org.mockito.kotlin.verify(prefsEditor).remove("saved_db_uri")
    }
}
