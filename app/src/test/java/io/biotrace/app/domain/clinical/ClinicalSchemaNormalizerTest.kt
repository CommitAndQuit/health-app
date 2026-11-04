package io.biotrace.app.domain.clinical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ClinicalSchemaNormalizerTest {

    private val normalizer = ClinicalSchemaNormalizer()

    @Test
    fun `normalizes common aliases to canonical keys`() {
        val hba1c = normalizer.normalize("Glycated Hemoglobin", "5.8", "%")
        assertEquals("HBA1C", hba1c.canonicalName)
        assertEquals("%", hba1c.unit)
        assertEquals("HIGH", hba1c.flag)

        val glucose = normalizer.normalize("FBS (Fasting Blood Sugar)", "92", "mg/dL")
        assertEquals("FASTING_GLUCOSE", glucose.canonicalName)
        assertEquals("NORMAL", glucose.flag)

        val chol = normalizer.normalize("Total Cholesterol", "225", "mg/dL")
        assertEquals("CHOLESTEROL_TOTAL", chol.canonicalName)
        assertEquals("HIGH", chol.flag)

        val ferritin = normalizer.normalize("Serum Ferritin Level", "18", "ng/mL")
        assertEquals("FERRITIN", ferritin.canonicalName)
        assertEquals("LOW", ferritin.flag)

        val tsh = normalizer.normalize("TSH Ultra", "2.4", "μIU/mL")
        assertEquals("TSH", tsh.canonicalName)
        assertEquals("NORMAL", tsh.flag)

        val vitd = normalizer.normalize("25-OH Vitamin D3", "45", "ng/mL")
        assertEquals("VITAMIN_D", vitd.canonicalName)
        assertEquals("NORMAL", vitd.flag)

        // Multiple lab wordings for Vitamin D
        val vitDVariants = listOf(
            "Vitamin D, 25-Hydroxy",
            "Total 25-Hydroxy Vitamin D",
            "25-OH Vitamin D (Total)",
            "Cholecalciferol",
            "Calcidiol",
            "25-Hydroxycholecalciferol",
            "Vit D3 Total",
            "25(OH)D",
            "Total Vitamin D"
        )
        for (variant in vitDVariants) {
            val result = normalizer.normalize(variant, "35.2", "ng/mL")
            assertEquals("Expected $variant to normalize to VITAMIN_D", "VITAMIN_D", result.canonicalName)
            assertEquals("ng/mL", result.unit)
        }
    }

    @Test
    fun `converts clinical units correctly`() {
        // Glucose mmol/L -> mg/dL (5.0 * 18.0182 = ~90.09)
        val glucose = normalizer.normalize("Blood Glucose", "5.0", "mmol/L")
        assertEquals("FASTING_GLUCOSE", glucose.canonicalName)
        assertEquals("mg/dL", glucose.unit)
        assertEquals(90.09, glucose.value, 0.1)

        // Cholesterol mmol/L -> mg/dL (5.2 * 38.67 = ~201.08)
        val chol = normalizer.normalize("Total Cholesterol", "5.2", "mmol/L")
        assertEquals("CHOLESTEROL_TOTAL", chol.canonicalName)
        assertEquals("mg/dL", chol.unit)
        assertEquals(201.08, chol.value, 0.5)

        // Vitamin D nmol/L -> ng/mL (75 / 2.496 = ~30.04)
        val vitD = normalizer.normalize("Vitamin D", "75", "nmol/L")
        assertEquals("VITAMIN_D", vitD.canonicalName)
        assertEquals("ng/mL", vitD.unit)
        assertEquals(30.04, vitD.value, 0.1)
    }

    @Test
    fun `retains interval comparison operators`() {
        val lowCrp = normalizer.normalize("hs-CRP", "< 0.3", "mg/L")
        assertEquals("HS_CRP", lowCrp.canonicalName)
        assertEquals("<", lowCrp.operator)
        assertEquals(0.3, lowCrp.value, 0.01)
        assertEquals("NORMAL", lowCrp.flag)

        val highTiter = normalizer.normalize("Ferritin", "> 400", "ng/mL")
        assertEquals(">", highTiter.operator)
        assertEquals(400.0, highTiter.value, 0.01)
        assertEquals("HIGH", highTiter.flag)
    }
}
