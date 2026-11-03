package io.biotrace.app.domain.agent

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.domain.agent.tools.GetActiveAnomaliesTool
import io.biotrace.app.domain.agent.tools.GetNutritionIntakeTool
import io.biotrace.app.domain.agent.tools.QueryLabBiomarkersTool
import io.biotrace.app.domain.agent.tools.QueryVitalsTool
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class HealthAgentTest {

    private val vitalsRepository: VitalsRepository = mock()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val mealDao: MealDao = mock()
    private val llmClient: LlmClient = mock()

    private lateinit var agent: HealthAgent

    @Before
    fun setup() {
        val vitalsTool = QueryVitalsTool(vitalsRepository)
        val labTool = QueryLabBiomarkersTool(labBiomarkerDao)
        val anomaliesTool = GetActiveAnomaliesTool(labBiomarkerDao)
        val nutritionTool = GetNutritionIntakeTool(mealDao)

        agent = HealthAgent(
            llmClient = llmClient,
            queryVitalsTool = vitalsTool,
            queryLabBiomarkersTool = labTool,
            getActiveAnomaliesTool = anomaliesTool,
            getNutritionIntakeTool = nutritionTool
        )
    }

    @Test
    fun testProcessQueryDirectAnswer() = runTest {
        whenever(llmClient.generateContent(any())).thenReturn(
            "Final Answer: Your resting heart rate has been steady at 68 bpm [Record #hr_1: 2026-11-03T10:00:00Z, 68.0]."
        )

        val responses = agent.processQuery("How is my heart rate?", emptyList()).toList()

        assertEquals(1, responses.size)
        val resp = responses.first()
        assertTrue(resp.isComplete)
        assertTrue(resp.text.contains("steady at 68 bpm"))
    }

    @Test
    fun testProcessQueryWithToolCall() = runTest {
        val now = Instant.parse("2026-11-03T10:00:00Z")
        whenever(vitalsRepository.getVitalsBetween(any(), any(), any())).thenReturn(
            listOf(
                VitalPoint("hr_1", now, 72.0, "bpm", "HEART_RATE")
            )
        )

        var callCount = 0
        whenever(llmClient.generateContent(any())).thenAnswer {
            callCount++
            if (callCount == 1) {
                "Action: query_vitals_summary\nAction Input: {\"metric\": \"HEART_RATE\"}"
            } else {
                "Final Answer: Based on your recent vitals, your average heart rate is 72 bpm [Record #hr_1: 2026-11-03T10:00:00Z, 72.0]."
            }
        }

        val responses = agent.processQuery("Check my heart rate", emptyList()).toList()

        assertEquals(1, responses.size)
        val resp = responses.first()
        assertTrue(resp.isComplete)
        assertEquals(1, resp.toolCallsMade.size)
        assertEquals("query_vitals_summary", resp.toolCallsMade.first())
        assertEquals(1, resp.citations.size)
        assertEquals("hr_1", resp.citations.first().id)
    }
}
