package io.biotrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "vitals_records",
    indices = [
        Index(value = ["metric_type"]),
        Index(value = ["timestamp"]),
        Index(value = ["metric_type", "timestamp"])
    ]
)
data class VitalsRecordEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "metric_type")
    val metricType: String, // "HEART_RATE", "HRV", "STEPS", "SLEEP", "CALORIES", "OXYGEN_SATURATION"

    val value: Double,

    val unit: String,

    val timestamp: Instant,

    val source: String, // "HEALTH_CONNECT", "MANUAL"

    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
