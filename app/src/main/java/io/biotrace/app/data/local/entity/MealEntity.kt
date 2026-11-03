package io.biotrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "meals",
    indices = [
        Index(value = ["consumed_at"])
    ]
)
data class MealEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    val calories: Int,

    @ColumnInfo(name = "protein_grams")
    val proteinGrams: Double,

    @ColumnInfo(name = "carbs_grams")
    val carbsGrams: Double,

    @ColumnInfo(name = "fats_grams")
    val fatsGrams: Double,

    @ColumnInfo(name = "fiber_grams")
    val fiberGrams: Double?,

    @ColumnInfo(name = "consumed_at")
    val consumedAt: Instant,

    @ColumnInfo(name = "photo_path")
    val photoPath: String?
)
