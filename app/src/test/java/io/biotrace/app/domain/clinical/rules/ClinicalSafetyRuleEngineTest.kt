package io.biotrace.app.domain.clinical.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalSafetyRuleEngineTest {

    private val engine = ClinicalSafetyRuleEngine()

    // --- Resting Heart Rate (AHA/ACC) Boundary Tests ---

    @Test
    fun `evaluates resting heart rate bradycardia below 50 bpm`() {
        val evaluations = engine.evaluateVitals("RESTING_HEART_RATE", 48.0)
        assertEquals(1, evaluations.size)
        val eval = evaluations.first()
        assertEquals(ClinicalStatus.BORDERLINE, eval.status)
        assertTrue(eval.narrative.contains("bradycardia", ignoreCase = true))
        assertEquals("50-85 bpm", eval.referenceRange)
    }

    @Test
    fun `evaluates resting heart rate normal range 50 to 85 bpm`() {
        val lowerBoundary = engine.evaluateVitals("RESTING_HEART_RATE", 50.0).first()
        assertEquals(ClinicalStatus.NORMAL, lowerBoundary.status)

        val mid = engine.evaluateVitals("RESTING_HEART_RATE", 68.0).first()
        assertEquals(ClinicalStatus.NORMAL, mid.status)

        val upperBoundary = engine.evaluateVitals("RESTING_HEART_RATE", 85.0).first()
        assertEquals(ClinicalStatus.NORMAL, upperBoundary.status)
    }

    @Test
    fun `evaluates resting heart rate elevated range 86 to 100 bpm`() {
        val eval86 = engine.evaluateVitals("RESTING_HEART_RATE", 86.0).first()
        assertEquals(ClinicalStatus.ELEVATED, eval86.status)

        val eval100 = engine.evaluateVitals("RESTING_HEART_RATE", 100.0).first()
        assertEquals(ClinicalStatus.ELEVATED, eval100.status)
    }

    @Test
    fun `evaluates resting heart rate tachycardia above 100 bpm`() {
        val eval = engine.evaluateVitals("RESTING_HEART_RATE", 105.0).first()
        assertEquals(ClinicalStatus.CRITICAL, eval.status)
        assertTrue(eval.narrative.contains("tachycardia", ignoreCase = true))
    }

    // --- Oxygen Saturation (SpO2) Boundary Tests ---

    @Test
    fun `evaluates oxygen saturation normal at or above 95 percent`() {
        val eval95 = engine.evaluateVitals("SPO2", 95.0).first()
        assertEquals(ClinicalStatus.NORMAL, eval95.status)

        val eval98 = engine.evaluateVitals("SPO2", 98.0).first()
        assertEquals(ClinicalStatus.NORMAL, eval98.status)
    }

    @Test
    fun `evaluates oxygen saturation mild hypoxemia between 90 and 94 percent`() {
        val eval90 = engine.evaluateVitals("SPO2", 90.0).first()
        assertEquals(ClinicalStatus.BORDERLINE, eval90.status)
        assertTrue(eval90.narrative.contains("mild hypoxemia", ignoreCase = true))

        val eval94 = engine.evaluateVitals("SPO2", 94.0).first()
        assertEquals(ClinicalStatus.BORDERLINE, eval94.status)
    }

    @Test
    fun `evaluates oxygen saturation critical alert below 90 percent`() {
        val eval89 = engine.evaluateVitals("SPO2", 89.0).first()
        assertEquals(ClinicalStatus.CRITICAL, eval89.status)
        assertTrue(eval89.narrative.contains("critical", ignoreCase = true))
    }

    // --- Fasting Glucose (ADA) Boundary Tests ---

    @Test
    fun `evaluates fasting glucose hypoglycemia below 70 mg dL`() {
        val eval = engine.evaluateBiomarker("FASTING_GLUCOSE", 62.0, "mg/dL").first()
        assertEquals(ClinicalStatus.CRITICAL, eval.status)
        assertTrue(eval.narrative.contains("hypoglycemia", ignoreCase = true))
    }

    @Test
    fun `evaluates fasting glucose normal between 70 and 99 mg dL`() {
        val eval70 = engine.evaluateBiomarker("FASTING_GLUCOSE", 70.0, "mg/dL").first()
        assertEquals(ClinicalStatus.NORMAL, eval70.status)

        val eval99 = engine.evaluateBiomarker("FASTING_GLUCOSE", 99.0, "mg/dL").first()
        assertEquals(ClinicalStatus.NORMAL, eval99.status)
    }

    @Test
    fun `evaluates fasting glucose pre-diabetes between 100 and 125 mg dL`() {
        val eval100 = engine.evaluateBiomarker("FASTING_GLUCOSE", 100.0, "mg/dL").first()
        assertEquals(ClinicalStatus.BORDERLINE, eval100.status)
        assertTrue(eval100.narrative.contains("pre-diabetes", ignoreCase = true))

        val eval125 = engine.evaluateBiomarker("FASTING_GLUCOSE", 125.0, "mg/dL").first()
        assertEquals(ClinicalStatus.BORDERLINE, eval125.status)
    }

    @Test
    fun `evaluates fasting glucose diabetes at or above 126 mg dL`() {
        val eval126 = engine.evaluateBiomarker("FASTING_GLUCOSE", 126.0, "mg/dL").first()
        assertEquals(ClinicalStatus.CRITICAL, eval126.status)
        assertTrue(eval126.narrative.contains("diabetes", ignoreCase = true))
    }

    @Test
    fun `evaluates fasting glucose with mmol per L unit conversion`() {
        // 5.0 mmol/L * 18.0182 ~= 90.1 mg/dL -> Normal
        val eval = engine.evaluateBiomarker("FASTING_GLUCOSE", 5.0, "mmol/L").first()
        assertEquals(ClinicalStatus.NORMAL, eval.status)

        // 7.2 mmol/L * 18.0182 ~= 129.7 mg/dL -> Diabetes
        val evalHigh = engine.evaluateBiomarker("FASTING_GLUCOSE", 7.2, "mmol/L").first()
        assertEquals(ClinicalStatus.CRITICAL, evalHigh.status)
    }

    // --- HbA1c (ADA) Boundary Tests ---

    @Test
    fun `evaluates HbA1c normal below 5 point 7 percent`() {
        val eval = engine.evaluateBiomarker("HBA1C", 5.4, "%").first()
        assertEquals(ClinicalStatus.NORMAL, eval.status)
    }

    @Test
    fun `evaluates HbA1c pre-diabetes between 5 point 7 and 6 point 4 percent`() {
        val eval57 = engine.evaluateBiomarker("HBA1C", 5.7, "%").first()
        assertEquals(ClinicalStatus.BORDERLINE, eval57.status)

        val eval64 = engine.evaluateBiomarker("HBA1C", 6.4, "%").first()
        assertEquals(ClinicalStatus.BORDERLINE, eval64.status)
    }

    @Test
    fun `evaluates HbA1c diabetes diagnostic threshold at or above 6 point 5 percent`() {
        val eval65 = engine.evaluateBiomarker("HBA1C", 6.5, "%").first()
        assertEquals(ClinicalStatus.CRITICAL, eval65.status)
        assertTrue(eval65.narrative.contains("diabetes", ignoreCase = true))
    }

    // --- Lipid Rules (NCEP ATP III) Boundary Tests ---

    @Test
    fun `evaluates fasting triglycerides across all 4 clinical tiers`() {
        // Normal < 150 mg/dL
        val normal = engine.evaluateBiomarker("TRIGLYCERIDES", 120.0, "mg/dL").first()
        assertEquals(ClinicalStatus.NORMAL, normal.status)

        // Borderline 150-199 mg/dL
        val borderline = engine.evaluateBiomarker("TRIGLYCERIDES", 175.0, "mg/dL").first()
        assertEquals(ClinicalStatus.BORDERLINE, borderline.status)

        // High 200-499 mg/dL
        val high = engine.evaluateBiomarker("TRIGLYCERIDES", 280.0, "mg/dL").first()
        assertEquals(ClinicalStatus.ELEVATED, high.status)

        // Severe >= 500 mg/dL
        val severe = engine.evaluateBiomarker("TRIGLYCERIDES", 520.0, "mg/dL").first()
        assertEquals(ClinicalStatus.CRITICAL, severe.status)
    }

    @Test
    fun `evaluates LDL cholesterol across all 5 clinical tiers`() {
        // Optimal < 100 mg/dL
        val optimal = engine.evaluateBiomarker("LDL", 85.0, "mg/dL").first()
        assertEquals(ClinicalStatus.OPTIMAL, optimal.status)

        // Near optimal 100-129 mg/dL
        val nearOptimal = engine.evaluateBiomarker("LDL", 115.0, "mg/dL").first()
        assertEquals(ClinicalStatus.NORMAL, nearOptimal.status)

        // Borderline 130-159 mg/dL
        val borderline = engine.evaluateBiomarker("LDL", 145.0, "mg/dL").first()
        assertEquals(ClinicalStatus.BORDERLINE, borderline.status)

        // High 160-189 mg/dL
        val high = engine.evaluateBiomarker("LDL", 175.0, "mg/dL").first()
        assertEquals(ClinicalStatus.ELEVATED, high.status)

        // Very High >= 190 mg/dL
        val veryHigh = engine.evaluateBiomarker("LDL", 205.0, "mg/dL").first()
        assertEquals(ClinicalStatus.CRITICAL, veryHigh.status)
    }

    @Test
    fun `evaluates HDL cholesterol sex-stratified risk and protective tiers`() {
        // Protective for both sexes (>= 60 mg/dL)
        val protectiveMale = engine.evaluateBiomarker("HDL", 65.0, "mg/dL", biologicalSex = "male").first()
        assertEquals(ClinicalStatus.OPTIMAL, protectiveMale.status)

        val protectiveFemale = engine.evaluateBiomarker("HDL", 65.0, "mg/dL", biologicalSex = "female").first()
        assertEquals(ClinicalStatus.OPTIMAL, protectiveFemale.status)

        // Male low risk (< 40 mg/dL) vs acceptable (40-59 mg/dL)
        val maleLow = engine.evaluateBiomarker("HDL", 35.0, "mg/dL", biologicalSex = "male").first()
        assertEquals(ClinicalStatus.BORDERLINE, maleLow.status)

        val maleAcceptable = engine.evaluateBiomarker("HDL", 45.0, "mg/dL", biologicalSex = "male").first()
        assertEquals(ClinicalStatus.NORMAL, maleAcceptable.status)

        // Female low risk (< 50 mg/dL) vs acceptable (50-59 mg/dL)
        val femaleLow = engine.evaluateBiomarker("HDL", 45.0, "mg/dL", biologicalSex = "female").first()
        assertEquals(ClinicalStatus.BORDERLINE, femaleLow.status)

        val femaleAcceptable = engine.evaluateBiomarker("HDL", 55.0, "mg/dL", biologicalSex = "female").first()
        assertEquals(ClinicalStatus.NORMAL, femaleAcceptable.status)
    }

    // --- Hemoglobin Anemia Rule Boundary Tests ---

    @Test
    fun `evaluates hemoglobin sex-stratified anemia and polycythemia`() {
        // Male: cutoff 13.5 g/dL
        val maleAnemia = engine.evaluateBiomarker("HEMOGLOBIN", 12.5, "g/dL", biologicalSex = "male").first()
        assertEquals(ClinicalStatus.BORDERLINE, maleAnemia.status)

        val maleNormal = engine.evaluateBiomarker("HEMOGLOBIN", 15.0, "g/dL", biologicalSex = "male").first()
        assertEquals(ClinicalStatus.NORMAL, maleNormal.status)

        // Female: cutoff 12.0 g/dL
        val femaleAnemia = engine.evaluateBiomarker("HEMOGLOBIN", 11.2, "g/dL", biologicalSex = "female").first()
        assertEquals(ClinicalStatus.BORDERLINE, femaleAnemia.status)

        val femaleNormal = engine.evaluateBiomarker("HEMOGLOBIN", 13.0, "g/dL", biologicalSex = "female").first()
        assertEquals(ClinicalStatus.NORMAL, femaleNormal.status)

        // Severe anemia (< 8.0 g/dL)
        val severe = engine.evaluateBiomarker("HEMOGLOBIN", 7.2, "g/dL", biologicalSex = "female").first()
        assertEquals(ClinicalStatus.CRITICAL, severe.status)
    }

    @Test
    fun `returns empty list for unrecognized vitals or biomarkers`() {
        assertTrue(engine.evaluateVitals("UNKNOWN_METRIC", 100.0).isEmpty())
        assertTrue(engine.evaluateBiomarker("NON_EXISTENT_BIOMARKER", 42.0, "mg/dL").isEmpty())
    }
}
