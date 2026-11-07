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
    val totalCalories: Int = 0,
    val dailyTargetCalories: Int = 2200,
    val burntPercentage: Int = 0,
    val totalProtein: Double = 0.0,
    val proteinGoal: Double = 150.0,
    val totalCarbs: Double = 0.0,
    val carbsGoal: Double = 220.0,
    val totalFats: Double = 0.0,
    val fatsGoal: Double = 70.0,
    val scanConfidence: Int? = null,
    val latestScannedTags: List<ScannedFoodTag> = emptyList(),
    val correlationData: WearableCorrelationData? = null,
    val scanMessage: String? = null,
    val error: String? = null,
    val capturedPhotoBitmap: Bitmap? = null,
    val pendingScanResult: MealScanResult? = null
)
