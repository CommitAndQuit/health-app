package io.biotrace.app.domain.longevity

/**
 * Longevity tier classification based on biological age delta relative to chronological baseline.
 */
enum class LongevityTier(
    val title: String,
    val subtitle: String,
    val colorHex: Long
) {
    OPTIMAL(
        title = "OPTIMAL REJUVENATION",
        subtitle = "Aging slower than demographic cohort",
        colorHex = 0xFF10B981
    ),
    BALANCED(
        title = "BALANCED PACE",
        subtitle = "Biological aging aligned with chronological baseline",
        colorHex = 0xFF3B82F6
    ),
    MILD_ACCELERATION(
        title = "MILD ACCELERATION",
        subtitle = "Actionable lifestyle levers detected",
        colorHex = 0xFFF59E0B
    ),
    ELEVATED_STRAIN(
        title = "ELEVATED STRAIN",
        subtitle = "Cardiovascular or metabolic recovery needed",
        colorHex = 0xFFEF4444
    )
}

/**
 * Individual physiological or clinical factor influencing biological age.
 */
data class LongevityDriver(
    val title: String,
    val impactYears: Double,
    val biomarkerValue: String,
    val insight: String,
    val isBonus: Boolean
)

/**
 * Comprehensive biological age calculation output.
 */
data class BiologicalAgeResult(
    val chronologicalAge: Int,
    val biologicalAge: Double,
    val ageDelta: Double,
    val agingPace: Double,
    val longevityTier: LongevityTier,
    val confidencePercentage: Int,
    val drivers: List<LongevityDriver>,
    val primaryRecommendation: String
)
