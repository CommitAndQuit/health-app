package io.biotrace.app.domain.analytics.correlation

/**
 * Encapsulates the complete result of bivariate correlation analysis between two biometric metrics.
 */
data class CorrelationResult(
    val metric1: String,
    val metric2: String,
    val pearsonR: Double,
    val spearmanRho: Double,
    val sampleCount: Int,
    val strength: CorrelationStrength,
    val isStatisticallySignificant: Boolean,
    val narrative: String,
    val tStatistic: Double = 0.0,
    val pValue: Double = 1.0,
    val lagDays: Int = 0
)
