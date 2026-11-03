package io.biotrace.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.local.entity.ChatMessageEntity
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.HealthAgent
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.llm.LlmNotConfiguredException
import io.biotrace.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isConfigured: Boolean = true,
    val isLoading: Boolean = false,
    val currentStreamingResponse: String = "",
    val currentCitations: List<Citation> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val healthAgent: HealthAgent,
    private val chatRepository: ChatRepository,
    private val llmClient: LlmClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        checkConfiguration()
        observeChatHistory()
    }

    fun checkConfiguration() {
        viewModelScope.launch {
            val configured = llmClient.isConfigured()
            _uiState.update { it.copy(isConfigured = configured) }
        }
    }

    private fun observeChatHistory() {
        viewModelScope.launch {
            chatRepository.getChatHistory().collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    fun sendMessage(userPrompt: String) {
        if (userPrompt.isBlank()) return

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = "USER",
                content = userPrompt.trim(),
                timestamp = Instant.now(),
                toolCalls = null,
                citations = null
            )
            chatRepository.saveMessage(userMsg)

            _uiState.update {
                it.copy(
                    isLoading = true,
                    currentStreamingResponse = "",
                    currentCitations = emptyList(),
                    error = null
                )
            }

            try {
                val history = _uiState.value.messages
                healthAgent.processQuery(userPrompt, history)
                    .catch { e ->
                        val errMsg = if (e is LlmNotConfiguredException) {
                            _uiState.update { it.copy(isConfigured = false) }
                            "Gemini API key is not configured. Please set it in Settings."
                        } else {
                            e.message ?: "An unexpected error occurred"
                        }
                        _uiState.update { it.copy(isLoading = false, error = errMsg) }
                    }
                    .collect { agentResponse ->
                        _uiState.update {
                            it.copy(
                                currentStreamingResponse = agentResponse.text,
                                currentCitations = agentResponse.citations
                            )
                        }

                        if (agentResponse.isComplete) {
                            val citationsJson = if (agentResponse.citations.isNotEmpty()) {
                                JSONArray(agentResponse.citations.map { c ->
                                    JSONObject().apply {
                                        put("id", c.id)
                                        put("timestamp", c.timestamp.toString())
                                        put("value", c.value)
                                        put("metric", c.metric)
                                        put("baseline", c.baseline)
                                    }
                                }).toString()
                            } else null

                            val assistantMsg = ChatMessageEntity(
                                id = UUID.randomUUID().toString(),
                                role = "ASSISTANT",
                                content = agentResponse.text,
                                timestamp = Instant.now(),
                                toolCalls = if (agentResponse.toolCallsMade.isNotEmpty()) JSONArray(agentResponse.toolCallsMade).toString() else null,
                                citations = citationsJson
                            )
                            chatRepository.saveMessage(assistantMsg)

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentStreamingResponse = "",
                                    currentCitations = emptyList()
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An error occurred"
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }
}
