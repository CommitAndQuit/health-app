package io.biotrace.app.data.gadgetbridge

import io.biotrace.app.data.local.entity.StressSampleEntity

data class RawGadgetbridgeStressRow(
    val timestamp: Long,
    val startTime: Long,
    val stress: Int,
    val level: Int,
    val deviceId: Int,
    val deviceName: String? = null,
    val deviceModel: String? = null
)

object GadgetbridgeStressParser {

    /**
     * Converts a raw row from Gadgetbridge's HUAWEI_STRESS_SAMPLE table into a Room entity.
     * Note: Gadgetbridge stores TIMESTAMP and START_TIME in epoch milliseconds (e.g. 1793769529000).
     */
    fun parseRow(row: RawGadgetbridgeStressRow): StressSampleEntity? {
        if (row.stress <= 0) return null

        return StressSampleEntity(
            timestampMillis = row.timestamp,
            deviceId = row.deviceId,
            userId = 1,
            stress = row.stress,
            level = row.level,
            startTimeMillis = row.startTime,
            deviceName = row.deviceName ?: "HONOR MagicWatch 2",
            deviceModel = row.deviceModel ?: "Hebe-B19S"
        )
    }

    /**
     * Parses a list of raw SQLite rows, filtering out invalid or non-positive stress readings.
     */
    fun parseRows(rows: List<RawGadgetbridgeStressRow>): List<StressSampleEntity> {
        return rows.mapNotNull { parseRow(it) }
    }
}
