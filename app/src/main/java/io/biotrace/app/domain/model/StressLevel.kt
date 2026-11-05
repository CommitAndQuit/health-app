package io.biotrace.app.domain.model

import androidx.compose.ui.graphics.Color
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.CobaltBlueSoftTint
import io.biotrace.app.ui.theme.CoralAlertPrimary
import io.biotrace.app.ui.theme.CoralAlertSoftTint
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.SolarAmberSoftTint

enum class StressLevel(
    val levelNumber: Int,
    val title: String,
    val rangeDescription: String,
    val color: Color,
    val backgroundColor: Color
) {
    RELAXED(1, "Relaxed", "1–29", EmeraldPrimary, EmeraldSoftTint),
    NORMAL(2, "Normal", "30–59", CobaltBluePrimary, CobaltBlueSoftTint),
    MEDIUM(3, "Medium", "60–79", SolarAmberPrimary, SolarAmberSoftTint),
    HIGH(4, "High", "80–99", CoralAlertPrimary, CoralAlertSoftTint);

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
