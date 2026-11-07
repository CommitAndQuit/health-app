package io.biotrace.app.domain.usecase

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.longevity.BiologicalAgeCalculator
import io.biotrace.app.domain.longevity.BiologicalAgeResult
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates physiological vitals, clinical lab biomarkers, and demographic chronological age
 * to compute phenotypic biological age, aging pace, and personalized longevity drivers.
 */
@Singleton
class GetBiologicalAgeUseCase @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val labBiomarkerDao: LabBiomarkerDao,
    private val userProfileStore: UserProfileStore,
    private val calculator: BiologicalAgeCalculator
) {

    suspend operator fun invoke(): BiologicalAgeResult = withContext(Dispatchers.IO) {
        val chronologicalAge = userProfileStore.getChronologicalAge()

        val vitalsSnapshot = runCatching { vitalsRepository.getVitalsDashboardSnapshot() }.getOrDefault(emptyList())

        // 1. Extract Resting Heart Rate (bpm)
        val rhrCard = vitalsSnapshot.firstOrNull { it.metricType == VitalMetricType.RESTING_HEART_RATE }
        val rhrFromCard = rhrCard?.primaryValue?.takeIf { it != "--" }?.let {
            Regex("""\d+""").find(it)?.value?.toIntOrNull()
        }

        // 2. Extract Sleep Duration (hours) and Sleep Score
        val sleepCard = vitalsSnapshot.firstOrNull { it.metricType == VitalMetricType.SLEEP }
        var sleepDurationHours: Double? = null
        var sleepScore: Int? = null

        // Check badgeText for explicit duration like "7h 48m", "8h", "450m"
        sleepCard?.badgeText?.takeIf { it != "--" }?.let { badge ->
            val hmRegex = Regex("""(?:(\d+(?:\.\d+)?)\s*h)?\s*(?:(\d+(?:\.\d+)?)\s*m)?""")
            val match = hmRegex.matchEntire(badge.trim())
            if (match != null && (match.groups[1] != null || match.groups[2] != null)) {
                val h = match.groups[1]?.value?.toDoubleOrNull() ?: 0.0
                val m = match.groups[2]?.value?.toDoubleOrNull() ?: 0.0
                if (h > 0.0 || m > 0.0) {
                    sleepDurationHours = h + (m / 60.0)
                }
            }
        }

        // Check primaryValue
        val primaryVal = sleepCard?.primaryValue?.takeIf { it != "--" }
        if (primaryVal != null) {
            val num = Regex("""(\d+(?:\.\d+)?)""").find(primaryVal)?.groupValues?.get(1)?.toDoubleOrNull()
            if (num != null) {
                val isScore = sleepCard.primaryUnit.contains("score", ignoreCase = true) ||
                    sleepCard.primaryUnit.contains("%") ||
                    num > 24.0
                if (isScore) {
                    sleepScore = num.toInt()
                } else if (sleepDurationHours == null) {
                    sleepDurationHours = num
                }
            }
        }

        // Check progress
        if (sleepDurationHours == null && sleepCard?.progress != null && sleepCard.progress > 0f) {
            sleepDurationHours = if (sleepCard.progress > 1.0f && sleepCard.progress <= 24.0f) {
                sleepCard.progress.toDouble()
            } else {
                (sleepCard.progress * 8.0).toDouble()
            }
        }

        // Fallback to ProfileTriptychSummary if resting HR or sleep metrics are missing
        val needsTriptych = rhrFromCard == null || sleepDurationHours == null || sleepScore == null
        val profileTriptych = if (needsTriptych) {
            runCatching { vitalsRepository.getProfileTriptychSummary() }.getOrNull()
        } else {
            null
        }

        val restingHrBpm = rhrFromCard ?: profileTriptych?.restingHr?.takeIf { it != "-- bpm" }?.let {
            Regex("""\d+""").find(it)?.value?.toIntOrNull()
        }

        val triptychScore = profileTriptych?.sleepScore?.takeIf { it != "--%" }?.let {
            Regex("""\d+""").find(it)?.value?.toIntOrNull()
        }
        if (sleepScore == null && triptychScore != null) {
            sleepScore = triptychScore
        }
        if (sleepDurationHours == null && sleepScore != null && sleepScore > 0) {
            sleepDurationHours = (sleepScore / 100.0) * 8.0
        }

        // 3. Extract Lab Biomarkers (ordered DESC by recorded_at; first entry per name is the latest)
        val biomarkers = runCatching { labBiomarkerDao.getAll() }.getOrDefault(emptyList())
        val biomarkerMap = mutableMapOf<String, Double>()
        for (biomarker in biomarkers) {
            val canonical = canonicalizeBiomarkerName(biomarker.biomarkerName)
            if (!biomarkerMap.containsKey(canonical)) {
                biomarkerMap[canonical] = biomarker.value
            }
            val rawUpper = biomarker.biomarkerName.trim().uppercase()
            if (!biomarkerMap.containsKey(rawUpper)) {
                biomarkerMap[rawUpper] = biomarker.value
            }
        }

        calculator.calculate(
            chronologicalAge = chronologicalAge,
            restingHrBpm = restingHrBpm,
            sleepDurationHours = sleepDurationHours,
            sleepScore = sleepScore,
            labBiomarkers = biomarkerMap
        )
    }

    private fun canonicalizeBiomarkerName(name: String): String {
        val trimmed = name.trim().uppercase()
        return when (trimmed) {
            "GLUCOSE", "FBS", "FASTING BLOOD SUGAR", "FASTING_GLUCOSE" -> "FASTING_GLUCOSE"
            "HS-CRP", "HS_CRP", "HIGH SENSITIVITY CRP", "HIGH_SENSITIVITY_CRP" -> "HS_CRP"
            "CRP", "C-REACTIVE PROTEIN", "C_REACTIVE_PROTEIN" -> "CRP"
            "HBA1C", "A1C", "GLYCATED HEMOGLOBIN" -> "HBA1C"
            "ALBUMIN", "SERUM_ALBUMIN", "SERUM ALBUMIN" -> "ALBUMIN"
            "TRIGLYCERIDES", "TG" -> "TRIGLYCERIDES"
            "HDL", "HDL-C", "HDL_CHOLESTEROL" -> "HDL"
            "CREATININE", "SERUM_CREATININE" -> "CREATININE"
            else -> trimmed
        }
    }
}
