package io.biotrace.app.domain.clinical

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class NormalizedBiomarkerResult(
    val canonicalName: String,
    val displayName: String,
    val value: Double,
    val unit: String,
    val referenceMin: Double?,
    val referenceMax: Double?,
    val flag: String, // NORMAL, HIGH, LOW
    val operator: String? = null // "<", ">", "<=", ">="
)

@Singleton
class ClinicalSchemaNormalizer @Inject constructor() {

    data class CanonicalDefinition(
        val canonicalName: String,
        val displayName: String,
        val canonicalUnit: String,
        val defaultMin: Double?,
        val defaultMax: Double?
    )

    private val aliasMap: Map<String, String> = mapOf(
        // Glycemic Panel
        "hba1c" to "HBA1C", "hemoglobin a1c" to "HBA1C", "glycated hemoglobin" to "HBA1C", "a1c" to "HBA1C", "glycohemoglobin" to "HBA1C",
        "glucose" to "FASTING_GLUCOSE", "fasting glucose" to "FASTING_GLUCOSE", "fbs" to "FASTING_GLUCOSE", "fasting blood sugar" to "FASTING_GLUCOSE", "blood glucose" to "FASTING_GLUCOSE", "serum glucose" to "FASTING_GLUCOSE",
        "postprandial glucose" to "PP_GLUCOSE", "ppbs" to "PP_GLUCOSE", "post glucose" to "PP_GLUCOSE",
        "insulin" to "FASTING_INSULIN", "fasting insulin" to "FASTING_INSULIN", "serum insulin" to "FASTING_INSULIN",
        "c-peptide" to "C_PEPTIDE", "c peptide" to "C_PEPTIDE",

        // Lipid Panel
        "total cholesterol" to "CHOLESTEROL_TOTAL", "cholesterol, total" to "CHOLESTEROL_TOTAL", "chol" to "CHOLESTEROL_TOTAL", "tc" to "CHOLESTEROL_TOTAL", "serum cholesterol" to "CHOLESTEROL_TOTAL",
        "hdl" to "HDL", "hdl-c" to "HDL", "hdl cholesterol" to "HDL", "high density lipoprotein" to "HDL",
        "ldl" to "LDL", "ldl-c" to "LDL", "ldl cholesterol" to "LDL", "low density lipoprotein" to "LDL", "direct ldl" to "LDL",
        "triglycerides" to "TRIGLYCERIDES", "tg" to "TRIGLYCERIDES", "trigs" to "TRIGLYCERIDES",
        "vldl" to "VLDL", "vldl-c" to "VLDL", "very low density lipoprotein" to "VLDL",
        "non-hdl cholesterol" to "NON_HDL_CHOLESTEROL", "non hdl" to "NON_HDL_CHOLESTEROL",
        "apolipoprotein b" to "APOB", "apo-b" to "APOB", "apob" to "APOB",
        "apolipoprotein a1" to "APOA1", "apo-a1" to "APOA1",
        "lipoprotein(a)" to "LIPOPROTEIN_A", "lp(a)" to "LIPOPROTEIN_A",

        // Iron & Red Blood Cell
        "ferritin" to "FERRITIN", "serum ferritin" to "FERRITIN",
        "iron" to "SERUM_IRON", "serum iron" to "SERUM_IRON", "fe" to "SERUM_IRON",
        "tibc" to "TIBC", "total iron binding capacity" to "TIBC",
        "transferrin" to "TRANSFERRIN", "transferrin saturation" to "TRANSFERRIN_SATURATION", "tsat" to "TRANSFERRIN_SATURATION",
        "hemoglobin" to "HEMOGLOBIN", "hb" to "HEMOGLOBIN", "hgb" to "HEMOGLOBIN",
        "hematocrit" to "HEMATOCRIT", "hct" to "HEMATOCRIT", "packed cell volume" to "HEMATOCRIT", "pcv" to "HEMATOCRIT",
        "rbc" to "RBC", "red blood cell count" to "RBC", "erythrocytes" to "RBC",
        "mcv" to "MCV", "mean corpuscular volume" to "MCV",
        "mch" to "MCH", "mean corpuscular hemoglobin" to "MCH",
        "mchc" to "MCHC", "mean corpuscular hemoglobin concentration" to "MCHC",
        "rdw" to "RDW", "red cell distribution width" to "RDW",

        // White Blood Cells & Platelets
        "wbc" to "WBC", "white blood cell count" to "WBC", "leukocytes" to "WBC", "total leukocyte count" to "WBC", "tlc" to "WBC",
        "platelets" to "PLATELETS", "platelet count" to "PLATELETS", "thrombocytes" to "PLATELETS", "plt" to "PLATELETS",
        "neutrophils" to "NEUTROPHILS", "neutrophil percentage" to "NEUTROPHILS", "segs" to "NEUTROPHILS",
        "lymphocytes" to "LYMPHOCYTES", "lymphocyte percentage" to "LYMPHOCYTES", "lymphs" to "LYMPHOCYTES",
        "monocytes" to "MONOCYTES", "monocyte percentage" to "MONOCYTES",
        "eosinophils" to "EOSINOPHILS", "eosinophil percentage" to "EOSINOPHILS", "eos" to "EOSINOPHILS",
        "basophils" to "BASOPHILS", "basophil percentage" to "BASOPHILS",

        // Renal & Metabolic Panel
        "creatinine" to "CREATININE", "serum creatinine" to "CREATININE", "creat" to "CREATININE",
        "blood urea nitrogen" to "BUN", "bun" to "BUN", "urea" to "UREA", "blood urea" to "UREA",
        "egfr" to "EGFR", "estimated gfr" to "EGFR", "glomerular filtration rate" to "EGFR",
        "uric acid" to "URIC_ACID", "serum uric acid" to "URIC_ACID",
        "sodium" to "SODIUM", "na" to "SODIUM", "serum sodium" to "SODIUM",
        "potassium" to "POTASSIUM", "k" to "POTASSIUM", "serum potassium" to "POTASSIUM",
        "chloride" to "CHLORIDE", "cl" to "CHLORIDE",
        "calcium" to "CALCIUM", "ca" to "CALCIUM", "total calcium" to "CALCIUM",
        "magnesium" to "MAGNESIUM", "mg" to "MAGNESIUM",
        "phosphorus" to "PHOSPHORUS", "phosphate" to "PHOSPHORUS",

        // Liver Function Panel
        "alt" to "ALT", "sgpt" to "ALT", "alanine aminotransferase" to "ALT",
        "ast" to "AST", "sgot" to "AST", "aspartate aminotransferase" to "AST",
        "alkaline phosphatase" to "ALP", "alk phos" to "ALP", "alp" to "ALP",
        "bilirubin, total" to "BILIRUBIN_TOTAL", "total bilirubin" to "BILIRUBIN_TOTAL", "t.bili" to "BILIRUBIN_TOTAL",
        "direct bilirubin" to "BILIRUBIN_DIRECT", "conjugated bilirubin" to "BILIRUBIN_DIRECT",
        "albumin" to "ALBUMIN", "serum albumin" to "ALBUMIN",
        "total protein" to "TOTAL_PROTEIN", "protein, total" to "TOTAL_PROTEIN",
        "ggt" to "GGT", "gamma gt" to "GGT", "gamma glutamyl transferase" to "GGT",

        // Thyroid & Endocrine
        "tsh" to "TSH", "thyroid stimulating hormone" to "TSH", "tsh ultra" to "TSH", "sensitive tsh" to "TSH",
        "free t3" to "FREE_T3", "ft3" to "FREE_T3", "triiodothyronine, free" to "FREE_T3",
        "free t4" to "FREE_T4", "ft4" to "FREE_T4", "thyroxine, free" to "FREE_T4",
        "total t3" to "TOTAL_T3", "total t4" to "TOTAL_T4",
        "testosterone" to "TESTOSTERONE_TOTAL", "total testosterone" to "TESTOSTERONE_TOTAL", "serum testosterone" to "TESTOSTERONE_TOTAL",
        "free testosterone" to "TESTOSTERONE_FREE",
        "estradiol" to "ESTRADIOL", "e2" to "ESTRADIOL",
        "progesterone" to "PROGESTERONE",
        "cortisol" to "CORTISOL", "morning cortisol" to "CORTISOL",
        "psa" to "PSA", "total psa" to "PSA", "prostate specific antigen" to "PSA",

        // Vitamins & Inflammatory Markers
        // Cross-lab normalization for Vitamin D across various regional and global diagnostic centers
        "vitamin d" to "VITAMIN_D", "25-hydroxy vitamin d" to "VITAMIN_D", "25 hydroxy vitamin d" to "VITAMIN_D",
        "25-hydroxyvitamin d" to "VITAMIN_D", "25 hydroxyvitamin d" to "VITAMIN_D",
        "25-oh vitamin d" to "VITAMIN_D", "25 oh vitamin d" to "VITAMIN_D", "25-oh vit d" to "VITAMIN_D", "25 oh vit d" to "VITAMIN_D",
        "25(oh)d" to "VITAMIN_D", "25 (oh) d" to "VITAMIN_D", "25-oh-d" to "VITAMIN_D",
        "total 25-hydroxy vitamin d" to "VITAMIN_D", "total 25 hydroxy vitamin d" to "VITAMIN_D",
        "total 25-oh vitamin d" to "VITAMIN_D", "total 25 oh vitamin d" to "VITAMIN_D", "total 25-oh vit d" to "VITAMIN_D",
        "total vitamin d" to "VITAMIN_D", "vitamin d total" to "VITAMIN_D", "vit d total" to "VITAMIN_D",
        "vitamin d3" to "VITAMIN_D", "vit d3" to "VITAMIN_D", "vit d" to "VITAMIN_D", "d3" to "VITAMIN_D",
        "calcidiol" to "VITAMIN_D", "calcifediol" to "VITAMIN_D",
        "cholecalciferol" to "VITAMIN_D", "25-hydroxycholecalciferol" to "VITAMIN_D", "25 hydroxycholecalciferol" to "VITAMIN_D",
        "vitamin d, 25-hydroxy" to "VITAMIN_D", "vitamin d (25-oh)" to "VITAMIN_D", "vitamin d 25 oh" to "VITAMIN_D",
        "25 oh d" to "VITAMIN_D", "25-oh vitamin d (total)" to "VITAMIN_D", "vitamin d (25-hydroxy)" to "VITAMIN_D",

        "vitamin b12" to "VITAMIN_B12", "vit b12" to "VITAMIN_B12", "b12" to "VITAMIN_B12", "cobalamin" to "VITAMIN_B12",
        "cyanocobalamin" to "VITAMIN_B12", "serum vitamin b12" to "VITAMIN_B12", "serum b12" to "VITAMIN_B12",
        "folate" to "FOLATE", "folic acid" to "FOLATE", "serum folate" to "FOLATE",
        "crp" to "CRP", "c-reactive protein" to "CRP", "hs-crp" to "HS_CRP", "high sensitivity crp" to "HS_CRP",
        "high sensitivity c-reactive protein" to "HS_CRP",
        "esr" to "ESR", "erythrocyte sedimentation rate" to "ESR",
        "homocysteine" to "HOMOCYSTEINE"
    )

    private val canonicalDefinitions: Map<String, CanonicalDefinition> = mapOf(
        "HBA1C" to CanonicalDefinition("HBA1C", "Hemoglobin A1c", "%", 4.0, 5.6),
        "FASTING_GLUCOSE" to CanonicalDefinition("FASTING_GLUCOSE", "Fasting Glucose", "mg/dL", 70.0, 99.0),
        "PP_GLUCOSE" to CanonicalDefinition("PP_GLUCOSE", "Postprandial Glucose", "mg/dL", 70.0, 140.0),
        "FASTING_INSULIN" to CanonicalDefinition("FASTING_INSULIN", "Fasting Insulin", "μIU/mL", 2.6, 24.9),
        "CHOLESTEROL_TOTAL" to CanonicalDefinition("CHOLESTEROL_TOTAL", "Total Cholesterol", "mg/dL", 125.0, 200.0),
        "HDL" to CanonicalDefinition("HDL", "HDL Cholesterol", "mg/dL", 40.0, 60.0),
        "LDL" to CanonicalDefinition("LDL", "LDL Cholesterol", "mg/dL", 0.0, 100.0),
        "TRIGLYCERIDES" to CanonicalDefinition("TRIGLYCERIDES", "Triglycerides", "mg/dL", 0.0, 150.0),
        "FERRITIN" to CanonicalDefinition("FERRITIN", "Ferritin", "ng/mL", 30.0, 300.0),
        "SERUM_IRON" to CanonicalDefinition("SERUM_IRON", "Serum Iron", "μg/dL", 60.0, 170.0),
        "HEMOGLOBIN" to CanonicalDefinition("HEMOGLOBIN", "Hemoglobin", "g/dL", 13.5, 17.5),
        "HEMATOCRIT" to CanonicalDefinition("HEMATOCRIT", "Hematocrit", "%", 38.0, 50.0),
        "RBC" to CanonicalDefinition("RBC", "Red Blood Cells", "10^6/μL", 4.3, 5.9),
        "WBC" to CanonicalDefinition("WBC", "White Blood Cells", "10^3/μL", 4.0, 11.0),
        "PLATELETS" to CanonicalDefinition("PLATELETS", "Platelets", "10^3/μL", 150.0, 450.0),
        "CREATININE" to CanonicalDefinition("CREATININE", "Creatinine", "mg/dL", 0.6, 1.2),
        "BUN" to CanonicalDefinition("BUN", "Blood Urea Nitrogen", "mg/dL", 7.0, 20.0),
        "EGFR" to CanonicalDefinition("EGFR", "eGFR", "mL/min/1.73m²", 60.0, null),
        "URIC_ACID" to CanonicalDefinition("URIC_ACID", "Uric Acid", "mg/dL", 3.5, 7.2),
        "ALT" to CanonicalDefinition("ALT", "ALT (SGPT)", "U/L", 7.0, 55.0),
        "AST" to CanonicalDefinition("AST", "AST (SGOT)", "U/L", 8.0, 48.0),
        "ALP" to CanonicalDefinition("ALP", "Alkaline Phosphatase", "U/L", 44.0, 147.0),
        "BILIRUBIN_TOTAL" to CanonicalDefinition("BILIRUBIN_TOTAL", "Total Bilirubin", "mg/dL", 0.1, 1.2),
        "ALBUMIN" to CanonicalDefinition("ALBUMIN", "Albumin", "g/dL", 3.4, 5.4),
        "TSH" to CanonicalDefinition("TSH", "TSH", "μIU/mL", 0.45, 4.5),
        "FREE_T3" to CanonicalDefinition("FREE_T3", "Free T3", "pg/mL", 2.0, 4.4),
        "FREE_T4" to CanonicalDefinition("FREE_T4", "Free T4", "ng/dL", 0.82, 1.77),
        "VITAMIN_D" to CanonicalDefinition("VITAMIN_D", "25-OH Vitamin D", "ng/mL", 30.0, 100.0),
        "VITAMIN_B12" to CanonicalDefinition("VITAMIN_B12", "Vitamin B12", "pg/mL", 200.0, 900.0),
        "HS_CRP" to CanonicalDefinition("HS_CRP", "hs-CRP", "mg/L", 0.0, 1.0),
        "CRP" to CanonicalDefinition("CRP", "C-Reactive Protein", "mg/L", 0.0, 3.0),
        "ESR" to CanonicalDefinition("ESR", "ESR", "mm/hr", 0.0, 20.0),
        "TESTOSTERONE_TOTAL" to CanonicalDefinition("TESTOSTERONE_TOTAL", "Total Testosterone", "ng/dL", 300.0, 1000.0)
    )

    private val sortedAliases = aliasMap.entries.sortedByDescending { it.key.length }

    fun normalize(
        rawName: String,
        rawValueStr: String,
        rawUnit: String?,
        userRefMin: Double? = null,
        userRefMax: Double? = null
    ): NormalizedBiomarkerResult {
        val cleanName = rawName.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s()\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val withoutParens = cleanName.replace(Regex("\\(.*?\\)"), "").replace(Regex("\\s+"), " ").trim()
        val insideParens = Regex("\\((.*?)\\)").find(cleanName)?.groupValues?.get(1)?.trim() ?: ""

        val canonicalKey = aliasMap[cleanName]
            ?: aliasMap[withoutParens]
            ?: aliasMap[insideParens]
            ?: aliasMap[cleanName.replace("-", " ")]
            ?: sortedAliases.firstOrNull { cleanName.contains(it.key) || withoutParens.contains(it.key) || insideParens.contains(it.key) }?.value
            ?: cleanName.uppercase(Locale.ROOT).replace(" ", "_")

        val definition = canonicalDefinitions[canonicalKey]
        val displayName = definition?.displayName ?: rawName.trim()
        val canonicalUnit = definition?.canonicalUnit ?: rawUnit?.trim() ?: ""

        // Parse numerical value and potential operators like "< 0.01" or "> 100"
        val trimmedVal = rawValueStr.trim()
        var operator: String? = null
        var numericVal = 0.0

        val operatorRegex = Regex("^(<|<=|>|>=)\\s*([0-9.]+)")
        val match = operatorRegex.find(trimmedVal)
        if (match != null) {
            operator = match.groupValues[1]
            numericVal = match.groupValues[2].toDoubleOrNull() ?: 0.0
        } else {
            numericVal = Regex("[0-9.]+").find(trimmedVal)?.value?.toDoubleOrNull() ?: 0.0
        }

        // Unit conversion
        val (convertedVal, finalUnit) = convertUnits(canonicalKey, numericVal, rawUnit, canonicalUnit)

        // Reference range resolution
        val min = userRefMin ?: definition?.defaultMin
        val max = userRefMax ?: definition?.defaultMax

        val flag = when {
            max != null && convertedVal > max -> "HIGH"
            min != null && convertedVal < min -> "LOW"
            operator == ">" && max != null && convertedVal >= max -> "HIGH"
            operator == "<" && min != null && convertedVal <= min -> "LOW"
            else -> "NORMAL"
        }

        return NormalizedBiomarkerResult(
            canonicalName = canonicalKey,
            displayName = displayName,
            value = (convertedVal * 100).toInt() / 100.0,
            unit = finalUnit,
            referenceMin = min,
            referenceMax = max,
            flag = flag,
            operator = operator
        )
    }

    private fun convertUnits(
        canonicalName: String,
        value: Double,
        rawUnit: String?,
        targetUnit: String
    ): Pair<Double, String> {
        val u = rawUnit?.trim()?.lowercase(Locale.ROOT)?.replace(" ", "") ?: return value to targetUnit

        return when (canonicalName) {
            "FASTING_GLUCOSE", "PP_GLUCOSE" -> {
                if (u == "mmol/l") Pair(value * 18.0182, "mg/dL")
                else Pair(value, "mg/dL")
            }
            "CHOLESTEROL_TOTAL", "HDL", "LDL" -> {
                if (u == "mmol/l") Pair(value * 38.67, "mg/dL")
                else Pair(value, "mg/dL")
            }
            "TRIGLYCERIDES" -> {
                if (u == "mmol/l") Pair(value * 88.57, "mg/dL")
                else Pair(value, "mg/dL")
            }
            "VITAMIN_D" -> {
                when (u) {
                    "nmol/l" -> Pair(value / 2.496, "ng/mL")
                    "ug/l", "mcg/l", "μg/l" -> Pair(value, "ng/mL")
                    "ug/dl", "mcg/dl", "μg/dl" -> Pair(value * 10.0, "ng/mL")
                    "pmol/l" -> Pair(value / 2496.0, "ng/mL")
                    else -> Pair(value, "ng/mL")
                }
            }
            "VITAMIN_B12" -> {
                when (u) {
                    "pmol/l" -> Pair(value * 1.355, "pg/mL")
                    "ng/l" -> Pair(value, "pg/mL")
                    else -> Pair(value, "pg/mL")
                }
            }
            "HEMOGLOBIN" -> {
                if (u == "mmol/l") Pair(value * 1.6114, "g/dL")
                else Pair(value, "g/dL")
            }
            "CREATININE" -> {
                if (u == "μmol/l" || u == "umol/l") Pair(value / 88.4, "mg/dL")
                else Pair(value, "mg/dL")
            }
            "BILIRUBIN_TOTAL", "BILIRUBIN_DIRECT" -> {
                if (u == "μmol/l" || u == "umol/l") Pair(value / 17.1, "mg/dL")
                else Pair(value, "mg/dL")
            }
            "URIC_ACID" -> {
                if (u == "μmol/l" || u == "umol/l") Pair(value / 59.48, "mg/dL")
                else Pair(value, "mg/dL")
            }
            "TESTOSTERONE_TOTAL" -> {
                if (u == "nmol/l") Pair(value * 28.84, "ng/dL")
                else Pair(value, "ng/dL")
            }
            "FERRITIN" -> {
                if (u == "μg/l" || u == "ug/l") Pair(value, "ng/mL")
                else Pair(value, "ng/mL")
            }
            "TSH" -> {
                if (u == "miu/l") Pair(value, "μIU/mL")
                else Pair(value, targetUnit.ifEmpty { "μIU/mL" })
            }
            else -> Pair(value, targetUnit.ifEmpty { rawUnit ?: "" })
        }
    }
}
