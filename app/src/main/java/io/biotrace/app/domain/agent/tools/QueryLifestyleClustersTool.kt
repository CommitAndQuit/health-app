package io.biotrace.app.domain.agent.tools

import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import io.biotrace.app.domain.analytics.clustering.DailyFeatureVector
import io.biotrace.app.domain.analytics.clustering.LifestyleArchetype
import io.biotrace.app.domain.analytics.clustering.LifestyleKMeansClusterer
import io.biotrace.app.domain.repository.VitalsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryLifestyleClustersTool @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val mealDao: MealDao,
    private val lifestyleKMeansClusterer: LifestyleKMeansClusterer
) : AgentTool {

    override val name: String = "query_lifestyle_clusters"
    override val description: String =
        "Evaluate unsupervised K-Means daily lifestyle clustering across activity (steps), sleep duration, autonomic recovery (HRV), energy burn, and late dinner timing to identify behavioral archetypes (Peak Performance, Deep Recovery, Sedentary Maintenance, Strain Depleted). Parameters: k (Int, optional, default 4)."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val k = ((params["k"] as? Number)?.toInt()) ?: 4
            val now = Instant.now()
            val thirtyDaysAgo = now.minus(Duration.ofDays(30))

            val steps = vitalsRepository.getVitalsBetween("STEPS", thirtyDaysAgo, now)
            val sleep = vitalsRepository.getVitalsBetween("SLEEP", thirtyDaysAgo, now)
            val hrv = vitalsRepository.getVitalsBetween("HRV_RMSSD", thirtyDaysAgo, now).ifEmpty {
                vitalsRepository.getVitalsBetween("HRV", thirtyDaysAgo, now)
            }
            val calories = vitalsRepository.getVitalsBetween("CALORIES", thirtyDaysAgo, now)
            val meals = mealDao.getMealsBetween(thirtyDaysAgo, now)

            val zone = ZoneId.systemDefault()
            val allDates = mutableSetOf<LocalDate>()

            val stepsByDate = steps.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            val sleepByDate = sleep.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            val hrvByDate = hrv.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            val calByDate = calories.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            val mealsByDate = meals.groupBy { it.consumedAt.atZone(zone).toLocalDate() }

            allDates.addAll(stepsByDate.keys)
            allDates.addAll(sleepByDate.keys)
            allDates.addAll(hrvByDate.keys)
            allDates.addAll(calByDate.keys)

            if (allDates.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = "Insufficient daily longitudinal observations in Health Connect to perform lifestyle clustering.",
                    citations = emptyList()
                )
            }

            val featureVectors = allDates.sorted().map { date ->
                val daySteps = stepsByDate[date]?.sumOf { it.value } ?: 0.0
                val daySleep = sleepByDate[date]?.sumOf { it.value } ?: 0.0
                val dayHrv = hrvByDate[date]?.map { it.value }?.average() ?: 0.0
                val dayCal = calByDate[date]?.sumOf { it.value } ?: 0.0
                val dayMeals = mealsByDate[date] ?: emptyList()
                val lateDinner = dayMeals.any { m ->
                    m.consumedAt.atZone(zone).toLocalTime().isAfter(LocalTime.of(20, 30))
                }

                DailyFeatureVector(
                    date = date,
                    steps = daySteps,
                    sleepHours = daySleep,
                    hrvRmssd = dayHrv,
                    activeCalories = dayCal,
                    lateDinnerFlag = lateDinner
                )
            }

            val clusterResult = lifestyleKMeansClusterer.cluster(featureVectors, k = k)

            val displayName = when (clusterResult.archetype) {
                LifestyleArchetype.PEAK_PERFORMANCE -> "Peak Performance"
                LifestyleArchetype.DEEP_RECOVERY -> "Deep Recovery"
                LifestyleArchetype.STRAIN_DEPLETED -> "Strain Depleted"
                LifestyleArchetype.SEDENTARY_MAINTENANCE -> "Sedentary Maintenance"
            }
            val description = when (clusterResult.archetype) {
                LifestyleArchetype.PEAK_PERFORMANCE -> "High activity exertion balanced with autonomic resilience."
                LifestyleArchetype.DEEP_RECOVERY -> "Extended restorative sleep and strong parasympathetic recovery."
                LifestyleArchetype.STRAIN_DEPLETED -> "Elevated physical strain compounded by sleep deficit or depressed HRV."
                LifestyleArchetype.SEDENTARY_MAINTENANCE -> "Low daily energy expenditure and stable baseline recovery."
            }

            val json = JSONObject().apply {
                put("dominantArchetype", clusterResult.archetype.name)
                put("dominantDisplayName", displayName)
                put("dominantPercentage", clusterResult.dominantPercentage)
                put("description", description)
                val insightsArray = JSONArray()
                clusterResult.clusterInsights.forEach { insightsArray.put(it) }
                put("clusterInsights", insightsArray)
                put("totalDaysClassified", clusterResult.dailyAssignments.size)
            }

            val citations = (steps.take(2) + sleep.take(2) + hrv.take(2)).map { pt ->
                Citation(
                    id = pt.id,
                    timestamp = pt.timestamp,
                    value = pt.value,
                    metric = pt.metricType,
                    baseline = "Archetype: $displayName"
                )
            }

            ToolResult(
                success = true,
                data = json.toString(),
                citations = citations
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Failed to evaluate lifestyle clusters: ${e.message}"
            )
        }
    }
}
