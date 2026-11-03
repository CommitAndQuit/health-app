package io.biotrace.app.ui.nutrition

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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class FoodScanUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val isManualEntryOpen: Boolean = false,
    val todayMeals: List<MealEntity> = emptyList(),
    val totalCalories: Int = 0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFats: Double = 0.0,
    val scanMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class FoodScanViewModel @Inject constructor(
    private val mealDao: MealDao,
    private val llmClient: LlmClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodScanUiState())
    val uiState: StateFlow<FoodScanUiState> = _uiState.asStateFlow()

    init {
        loadTodayMeals()
    }

    fun loadTodayMeals() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
            val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()

            try {
                mealDao.getMealsBetweenFlow(startOfDay, endOfDay).collect { meals ->
                    val cals = meals.sumOf { it.calories }
                    val protein = meals.sumOf { it.proteinGrams }
                    val carbs = meals.sumOf { it.carbsGrams }
                    val fats = meals.sumOf { it.fatsGrams }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todayMeals = meals,
                            totalCalories = cals,
                            totalProtein = (protein * 10).toInt() / 10.0,
                            totalCarbs = (carbs * 10).toInt() / 10.0,
                            totalFats = (fats * 10).toInt() / 10.0
                        )
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
                    Estimate the meal title, estimated portion calories, and macronutrient grams.
                    Respond ONLY with a valid JSON object in this format:
                    {
                      "name": "Grilled Chicken Salad with Olive Oil",
                      "calories": 480,
                      "protein": 42.0,
                      "carbs": 18.0,
                      "fats": 24.0,
                      "fiber": 6.0
                    }
                    Return ONLY the raw JSON object, without markdown ticks.
                """.trimIndent()

                val response = llmClient.generateContentWithImage(prompt, base64Image, mimeType)
                val cleanedJson = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)

                val name = json.optString("name", "Scanned Meal")
                val calories = json.optInt("calories", 0)
                val protein = json.optDouble("protein", 0.0)
                val carbs = json.optDouble("carbs", 0.0)
                val fats = json.optDouble("fats", 0.0)
                val fiber = if (json.has("fiber")) json.optDouble("fiber", 0.0) else null

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
}
