package io.biotrace.app.ui.nutrition

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.domain.llm.LlmClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

data class ScannedFoodTag(
    val label: String,
    val value: String,
    val isGlycemic: Boolean = false,
    val isSegmentedBadge: Boolean = false
)

data class WearableCorrelationData(
    val earlyTiming: String = "Before 7:30 PM",
    val earlyHrv: Int = 0,
    val earlyLabel: String = "",
    val lateTiming: String = "After 8:30 PM",
    val lateHrv: Int = 0,
    val lateLabel: String = "",
    val deltaMs: Int = 0,
    val deepSleepBonusMin: Int = 0
)

@HiltViewModel
class FoodScanViewModel @Inject constructor(
    private val mealDao: MealDao,
    private val llmClient: LlmClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodScanUiState())
    val uiState: StateFlow<FoodScanUiState> = _uiState.asStateFlow()

    init {
        initOrLoadMeals()
    }

    private fun initOrLoadMeals() {
        viewModelScope.launch {
            loadTodayMeals()
        }
    }

    fun loadTodayMeals() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
            val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()

            try {
                mealDao.getMealsBetweenFlow(startOfDay, endOfDay).collect { meals ->
                    if (meals.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                todayMeals = emptyList(),
                                totalCalories = 0,
                                burntPercentage = 0,
                                totalProtein = 0.0,
                                totalCarbs = 0.0,
                                totalFats = 0.0
                            )
                        }
                    } else {
                        val mealsCals = meals.sumOf { it.calories }
                        val mealsProtein = meals.sumOf { it.proteinGrams }
                        val mealsCarbs = meals.sumOf { it.carbsGrams }
                        val mealsFats = meals.sumOf { it.fatsGrams }

                        val target = _uiState.value.dailyTargetCalories
                        val pct = if (target > 0) ((mealsCals.toDouble() / target.toDouble()) * 100).toInt().coerceIn(0, 100) else 0

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                todayMeals = meals,
                                totalCalories = mealsCals,
                                burntPercentage = pct,
                                totalProtein = (mealsProtein * 10).toInt() / 10.0,
                                totalCarbs = (mealsCarbs * 10).toInt() / 10.0,
                                totalFats = (mealsFats * 10).toInt() / 10.0
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun openManualEntry(open: Boolean) {
        _uiState.update { it.copy(isManualEntryOpen = open) }
    }

    fun addManualMeal(
        name: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fats: Double,
        fiber: Double?
    ) {
        viewModelScope.launch {
            val entity = MealEntity(
                id = "meal_${UUID.randomUUID()}",
                name = name.trim(),
                calories = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatsGrams = fats,
                fiberGrams = fiber,
                consumedAt = Instant.now(),
                photoPath = null
            )
            mealDao.insert(entity)
            _uiState.update { it.copy(isManualEntryOpen = false) }
        }
    }

    fun scanFoodPhoto(base64Image: String, mimeType: String = "image/jpeg") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanMessage = "Estimating calories & macronutrients via Vision AI...",
                    error = null
                )
            }

            try {
                val prompt = """
                    You are a clinical nutrition & macro estimation AI. Analyze this meal photo carefully.
                    Estimate the meal title, estimated portion calories, macronutrient grams, and glycemic impact (LOW, MEDIUM, HIGH).
                    Respond ONLY with a valid JSON object in this format:
                    {
                      "name": "Grilled Salmon Harvest Bowl",
                      "calories": 640,
                      "protein": 42.0,
                      "carbs": 58.0,
                      "fats": 22.0,
                      "fiber": 6.0,
                      "glycemic": "LOW",
                      "confidence": 98
                    }
                    Return ONLY the raw JSON object, without markdown ticks.
                """.trimIndent()

                val response = llmClient.generateContentWithImage(prompt, base64Image, mimeType)
                val cleanedJson = Regex("""\{[\s\S]*\}""").find(response)?.value
                    ?: response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)

                val calories = if (json.has("calories")) json.optInt("calories", 0) else 0
                if (calories <= 0) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            error = "Could not estimate calories for this item. Please retake photo or enter manually."
                        )
                    }
                    return@launch
                }

                val name = json.optString("name", "Scanned Meal").ifBlank { "Scanned Meal" }
                val protein = json.optDouble("protein", 0.0)
                val carbs = json.optDouble("carbs", 0.0)
                val fats = json.optDouble("fats", 0.0)
                val fiber = if (json.has("fiber")) json.optDouble("fiber", 0.0) else null
                val glycemic = json.optString("glycemic", "LOW")
                val confidence = if (json.has("confidence")) json.optInt("confidence", 0) else 0

                val mealEntity = MealEntity(
                    id = "meal_${UUID.randomUUID()}",
                    name = name,
                    calories = calories,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatsGrams = fats,
                    fiberGrams = fiber,
                    consumedAt = Instant.now(),
                    photoPath = null
                )

                mealDao.insert(mealEntity)

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanConfidence = if (confidence > 0) confidence else null,
                        latestScannedTags = listOf(
                            ScannedFoodTag(label = "Est", value = "$calories kcal"),
                            ScannedFoodTag(label = "Protein", value = "${protein.toInt()}g"),
                            ScannedFoodTag(label = "GLYCEMIC", value = glycemic, isGlycemic = true),
                            ScannedFoodTag(label = "PLATE SEGMENTED", value = "", isSegmentedBadge = true)
                        ),
                        scanMessage = "Logged '$name' ($calories kcal)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to analyze meal image. Check Gemini API key in Settings."
                    )
                }
            }
        }
    }

    fun scanCapturedBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    capturedPhotoBitmap = bitmap,
                    isScanning = true,
                    scanMessage = "Analyzing meal with Gemini 2.5 Flash...",
                    error = null,
                    pendingScanResult = null
                )
            }

            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val bytes = outputStream.toByteArray()
                val base64Image = try {
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } catch (e: Throwable) {
                    null
                } ?: java.util.Base64.getEncoder().encodeToString(bytes)

                val prompt = """
                    You are a clinical nutrition & macro estimation AI. Analyze this meal photo carefully.
                    Estimate the meal title, estimated portion calories, macronutrient grams, and glycemic impact (LOW, MEDIUM, HIGH).
                    Respond ONLY with a valid JSON object in this format:
                    {
                      "name": "Grilled Salmon Harvest Bowl",
                      "calories": 640,
                      "protein": 42.0,
                      "carbs": 58.0,
                      "fats": 22.0,
                      "fiber": 6.0,
                      "glycemic": "LOW",
                      "confidence": 98
                    }
                    Return ONLY the raw JSON object, without markdown ticks.
                """.trimIndent()

                val response = llmClient.generateContentWithImage(prompt, base64Image, "image/jpeg")
                val cleanedJson = Regex("""\{[\s\S]*\}""").find(response)?.value
                    ?: response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)

                val calories = if (json.has("calories")) json.optInt("calories", 0) else 0
                if (calories <= 0) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            pendingScanResult = null,
                            error = "Could not estimate calories for this item. Please retake photo or enter manually."
                        )
                    }
                    return@launch
                }

                val name = json.optString("name", "Scanned Meal").ifBlank { "Scanned Meal" }
                val protein = json.optDouble("protein", 0.0)
                val carbs = json.optDouble("carbs", 0.0)
                val fats = json.optDouble("fats", 0.0)
                val fiber = if (json.has("fiber")) json.optDouble("fiber", 0.0) else null
                val glycemic = json.optString("glycemic", "LOW")
                val confidence = if (json.has("confidence")) json.optInt("confidence", 0) else 0

                val scanResult = MealScanResult(name, calories, protein, carbs, fats, fiber, glycemic, confidence)

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        pendingScanResult = scanResult,
                        scanConfidence = if (confidence > 0) confidence else null,
                        scanMessage = "Estimated $calories kcal for $name"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to analyze meal image. Check Gemini API key in Settings."
                    )
                }
            }
        }
    }

    fun confirmAndLogScan() {
        val pending = _uiState.value.pendingScanResult ?: return
        viewModelScope.launch {
            val entity = MealEntity(
                id = "meal_${UUID.randomUUID()}",
                name = pending.name,
                calories = pending.calories,
                proteinGrams = pending.proteinGrams,
                carbsGrams = pending.carbsGrams,
                fatsGrams = pending.fatsGrams,
                fiberGrams = pending.fiberGrams,
                consumedAt = Instant.now(),
                photoPath = null
            )
            mealDao.insert(entity)
            _uiState.update {
                it.copy(
                    pendingScanResult = null,
                    latestScannedTags = listOf(
                        ScannedFoodTag(label = "Est", value = "${pending.calories} kcal"),
                        ScannedFoodTag(label = "Protein", value = "${pending.proteinGrams.toInt()}g"),
                        ScannedFoodTag(label = "GLYCEMIC", value = pending.glycemic, isGlycemic = true),
                        ScannedFoodTag(label = "PLATE SEGMENTED", value = "", isSegmentedBadge = true)
                    ),
                    scanMessage = "Logged '${pending.name}' (${pending.calories} kcal)"
                )
            }
        }
    }

    fun dismissScanResult() {
        _uiState.update {
            it.copy(
                pendingScanResult = null,
                capturedPhotoBitmap = null
            )
        }
    }
}
