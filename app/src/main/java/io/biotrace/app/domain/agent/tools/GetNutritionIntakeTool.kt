package io.biotrace.app.domain.agent.tools

import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class GetNutritionIntakeTool @Inject constructor(
    private val mealDao: MealDao
) : AgentTool {

    override val name: String = "get_nutrition_intake"
    override val description: String =
        "Query meals and macronutrient intake (calories, protein, carbs, fats, fiber). Parameters: startDate (ISO-8601 Instant String, optional), endDate (ISO-8601 Instant String, optional)."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val end = (params["endDate"] as? String)?.let { Instant.parse(it) } ?: Instant.now()
            val start = (params["startDate"] as? String)?.let { Instant.parse(it) } ?: end.minus(Duration.ofDays(1))

            val meals = mealDao.getMealsBetween(start, end)
            if (meals.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = "No meal records found between $start and $end",
                    citations = emptyList()
                )
            }

            val totalCalories = meals.sumOf { it.calories }
            val totalProtein = meals.sumOf { it.proteinGrams }
            val totalCarbs = meals.sumOf { it.carbsGrams }
            val totalFats = meals.sumOf { it.fatsGrams }
            val totalFiber = meals.sumOf { it.fiberGrams ?: 0.0 }

            val dataJson = """{"totalCalories":$totalCalories,"proteinGrams":$totalProtein,"carbsGrams":$totalCarbs,"fatsGrams":$totalFats,"fiberGrams":$totalFiber,"mealCount":${meals.size}}"""

            val citations = meals.map {
                Citation(
                    id = it.id,
                    timestamp = it.consumedAt,
                    value = it.calories.toDouble(),
                    metric = it.name,
                    baseline = "${it.proteinGrams}g P / ${it.carbsGrams}g C / ${it.fatsGrams}g F"
                )
            }

            ToolResult(
                success = true,
                data = dataJson,
                citations = citations
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Failed to get nutrition intake: ${e.message}"
            )
        }
    }
}
