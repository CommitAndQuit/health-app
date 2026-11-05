package io.biotrace.app.testing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import io.biotrace.app.data.gadgetbridge.GadgetbridgeStressDataSource
import io.biotrace.app.data.llm.GeminiClient
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.clinical.ClinicalSchemaNormalizer
import io.biotrace.app.domain.clinical.NormalizedBiomarkerResult
import io.biotrace.app.domain.clinical.PdfReportProcessor
import io.biotrace.app.domain.security.KeystoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class LabHubTestReceiver : BroadcastReceiver() {

    @Inject lateinit var keystoreManager: KeystoreManager
    @Inject lateinit var pdfReportProcessor: PdfReportProcessor
    @Inject lateinit var geminiClient: GeminiClient
    @Inject lateinit var clinicalSchemaNormalizer: ClinicalSchemaNormalizer
    @Inject lateinit var labBiomarkerDao: LabBiomarkerDao
    @Inject lateinit var gadgetbridgeDataSource: GadgetbridgeStressDataSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "LabHubTestReceiver"
        const val ACTION_PROVISION_KEY = "io.biotrace.app.TEST_PROVISION_KEY"
        const val ACTION_PROCESS_PDF = "io.biotrace.app.TEST_PROCESS_PDF"
        const val ACTION_IMPORT_GADGETBRIDGE = "io.biotrace.app.TEST_IMPORT_GADGETBRIDGE"
        const val EXTRA_API_KEY = "api_key"
        const val EXTRA_PDF_PATH = "pdf_path"
        const val EXTRA_DB_PATH = "db_path"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Received broadcast action: $action")

        val pendingResult = goAsync()

        when (action) {
            ACTION_PROVISION_KEY -> {
                val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: ""
                scope.launch {
                    try {
                        if (apiKey.isNotBlank()) {
                            // Store securely in Android KeyStore
                            keystoreManager.storeApiKey("gemini", apiKey)
                            // Also persist in app internal files for fast recovery
                            val keyFile = File(context.filesDir, "gemini_api_key.txt")
                            keyFile.writeText(apiKey)

                            val statusFile = File(context.filesDir, "test_key_status.txt")
                            statusFile.writeText("SUCCESS: API key provisioned (length=${apiKey.length})")
                            Log.i(TAG, "Successfully provisioned Gemini API key into Keystore")
                        } else {
                            Log.e(TAG, "Empty API key received in broadcast")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error provisioning API key", e)
                        File(context.filesDir, "test_key_status.txt").writeText("ERROR: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_PROCESS_PDF -> {
                val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
                    ?: File(context.filesDir, "report.pdf").absolutePath
                val resultFile = File(context.filesDir, "test_pdf_result.json")

                scope.launch {
                    try {
                        val file = File(pdfPath)
                        Log.i(TAG, "Testing PDF processing for file: ${file.absolutePath} (exists=${file.exists()}, size=${file.length()})")

                        if (!file.exists()) {
                            val err = JSONObject().apply {
                                put("status", "ERROR")
                                put("error", "File not found: ${file.absolutePath}")
                            }
                            resultFile.writeText(err.toString(2))
                            return@launch
                        }

                        // 1. Process PDF Document
                        val doc = pdfReportProcessor.processFile(file)
                        Log.i(TAG, "Document processed: ${doc.fileName}, pages=${doc.pageCount}, bytes=${doc.byteSize}")

                        // 2. Invoke Gemini Multimodal Vision Document Analysis
                        val prompt = buildClinicalExtractionPrompt()
                        val responseJson = geminiClient.generateContentWithDocument(
                            prompt = prompt,
                            base64Data = doc.base64Data,
                            mimeType = "application/pdf"
                        )
                        Log.i(TAG, "Gemini document analysis response received (${responseJson.length} chars)")

                        // 3. Parse and Normalize Biomarkers
                        val parsedData = parseAndNormalize(responseJson)

                        // 4. Persist extracted biomarkers directly into local encrypted Room database
                        // Extract clinical report date or fallback to now
                        val reportInstant = io.biotrace.app.domain.clinical.ClinicalDateParser.parseToInstant(parsedData.reportDate)
                        val dateIso = io.biotrace.app.domain.clinical.ClinicalDateParser.toIsoDateString(reportInstant)
                        val entities = parsedData.biomarkers.map { b ->
                            LabBiomarkerEntity(
                                id = "lab_${b.canonicalName}_${dateIso}",
                                biomarkerName = b.canonicalName,
                                rawName = b.displayName,
                                value = b.value,
                                unit = b.unit,
                                referenceMin = b.referenceMin,
                                referenceMax = b.referenceMax,
                                flag = b.flag,
                                recordedAt = reportInstant,
                                source = parsedData.labName ?: "VISION_LLM"
                            )
                        }
                        if (entities.isNotEmpty()) {
                            labBiomarkerDao.deleteLegacyScanEntries()
                            labBiomarkerDao.insertAll(entities)
                            Log.i(TAG, "Persisted ${entities.size} biomarkers into Room SQLite with reportDate=$dateIso (LabBiomarkerDao)")
                        }

                        val output = JSONObject().apply {
                            put("status", "SUCCESS")
                            put("fileName", doc.fileName)
                            put("pageCount", doc.pageCount)
                            put("byteSize", doc.byteSize)
                            put("hasPreviewBitmap", doc.previewBitmap != null)
                            put("labName", parsedData.labName)
                            put("reportDate", parsedData.reportDate)
                            put("biomarkerCount", parsedData.biomarkers.size)

                            val markersArray = JSONArray()
                            parsedData.biomarkers.forEach { b ->
                                markersArray.put(JSONObject().apply {
                                    put("name", b.displayName)
                                    put("standardName", b.canonicalName)
                                    put("value", b.value)
                                    put("unit", b.unit)
                                    put("referenceLow", b.referenceMin ?: JSONObject.NULL)
                                    put("referenceHigh", b.referenceMax ?: JSONObject.NULL)
                                    put("isOutOfRange", b.flag != "NORMAL")
                                    put("flag", b.flag)
                                })
                            }
                            put("biomarkers", markersArray)
                        }

                        resultFile.writeText(output.toString(2))
                        Log.i(TAG, "Wrote test results to ${resultFile.absolutePath} with ${parsedData.biomarkers.size} biomarkers")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed PDF test processing", e)
                        val err = JSONObject().apply {
                            put("status", "ERROR")
                            put("error", e.localizedMessage ?: e.message ?: "Unknown error")
                            put("stackTrace", Log.getStackTraceString(e))
                        }
                        resultFile.writeText(err.toString(2))
                    } finally {
                        try {
                            pendingResult.finish()
                        } catch (_: Exception) {}
                    }
                }
            }

            ACTION_IMPORT_GADGETBRIDGE -> {
                val dbPath = intent.getStringExtra(EXTRA_DB_PATH) ?: "/sdcard/Download/Gadgetbridge.db"
                val resultFile = File(context.filesDir, "test_gadgetbridge_result.json")

                scope.launch {
                    try {
                        val file = File(dbPath)
                        Log.i(TAG, "Importing Gadgetbridge DB from: ${file.absolutePath} (exists=${file.exists()}, size=${file.length()})")
                        val result = gadgetbridgeDataSource.importFromDbFile(file)
                        result.fold(
                            onSuccess = { count ->
                                Log.i(TAG, "Successfully imported $count stress records via broadcast")
                                resultFile.writeText(JSONObject().apply {
                                    put("status", "SUCCESS")
                                    put("importedCount", count)
                                }.toString(2))
                            },
                            onFailure = { err ->
                                Log.e(TAG, "Gadgetbridge import failed", err)
                                resultFile.writeText(JSONObject().apply {
                                    put("status", "ERROR")
                                    put("error", err.message)
                                }.toString(2))
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling Gadgetbridge broadcast", e)
                    } finally {
                        try {
                            pendingResult.finish()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun buildClinicalExtractionPrompt(): String {
        return """
            You are BioTrace's clinical document OCR engine. Analyze this medical diagnostic laboratory report.
            Extract all diagnostic tests/biomarkers with their measured value, unit, and reference interval.
            Return a single JSON object with this exact schema:
            {
              "labName": "Diagnostic center or laboratory name",
              "reportDate": "YYYY-MM-DD or date from report",
              "biomarkers": [
                {
                  "testName": "Biomarker Name",
                  "value": 12.5,
                  "unit": "g/dL",
                  "referenceLow": 11.0,
                  "referenceHigh": 16.0
                }
              ]
            }
            Return ONLY the valid JSON object without markdown code blocks.
        """.trimIndent()
    }

    private data class ParsedReport(
        val labName: String?,
        val reportDate: String?,
        val biomarkers: List<NormalizedBiomarkerResult>
    )

    private fun parseAndNormalize(rawJson: String): ParsedReport {
        var clean = rawJson.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }

        val root = JSONObject(clean)
        val labName = root.optString("labName").takeIf { it.isNotBlank() }
        val reportDate = root.optString("reportDate").takeIf { it.isNotBlank() }

        val array = root.optJSONArray("biomarkers")
            ?: root.optJSONArray("results")
            ?: root.optJSONArray("tests")
            ?: JSONArray()

        val results = mutableListOf<NormalizedBiomarkerResult>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val testName = obj.optString("testName", obj.optString("name", "Unknown Marker"))
            val value = obj.optDouble("value", Double.NaN)
            if (value.isNaN()) continue

            val unit = obj.optString("unit", "")
            val refLow = if (obj.has("referenceLow") && !obj.isNull("referenceLow")) obj.optDouble("referenceLow") else null
            val refHigh = if (obj.has("referenceHigh") && !obj.isNull("referenceHigh")) obj.optDouble("referenceHigh") else null

            val norm = clinicalSchemaNormalizer.normalize(
                rawName = testName,
                rawValueStr = value.toString(),
                rawUnit = unit,
                userRefMin = refLow,
                userRefMax = refHigh
            )
            results.add(norm)
        }

        return ParsedReport(labName, reportDate, results)
    }
}
