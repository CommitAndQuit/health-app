package io.biotrace.app.ui.insights

import io.biotrace.app.domain.anomaly.AnomalyAlert
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import io.biotrace.app.domain.anomaly.AnomalyRuleType
import io.biotrace.app.domain.clinical.rules.ClinicalEvaluation
import io.biotrace.app.domain.clinical.rules.ClinicalSafetyRuleEngine
import io.biotrace.app.domain.clinical.rules.ClinicalStatus
import io.biotrace.app.domain.insights.arbitration.InsightArbitrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class InsightArbitratorTest {

    private lateinit var arbitrator: InsightArbitrator
    private val ruleEngine = ClinicalSafetyRuleEngine()
    private val anomalyEngine = AnomalyDetectionEngine()

    @Before
    fun setUp() {
        arbitrator = InsightArbitrator(ruleEngine, anomalyEngine)
    }

    @Test
    fun `arbitrate returns raw report unmodified when only OPTIMAL or NORMAL evaluations exist`() {
        val evaluations = listOf(
            ClinicalEvaluation(
                ruleId = "rhr_rule",
                metricName = "Resting Heart Rate",
                status = ClinicalStatus.OPTIMAL,
                value = 58.0,
                unit = "bpm",
                referenceRange = "60-100 bpm",
                narrative = "Excellent resting tone",
                guidelineSource = "AHA"
            )
        )
        val alerts = listOf(
            AnomalyAlert(
                id = "alert_1",
                ruleType = AnomalyRuleType.PERSONAL_BASELINE_DEVIATION,
                metricName = "STEPS",
                currentValue = 9500.0,
                baselineValue = 9000.0,
                unit = "steps",
                severity = "INFO",
                narrative = "Slight step increase"
            )
        )

        val rawReport = "Your overall wellness is performing exceptionally well."

        val arbitrated = arbitrator.arbitrate(evaluations, alerts, rawReport)

        assertFalse(arbitrated.hasClinicalSafetyNotice)
        assertNull(arbitrated.safetyNotice)
        assertEquals(rawReport, arbitrated.finalContent)
        assertEquals(ClinicalStatus.OPTIMAL, arbitrated.highestClinicalStatus)
        assertTrue(arbitrated.citations.isEmpty())
    }

    @Test
    fun `arbitrate prepends Clinical Safety Notice when CRITICAL evaluation exists`() {
        val evaluations = listOf(
            ClinicalEvaluation(
                ruleId = "spo2_critical",
                metricName = "SPO2",
                status = ClinicalStatus.CRITICAL,
                value = 87.0,
                unit = "%",
                referenceRange = "≥ 95%",
                narrative = "Critical hypoxemia observed",
                guidelineSource = "WHO"
            )
        )

        val rawReport = "Continue light walking and staying hydrated."

        val arbitrated = arbitrator.arbitrate(evaluations, emptyList(), rawReport)

        assertTrue(arbitrated.hasClinicalSafetyNotice)
        assertNotNull(arbitrated.safetyNotice)
        assertEquals(ClinicalStatus.CRITICAL, arbitrated.highestClinicalStatus)
        assertEquals(1, arbitrated.criticalEvaluations.size)

        // Verifies medical priority over generative advice
        assertTrue(arbitrated.finalContent.startsWith("⚠️ CLINICAL SAFETY NOTICE"))
        assertTrue(arbitrated.finalContent.contains("CRITICAL CLINICAL FINDINGS"))
        assertTrue(arbitrated.finalContent.contains("WHO"))
        assertTrue(arbitrated.finalContent.contains("Continue light walking"))
        assertEquals(1, arbitrated.citations.size)
        assertEquals("spo2_critical", arbitrated.citations.first().id)
    }

    @Test
    fun `arbitrate prepends notice when ELEVATED evaluation exists`() {
        val evaluations = listOf(
            ClinicalEvaluation(
                ruleId = "glucose_elevated",
                metricName = "Fasting Glucose",
                status = ClinicalStatus.ELEVATED,
                value = 135.0,
                unit = "mg/dL",
                referenceRange = "< 100 mg/dL",
                narrative = "Fasting blood sugar in diabetic threshold",
                guidelineSource = "ADA"
            )
        )

        val rawReport = "Consider moderate complex carbohydrates."

        val arbitrated = arbitrator.arbitrate(evaluations, emptyList(), rawReport)

        assertTrue(arbitrated.hasClinicalSafetyNotice)
        assertEquals(ClinicalStatus.ELEVATED, arbitrated.highestClinicalStatus)
        assertTrue(arbitrated.finalContent.contains("ELEVATED CLINICAL RISKS"))
        assertTrue(arbitrated.finalContent.contains("ADA"))
        assertEquals(1, arbitrated.citations.size)
    }

    @Test
    fun `arbitrate prepends notice when high severity ALERT anomaly is detected`() {
        val alerts = listOf(
            AnomalyAlert(
                id = "compound_strain_1",
                ruleType = AnomalyRuleType.MULTIVARIATE_COMPOUND_STRAIN,
                metricName = "COMPOUND_AUTONOMIC_STRAIN",
                currentValue = 88.0,
                baselineValue = 65.0,
                unit = "bpm",
                severity = "ALERT",
                narrative = "Multivariate autonomic strain detected with elevated RHR and suppressed HRV",
                detectedAt = Instant.now()
            )
        )

        val rawReport = "Today is a great day for high intensity training."

        val arbitrated = arbitrator.arbitrate(emptyList(), alerts, rawReport)

        assertTrue(arbitrated.hasClinicalSafetyNotice)
        assertEquals(1, arbitrated.highSeverityAlerts.size)
        assertTrue(arbitrated.finalContent.contains("PHYSIOLOGICAL ANOMALIES"))
        assertTrue(arbitrated.finalContent.contains("ALERT"))
        assertTrue(arbitrated.finalContent.contains("Multivariate autonomic strain detected"))
        assertEquals(1, arbitrated.citations.size)
        assertEquals("compound_strain_1", arbitrated.citations.first().id)
    }

    @Test
    fun `arbitrate adheres strictly to AGENTS md rules with zero vendor references`() {
        val evaluations = listOf(
            ClinicalEvaluation(
                ruleId = "ldl_rule",
                metricName = "LDL",
                status = ClinicalStatus.ELEVATED,
                value = 170.0,
                unit = "mg/dL",
                referenceRange = "< 100 mg/dL",
                narrative = "Elevated atherogenic lipoprotein",
                guidelineSource = "ACC/AHA"
            )
        )

        val arbitrated = arbitrator.arbitrate(evaluations, emptyList(), "AI summary")

        val content = arbitrated.finalContent
        assertTrue(content.contains("Health Connect"))
        assertFalse(content.contains("Fitbit", ignoreCase = true))
        assertFalse(content.contains("Garmin", ignoreCase = true))
        assertFalse(content.contains("Huawei", ignoreCase = true))
        assertFalse(content.contains("Gadgetbridge", ignoreCase = true))
    }
}
