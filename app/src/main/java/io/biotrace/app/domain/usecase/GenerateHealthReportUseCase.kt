package io.biotrace.app.domain.usecase

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

sealed class ReportChunk {
    data object Started : ReportChunk()
    data class Text(val text: String) : ReportChunk()
    data object Done : ReportChunk()
    data class Error(val message: String) : ReportChunk()
}

@Singleton
class GenerateHealthReportUseCase @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val labBiomarkerDao: LabBiomarkerDao,
    private val llmClient: LlmClient,
) {
    fun execute(): Flow<ReportChunk> = flow {
        if (!llmClient.isConfigured()) {
            emit(ReportChunk.Error("API key not configured"))
            return@flow
        }

        val profile = vitalsRepository.getProfileTriptychSummary()
        val vitals = vitalsRepository.getVitalsDashboardSnapshot()
        val biomarkers = labBiomarkerDao.getAll()

        val prompt = buildPrompt(profile, vitals, biomarkers)

        emit(ReportChunk.Started)

        llmClient.generateContentStream(prompt).collect { chunk ->
            emit(ReportChunk.Text(chunk))
        }

        emit(ReportChunk.Done)
    }
        .catch { e -> emit(ReportChunk.Error(e.message ?: "Unknown error")) }
        .flowOn(Dispatchers.IO)

    private fun buildPrompt(
        profile: ProfileTriptychSummary,
        vitals: List<VitalSummaryCardModel>,
        biomarkers: List<LabBiomarkerEntity>,
    ): String = buildString {
        appendLine("=== BioTrace Health Report ===")
        appendLine()

        // Profile summary
        appendLine("## Profile")
        appendLine("User: ${profile.userName}")
        appendLine("Source: ${profile.deviceLabel}")
        appendLine("Cardio Status: ${profile.cardioStatus}")
        appendLine("Resting HR: ${profile.restingHr} (${profile.restingHrDelta})")
        appendLine("Sleep Score: ${profile.sleepScore} — ${profile.sleepStatus}")
        appendLine("VO2 Max: ${profile.vo2Max} (${profile.vo2MaxTier})")
        appendLine()

        // Vitals snapshot
        appendLine("## Vitals Snapshot")
        if (vitals.isEmpty()) {
            appendLine("No vitals data available.")
        } else {
            vitals.forEach { vital ->
                val metricTitle = vital.metricType.name
                    .replace('_', ' ')
                    .lowercase()
                    .replaceFirstChar { it.uppercaseChar() }
                appendLine("$metricTitle: ${vital.primaryValue} ${vital.primaryUnit}".trim())
            }
        }
        appendLine()

        // Lab biomarkers
        appendLine("## Lab Biomarkers")
        if (biomarkers.isEmpty()) {
            appendLine("No lab biomarker data available.")
        } else {
            biomarkers.forEach { marker ->
                val refRange = when {
                    marker.referenceMin != null && marker.referenceMax != null ->
                        "${marker.referenceMin}–${marker.referenceMax} ${marker.unit}"
                    marker.referenceMin != null -> ">= ${marker.referenceMin} ${marker.unit}"
                    marker.referenceMax != null -> "<= ${marker.referenceMax} ${marker.unit}"
                    else -> "N/A"
                }
                appendLine("${marker.rawName}: ${marker.value} ${marker.unit} | refRange: $refRange")
            }
        }
        appendLine()

        appendLine(
            "Based on the above health data, provide a concise, evidence-based health report. " +
                "Highlight any values outside normal ranges, trends of concern, and actionable " +
                "recommendations. Keep the tone professional and supportive."
        )
    }
}
