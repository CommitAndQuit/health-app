package io.biotrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "lab_biomarkers",
    indices = [
        Index(value = ["biomarker_name"]),
        Index(value = ["recorded_at"])
    ]
)
data class LabBiomarkerEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "biomarker_name")
    val biomarkerName: String, // Normalized name (e.g., "HBA1C", "CHOLESTEROL_TOTAL")

    @ColumnInfo(name = "raw_name")
    val rawName: String, // Original string from report

    val value: Double,

    val unit: String,

    @ColumnInfo(name = "reference_min")
    val referenceMin: Double?,

    @ColumnInfo(name = "reference_max")
    val referenceMax: Double?,

    val flag: String?, // "HIGH", "LOW", "NORMAL"

    @ColumnInfo(name = "recorded_at")
    val recordedAt: Instant,

    val source: String // "MANUAL", "VISION_LLM"
)
