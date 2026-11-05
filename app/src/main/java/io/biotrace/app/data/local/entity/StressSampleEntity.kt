package io.biotrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import io.biotrace.app.domain.model.StressLevel

@Entity(
    tableName = "stress_samples",
    primaryKeys = ["timestampMillis", "deviceId"],
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["deviceId"]),
        Index(value = ["stress"])
    ]
)
data class StressSampleEntity(
    @ColumnInfo(name = "timestampMillis")
    val timestampMillis: Long,

    @ColumnInfo(name = "deviceId")
    val deviceId: Int,

    @ColumnInfo(name = "userId")
    val userId: Int = 1,

    @ColumnInfo(name = "stress")
    val stress: Int,

    @ColumnInfo(name = "level")
    val level: Int,

    @ColumnInfo(name = "startTimeMillis")
    val startTimeMillis: Long,

    @ColumnInfo(name = "deviceName")
    val deviceName: String? = null,

    @ColumnInfo(name = "deviceModel")
    val deviceModel: String? = null
) {
    val stressLevel: StressLevel
        get() = StressLevel.fromLevel(level, stress)

    val durationSeconds: Long
        get() = if (timestampMillis > startTimeMillis && startTimeMillis > 0) {
            (timestampMillis - startTimeMillis) / 1000L
        } else {
            60L
        }
}
