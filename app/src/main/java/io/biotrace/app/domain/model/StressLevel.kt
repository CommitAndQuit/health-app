package io.biotrace.app.domain.model

import androidx.compose.ui.graphics.Color

enum class StressLevel(
    val levelNumber: Int,
    val title: String,
    val rangeDescription: String,
    val color: Color,
    val backgroundColor: Color
) {
    RELAXED(1, "Relaxed", "1–29", Color(0xFF0D9488), Color(0xFFCCFBF1)),
    NORMAL(2, "Normal", "30–59", Color(0xFF2563EB), Color(0xFFDBEAFE)),
    MEDIUM(3, "Medium", "60–79", Color(0xFFD97706), Color(0xFFFEF3C7)),
    HIGH(4, "High", "80–99", Color(0xFFDC2626), Color(0xFFFEE2E2));

    companion object {
        fun fromScore(score: Int): StressLevel {
            return when {
                score in 1..29 -> RELAXED
                score in 30..59 -> NORMAL
                score in 60..79 -> MEDIUM
                score >= 80 -> HIGH
                else -> NORMAL
            }
        }

        fun fromLevel(level: Int, score: Int = 0): StressLevel {
            return when (level) {
                1 -> RELAXED
                2 -> NORMAL
                3 -> MEDIUM
                4 -> HIGH
                else -> fromScore(score)
            }
        }
    }
}
