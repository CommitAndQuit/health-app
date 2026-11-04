package io.biotrace.app.domain.anomaly

import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class AnomalyDetectionEngineTest {

    private val engine = AnomalyDetectionEngine()

    @Test
    fun `Rule 1 flags out-of-range clinical biomarkers`() {
        val normal = LabBiomarkerEntity(
            id = "1",
            biomarkerName = "FERRITIN",
            rawName = "Ferritin",
            value = 65.0,
            unit = "ng/mL",
            referenceMin = 30.0,
            referenceMax = 300.0,
            flag = "NORMAL",
            recordedAt = Instant.now(),
            source = "VISION_LLM"
        )
        val lowAlerts = engine.evaluateBiomarker(
            normal.copy(value = 15.0, flag = "LOW")
        )
        assertEquals(1, lowAlerts.size)
        assertEquals(AnomalyRuleType.OUT_OF_RANGE_CLINICAL, lowAlerts[0].ruleType)
        assertTrue(lowAlerts[0].narrative.contains("sits below clinical lower limit"))

        val highAlerts = engine.evaluateBiomarker(
            normal.copy(value = 450.0, flag = "HIGH")
        )
        assertEquals(1, highAlerts.size)
        assertEquals(AnomalyRuleType.OUT_OF_RANGE_CLINICAL, highAlerts[0].ruleType)
        assertTrue(highAlerts[0].narrative.contains("exceeds clinical upper limit"))
    }

    @Test
    fun `Rule 2 flags longitudinal shift over historical records`() {
        val now = Instant.now()
        val prior = LabBiomarkerEntity(
            id = "prior_1",
            biomarkerName = "HBA1C",
            rawName = "HbA1c",
            value = 5.2,
            unit = "%",
            referenceMin = 4.0,
            referenceMax = 5.6,
            flag = "NORMAL",
            recordedAt = now.minus(90, ChronoUnit.DAYS),
            source = "VISION_LLM"
        )
        val current = prior.copy(
            id = "curr_2",
            value = 6.4,
            recordedAt = now
        )

        val alerts = engine.evaluateBiomarker(current, listOf(prior))
        // Should flag Rule 1 (out of range) and Rule 2 (shift >= 20%)
        assertTrue(alerts.any { it.ruleType == AnomalyRuleType.LONGITUDINAL_TREND_SHIFT })
    }

    @Test
    fun `Rule 3 flags personal baseline median deviation for vitals`() {
        val now = Instant.now()
        val historicalRecords = (1..30).map { i ->
            VitalsRecordEntity(
                id = "v_$i",
                metricType = "HEART_RATE",
                value = 65.0,
                unit = "bpm",
                timestamp = now.minus(i.toLong(), ChronoUnit.DAYS),
                source = "HEALTH_CONNECT",
                createdAt = now
            )
        }

        // Test with current resting HR elevated to 82 bpm (> 25% elevation)
        val alerts = engine.evaluateVitals(
            metricType = "HEART_RATE",
            currentValue = 82.0,
            unit = "bpm",
            recent30DaysRecords = historicalRecords
        )

        assertEquals(1, alerts.size)
        assertEquals(AnomalyRuleType.PERSONAL_BASELINE_DEVIATION, alerts[0].ruleType)
        assertTrue(alerts[0].narrative.contains("15%+ elevation"))
    }
}
