package io.biotrace.app.domain.analytics.clustering

import java.time.LocalDate

/**
 * Behavioral and physiological lifestyle archetypes identified through unsupervised clustering.
 */
enum class LifestyleArchetype {
    DEEP_RECOVERY,
    PEAK_PERFORMANCE,
    STRAIN_DEPLETED,
    SEDENTARY_MAINTENANCE
}

/**
 * Unsupervised clustering outcome for longitudinal lifestyle vectors.
 *
 * @param archetype The dominant archetype across the analyzed window.
 * @param dominantPercentage Percentage (0.0 to 100.0) of days classified into the dominant archetype.
 * @param dailyAssignments Mapping of each day to its clustered lifestyle archetype.
 * @param clusterInsights Clinical and behavioral interpretations of the identified clusters.
 */
data class LifestyleClusterResult(
    val archetype: LifestyleArchetype,
    val dominantPercentage: Double,
    val dailyAssignments: Map<LocalDate, LifestyleArchetype>,
    val clusterInsights: List<String>
)
