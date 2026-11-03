package io.biotrace.app.domain.agent

import io.biotrace.app.data.local.entity.ChatMessageEntity
import io.biotrace.app.domain.agent.tools.GetActiveAnomaliesTool
import io.biotrace.app.domain.agent.tools.GetNutritionIntakeTool
import io.biotrace.app.domain.agent.tools.QueryLabBiomarkersTool
import io.biotrace.app.domain.agent.tools.QueryVitalsTool
import io.biotrace.app.domain.llm.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthAgent @Inject constructor(
    private val llmClient: LlmClient,
    queryVitalsTool: QueryVitalsTool,
    queryLabBiomarkersTool: QueryLabBiomarkersTool,
    getActiveAnomaliesTool: GetActiveAnomaliesTool,
    getNutritionIntakeTool: GetNutritionIntakeTool
) {

    private val tools: Map<String, AgentTool> = listOf(
        queryVitalsTool,
        queryLabBiomarkersTool,
        getActiveAnomaliesTool,
        getNutritionIntakeTool
    ).associateBy { it.name }

    companion object {
        private const val MAX_TURNS = 5
        private const val MAX_TOOL_CALLS = 6
    }

    private fun buildToolsDescription(): String {
        return tools.values.joinToString("\n\n") { tool ->
            "- Tool: ${tool.name}\n  Description: ${tool.description}"
        }
    }

    suspend fun processQuery(
        userMessage: String,
        chatHistory: List<ChatMessageEntity>
    ): Flow<AgentResponse> = flow {
        val toolDescriptions = buildToolsDescription()
        val allCitations = mutableListOf<Citation>()
        val toolCallsMade = mutableListOf<String>()

        val conversationContext = StringBuilder()
        if (chatHistory.isNotEmpty()) {
            conversationContext.append("Recent Conversation History:\n")
            chatHistory.takeLast(4).forEach { msg ->
                conversationContext.append("${msg.role}: ${msg.content}\n")
            }
            conversationContext.append("\n")
        }

        var reasoningPrompt = """
You are the BioTrace ReAct reasoning agent. You have access to local health data on the user's phone via internal tools:

$toolDescriptions

Instructions:
1. When you need local health data (vitals, lab tests, anomalies, meals), invoke a tool using this EXACT format:
Action: <tool_name>
Action Input: <json_formatted_parameters>

2. When you receive an Observation, analyze it. You may call another tool if needed.

3. Once you have enough data to answer the user's question, provide the final response in this EXACT format:
Final Answer: <your full helpful analysis and guidance, citing records like [Record #ID: Date, Value, Baseline]>

Rules:
- Never guess user health data. If you need data, call the appropriate tool.
- Ground all conclusions in the observed data.
- Always be empathetic, clinical, and conservative.

$conversationContext
User Question: $userMessage
"""

        var turns = 0
        var totalToolCalls = 0

        while (turns < MAX_TURNS && totalToolCalls < MAX_TOOL_CALLS) {
            turns++

            val llmOutput = llmClient.generateContent(reasoningPrompt)

            // Check if model called an Action
            val actionMatch = Regex("Action:\\s*([a-zA-Z0-9_]+)").find(llmOutput)
            val actionInputMatch = Regex("Action Input:\\s*(\\{.*?\\}|[^\n]+)", RegexOption.DOT_MATCHES_ALL).find(llmOutput)

            if (actionMatch != null) {
                val toolName = actionMatch.groupValues[1].trim()
                val toolInputRaw = actionInputMatch?.groupValues?.get(1)?.trim().orEmpty()

                val tool = tools[toolName]
                if (tool != null) {
                    totalToolCalls++
                    toolCallsMade.add(toolName)

                    val params = parseParams(toolInputRaw)
                    val result = tool.execute(params)
                    allCitations.addAll(result.citations)

                    reasoningPrompt += "\n$llmOutput\nObservation: ${result.data}\n"
                    continue
                }
            }

            // Check for Final Answer
            val finalAnswerMatch = Regex("Final Answer:\\s*(.*)", RegexOption.DOT_MATCHES_ALL).find(llmOutput)
            val finalAnswer = if (finalAnswerMatch != null) {
                finalAnswerMatch.groupValues[1].trim()
            } else {
                llmOutput.trim()
            }

            emit(
                AgentResponse(
                    text = finalAnswer,
                    isComplete = true,
                    citations = allCitations.distinctBy { it.id },
                    toolCallsMade = toolCallsMade
                )
            )
            return@flow
        }

        // If loop exceeded max turns, generate final summary directly
        val fallbackPrompt = "$reasoningPrompt\nPlease provide your Final Answer based on the observations collected so far."
        val finalResult = llmClient.generateContent(fallbackPrompt)
        val cleaned = finalResult.replace(Regex("^Final Answer:\\s*", RegexOption.IGNORE_CASE), "").trim()

        emit(
            AgentResponse(
                text = cleaned,
                isComplete = true,
                citations = allCitations.distinctBy { it.id },
                toolCallsMade = toolCallsMade
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun parseParams(raw: String): Map<String, Any> {
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Any>()
        Regex("\"([a-zA-Z0-9_]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9.]+)|(true|false))").findAll(raw).forEach { match ->
            val key = match.groupValues[1]
            val strVal = match.groupValues[2]
            val numVal = match.groupValues[3]
            val boolVal = match.groupValues[4]
            when {
                strVal.isNotEmpty() -> map[key] = strVal
                numVal.isNotEmpty() -> map[key] = numVal.toDoubleOrNull() ?: numVal
                boolVal.isNotEmpty() -> map[key] = boolVal.toBoolean()
            }
        }
        return map
    }
}
