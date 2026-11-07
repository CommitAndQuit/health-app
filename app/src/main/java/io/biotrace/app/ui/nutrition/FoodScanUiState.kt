package io.biotrace.app.ui.nutrition

import android.graphics.Bitmap
import io.biotrace.app.data.local.entity.MealEntity

data class MealScanResult(
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatsGrams: Double,
    val fiberGrams: Double? = null,
    val glycemic: String,
    val confidence: Int
)

data class FoodScanUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val isManualEntryOpen: Boolean = false,
    val todayMeals: List<MealEntity> = emptyList(),
    val totalCalories: Int = 1840,
    val dailyTargetCalories: Int = 2200,
    val burntPercentage: Int = 84,
    val totalProtein: Double = 135.0,
    val proteinGoal: Double = 150.0,
    val totalCarbs: Double = 180.0,
    val carbsGoal: Double = 220.0,
    val totalFats: Double = 58.0,
    val fatsGoal: Double = 70.0,
    val scanConfidence: Int = 98,
    val latestScannedTags: List<ScannedFoodTag> = listOf(
        ScannedFoodTag(label = "Est", value = "640 kcal"),
        ScannedFoodTag(label = "Protein", value = "42g"),
        ScannedFoodTag(label = "GLYCEMIC", value = "LOW", isGlycemic = true),
        ScannedFoodTag(label = "PLATE SEGMENTED", value = "", isSegmentedBadge = true)
    ),
    val correlationData: WearableCorrelationData = WearableCorrelationData(),
    val scanMessage: String? = null,
    val error: String? = null,
    val capturedPhotoBitmap: Bitmap? = null,
    val pendingScanResult: MealScanResult? = null
)
