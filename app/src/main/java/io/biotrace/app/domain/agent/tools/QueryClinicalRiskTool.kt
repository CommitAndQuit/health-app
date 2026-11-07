package io.biotrace.app.domain.agent.tools

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import io.biotrace.app.domain.clinical.risk.AtherogenicRiskCalculator
import io.biotrace.app.domain.clinical.risk.HarmonizedMetabolicRiskCalculator
import io.biotrace.app.domain.clinical.risk.SleepDebtCalculator
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
class QueryClinicalRiskTool @Inject constructor(
    private val atherogenicRiskCalculator: AtherogenicRiskCalculator,
    private val harmonizedMetabolicRiskCalculator: HarmonizedMetabolicRiskCalculator,
    private val sleepDebtCalculator: SleepDebtCalculator,
    private val labBiomarkerDao: LabBiomarkerDao,
    private val vitalsRepository: VitalsRepository,
    private val userProfileStore: UserProfileStore
) : AgentTool {

    override val name: String = "query_clinical_risk_scores"
    override val description: String =
        "Query multi-system clinical risk assessments: Atherogenic Lipid Dyslipidemia (TG/HDL, Non-HDL), Harmonized Metabolic Syndrome risk (zero blood pressure dependency), and 14-day cumulative Sleep Debt with autonomic strain. Parameters: none required."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val citations = mutableListOf<Citation>()
            val biomarkers = labBiomarkerDao.getAll()

            // 1. Gather Lab Biomarkers
            val cholEntity = biomarkers.firstOrNull { bm ->
                val name = bm.biomarkerName.uppercase(Locale.ROOT)
                name == "CHOLESTEROL" || name == "TOTAL_CHOLESTEROL" || name == "TOTAL CHOLESTEROL"
            }
            val hdlEntity = biomarkers.firstOrNull { bm ->
                val name = bm.biomarkerName.uppercase(Locale.ROOT)
                name == "HDL" || name == "HDL_CHOLESTEROL" || name == "HDL CHOLESTEROL"
            }
            val tgEntity = biomarkers.firstOrNull { bm ->
                val name = bm.biomarkerName.uppercase(Locale.ROOT)
                name == "TRIGLYCERIDES" || name == "TG"
            }
            val glucoseEntity = biomarkers.firstOrNull { bm ->
                val name = bm.biomarkerName.uppercase(Locale.ROOT)
                name == "FASTING_GLUCOSE" || name == "GLUCOSE" || name == "BLOOD_GLUCOSE"
            }
            val hba1cEntity = biomarkers.firstOrNull { bm ->
                val name = bm.biomarkerName.uppercase(Locale.ROOT)
                name == "HBA1C" || name == "HEMOGLOBIN_A1C" || name == "A1C"
            }

            listOfNotNull(cholEntity, hdlEntity, tgEntity, glucoseEntity, hba1cEntity).forEach { bm ->
                citations.add(
                    Citation(
                        id = bm.id,
                        timestamp = bm.recordedAt,
                        value = bm.value,
                        metric = bm.biomarkerName,
                        baseline = "Ref: ${bm.referenceMin ?: "?"}-${bm.referenceMax ?: "?"} ${bm.unit}"
                    )
                )
            }

            // 2. Atherogenic Risk Calculation
            val totalChol = cholEntity?.value
            val hdl = hdlEntity?.value
            val tg = tgEntity?.value

            val atherogenicJson = if (totalChol != null && hdl != null && tg != null && hdl > 0.0) {
                val res = atherogenicRiskCalculator.calculate(
                    totalCholesterolMgDl = totalChol,
                    hdlMgDl = hdl,
                    triglyceridesMgDl = tg
                )
                JSONObject().apply {
                    put("overallTier", res.overallRiskTier.name)
                    put("tgHdlRatio", res.tgHdlRatio)
                    put("tgHdlTier", res.tgHdlTier.name)
                    put("nonHdlCholesterol", res.nonHdlCholesterol)
                    put("nonHdlTier", res.nonHdlTier.name)
                    put("tcHdlRatio", res.tcHdlRatio)
                    put("tcHdlTier", res.tcHdlTier.name)
                    put("narrative", res.narrative)
                }
            } else {
                JSONObject().apply {
                    put("status", "INSUFFICIENT_DATA")
                    put("message", "Atherogenic risk requires Total Cholesterol, HDL, and Triglycerides in local lab records.")
                }
            }

            // 3. Harmonized Metabolic Risk Calculation
            val isMale = userProfileStore.getBiologicalSex().equals("male", ignoreCase = true)

            val metabolicRes = harmonizedMetabolicRiskCalculator.calculate(
                fastingGlucoseMgDl = glucoseEntity?.value,
                hba1cPercent = hba1cEntity?.value,
                triglyceridesMgDl = tgEntity?.value,
                hdlMgDl = hdlEntity?.value,
                isMale = isMale,
                bmi = null
            )

            val metabolicJson = JSONObject().apply {
                put("riskLevel", metabolicRes.riskLevel.name)
                put("criteriaMetCount", metabolicRes.criteriaMetCount)
                put("guidance", metabolicRes.guidance)
                val criteriaArray = JSONArray()
                metabolicRes.criteriaDetails.forEach { cd ->
                    criteriaArray.put(
                        JSONObject().apply {
                            put("criterionName", cd.criterionName)
                            put("isMet", cd.isMet)
                            put("observedValue", cd.observedValue)
                            put("thresholdDescription", cd.thresholdDescription)
                        }
                    )
                }
                put("criteriaDetails", criteriaArray)
            }

            // 4. Cumulative Sleep Debt & Allostatic Strain
            val now = Instant.now()
            val thirtyDaysAgo = now.minus(Duration.ofDays(30))
            val sleepPoints = vitalsRepository.getVitalsBetween("SLEEP", thirtyDaysAgo, now)
            val hrvPoints = vitalsRepository.getVitalsBetween("HRV_RMSSD", thirtyDaysAgo, now).ifEmpty {
                vitalsRepository.getVitalsBetween("HRV", thirtyDaysAgo, now)
            }

            val zone = ZoneId.systemDefault()
            val dailySleepHours = sleepPoints
                .groupBy { it.timestamp.atZone(zone).toLocalDate() }
                .values
                .map { list -> list.sumOf { it.value } }

            val currentHrv = hrvPoints.lastOrNull()?.value
            val baselineHrv = if (hrvPoints.isNotEmpty()) hrvPoints.map { it.value }.average() else null

            val sleepDebtRes = sleepDebtCalculator.calculate(
                sleepDurationsHours = dailySleepHours,
                targetSleepHours = 8.0,
                currentHrvRmssd = currentHrv,
                baselineHrvRmssd = baselineHrv
            )

            sleepPoints.takeLast(3).forEach { pt ->
                citations.add(
                    Citation(
                        id = pt.id,
                        timestamp = pt.timestamp,
                        value = pt.value,
                        metric = pt.metricType,
                        baseline = "Sleep record (Health Connect)"
                    )
                )
            }

            val sleepDebtJson = JSONObject().apply {
                put("tier", sleepDebtRes.tier.name)
                put("cumulativeDebtHours", sleepDebtRes.cumulativeDebtHours)
                put("allostaticStrainScore", sleepDebtRes.allostaticAutonomicStrainScore)
                put("recoveryRecommendation", sleepDebtRes.recoveryRecommendation)
            }

            val rootJson = JSONObject().apply {
                put("atherogenicRisk", atherogenicJson)
                put("metabolicRisk", metabolicJson)
                put("sleepDebt", sleepDebtJson)
            }

            ToolResult(
                success = true,
                data = rootJson.toString(),
                citations = citations.distinctBy { it.id }
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Failed to query clinical risk scores: ${e.message}"
            )
        }
    }
}
