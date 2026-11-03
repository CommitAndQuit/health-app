package io.biotrace.app.domain.agent

import java.time.Instant

data class Citation(
    val id: String,
    val timestamp: Instant,
    val value: Double,
    val metric: String? = null,
    val baseline: String? = null
) {
    fun format(): String {
        val basePart = if (baseline != null) ", Baseline: $baseline" else ""
        return "[Record #$id: $timestamp, $value$basePart]"
    }
}

data class ToolResult(
    val success: Boolean,
    val data: String,
    val citations: List<Citation> = emptyList(),
    val error: String? = null
)

interface AgentTool {
    val name: String
    val description: String
    suspend fun execute(params: Map<String, Any>): ToolResult
}

data class AgentResponse(
    val text: String,
    val isComplete: Boolean = false,
    val citations: List<Citation> = emptyList(),
    val toolCallsMade: List<String> = emptyList()
)
