package io.biotrace.app.domain.agent.tools

import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import io.biotrace.app.domain.analytics.correlation.DailyMetricSample
import io.biotrace.app.domain.analytics.correlation.MetricPairwiseCorrelator
import io.biotrace.app.domain.repository.VitalsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryCorrelationMatrixTool @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val metricPairwiseCorrelator: MetricPairwiseCorrelator = MetricPairwiseCorrelator
) : AgentTool {

    override val name: String = "query_correlation_matrix"
    override val description: String =
        "Query pairwise correlation coefficients (Pearson r, Spearman rho) and statistical significance across longitudinal biometric metric streams from Health Connect (e.g., HEART_RATE, HRV_RMSSD, SLEEP, STEPS, CALORIES). Parameters: metric1 (String, optional), metric2 (String, optional), lagDays (Int, optional, default 0)."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val metric1 = params["metric1"] as? String
            val metric2 = params["metric2"] as? String
            val lagDays = ((params["lagDays"] as? Number)?.toInt()) ?: 0
            val alpha = ((params["alpha"] as? Number)?.toDouble()) ?: 0.05

            val now = Instant.now()
            val thirtyDaysAgo = now.minus(Duration.ofDays(30))

            if (!metric1.isNullOrBlank() && !metric2.isNullOrBlank()) {
                val series1Points = vitalsRepository.getVitalsBetween(metric1.trim().uppercase(Locale.ROOT), thirtyDaysAgo, now)
                val series2Points = vitalsRepository.getVitalsBetween(metric2.trim().uppercase(Locale.ROOT), thirtyDaysAgo, now)

                val zone = ZoneId.systemDefault()
                val series1 = series1Points
                    .groupBy { it.timestamp.atZone(zone).toLocalDate() }
                    .map { (date, pts) -> DailyMetricSample(date, pts.map { it.value }.average()) }
                    .sortedBy { it.date }

                val series2 = series2Points
                    .groupBy { it.timestamp.atZone(zone).toLocalDate() }
                    .map { (date, pts) -> DailyMetricSample(date, pts.map { it.value }.average()) }
                    .sortedBy { it.date }

                val result = metricPairwiseCorrelator.correlate(
                    metric1Name = metric1,
                    series1 = series1,
                    metric2Name = metric2,
                    series2 = series2,
                    lagDays = lagDays,
                    alpha = alpha
                )

                val json = JSONObject().apply {
                    put("metric1", result.metric1)
                    put("metric2", result.metric2)
                    put("pearsonR", result.pearsonR)
                    put("spearmanRho", result.spearmanRho)
                    put("sampleCount", result.sampleCount)
                    put("strength", result.strength.label)
                    put("isStatisticallySignificant", result.isStatisticallySignificant)
                    put("pValue", result.pValue)
                    put("lagDays", result.lagDays)
                    put("narrative", result.narrative)
                }

                val citations = (series1Points.take(3) + series2Points.take(3)).map { pt ->
                    Citation(
                        id = pt.id,
                        timestamp = pt.timestamp,
                        value = pt.value,
                        metric = pt.metricType,
                        baseline = "Daily sample"
                    )
                }

                ToolResult(
                    success = true,
                    data = json.toString(),
                    citations = citations
                )
            } else {
                val metricsToQuery = listOf("HEART_RATE", "HRV_RMSSD", "SLEEP", "STEPS", "CALORIES")
                val zone = ZoneId.systemDefault()
                val allPoints = mutableListOf<io.biotrace.app.domain.model.VitalPoint>()
                val seriesMap = mutableMapOf<String, List<DailyMetricSample>>()

                for (m in metricsToQuery) {
                    val pts = vitalsRepository.getVitalsBetween(m, thirtyDaysAgo, now)
                    if (pts.isNotEmpty()) {
                        allPoints.addAll(pts)
                        val daily = pts
                            .groupBy { it.timestamp.atZone(zone).toLocalDate() }
                            .map { (date, points) -> DailyMetricSample(date, points.map { it.value }.average()) }
                            .sortedBy { it.date }
                        seriesMap[m] = daily
                    }
                }

                if (seriesMap.size < 2) {
                    return ToolResult(
                        success = true,
                        data = "Insufficient distinct metric streams in Health Connect (need at least 2) to compute correlation matrix.",
                        citations = emptyList()
                    )
                }

                val matrix = metricPairwiseCorrelator.computeCorrelationMatrix(
                    metrics = seriesMap,
                    lagDays = lagDays,
                    alpha = alpha
                )

                val jsonArray = JSONArray()
                for (res in matrix) {
                    val obj = JSONObject().apply {
                        put("metric1", res.metric1)
                        put("metric2", res.metric2)
                        put("pearsonR", res.pearsonR)
                        put("spearmanRho", res.spearmanRho)
                        put("sampleCount", res.sampleCount)
                        put("strength", res.strength.label)
                        put("isStatisticallySignificant", res.isStatisticallySignificant)
                        put("narrative", res.narrative)
                    }
                    jsonArray.put(obj)
                }

                val citations = allPoints.takeLast(6).map { pt ->
                    Citation(
                        id = pt.id,
                        timestamp = pt.timestamp,
                        value = pt.value,
                        metric = pt.metricType,
                        baseline = "Health Connect measurement"
                    )
                }

                ToolResult(
                    success = true,
                    data = jsonArray.toString(),
                    citations = citations
                )
            }
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Failed to evaluate correlation matrix: ${e.message}"
            )
        }
    }
}
