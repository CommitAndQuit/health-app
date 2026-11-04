package io.biotrace.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.R
import io.biotrace.app.data.local.entity.ChatMessageEntity
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.ui.theme.BioOrange
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.StatusGreen
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextSecondary
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.checkConfiguration()
    }

    LaunchedEffect(uiState.messages.size, uiState.currentStreamingResponse) {
        val total = uiState.messages.size + (if (uiState.isLoading) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            io.biotrace.app.ui.common.BioTraceTopBar(
                subtitle = "AI Copilot",
                onAvatarClick = onNavigateToSettings,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Unconfigured API Key Banner
            if (!uiState.isConfigured) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1ED)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFCCBC))
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = BioOrange
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini API key is required to query your vitals, lab reports, and meals.",
                                fontSize = 12.sp,
                                color = TextDark
                            )
                        }
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BioOrange,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Settings", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BioOrange,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cross-Telemetry Health Intelligence",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your AI assistant synthesizes Health Connect vitals, scanned blood reports, and food calories into actionable plans and insights.",
                                fontSize = 12.5.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.Center,
                                maxItemsInEachRow = 2
                            ) {
                                AssistChip(
                                    onClick = { viewModel.sendMessage("Create a comprehensive 7-day health, sleep, and nutrition plan based on my wearable vitals, lab reports, and meal logs.") },
                                    label = { Text("📋 Create 7-Day Plan", fontSize = 11.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = PureWhite, labelColor = TextDark),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = CardBorder),
                                    modifier = Modifier.padding(4.dp)
                                )
                                AssistChip(
                                    onClick = { viewModel.sendMessage("Analyze my resting heart rate and HRV against my latest lab test biomarkers to find any correlations.") },
                                    label = { Text("🔬 Correlate Vitals with Labs", fontSize = 11.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = PureWhite, labelColor = TextDark),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = CardBorder),
                                    modifier = Modifier.padding(4.dp)
                                )
                                AssistChip(
                                    onClick = { viewModel.sendMessage("Evaluate my calorie and macronutrient intake against my daily activity, sleep, and vitals.") },
                                    label = { Text("🥗 Meal & Calorie Analysis", fontSize = 11.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = PureWhite, labelColor = TextDark),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = CardBorder),
                                    modifier = Modifier.padding(4.dp)
                                )
                                AssistChip(
                                    onClick = { viewModel.sendMessage("Check for any active anomalies across my vitals, lab biomarkers, and meals, and recommend next steps.") },
                                    label = { Text("⚡ Full Health Anomaly Scan", fontSize = 11.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = PureWhite, labelColor = TextDark),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = CardBorder),
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(message = message)
                }

                if (uiState.isLoading) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = BioOrange
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Synthesizing vitals, blood markers, and meals...",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                if (uiState.currentStreamingResponse.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = uiState.currentStreamingResponse,
                                        color = TextDark,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = PureWhite,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask about plans, vitals, labs, or food...", fontSize = 13.sp, color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LightBackground,
                            unfocusedContainerColor = LightBackground,
                            focusedBorderColor = BioOrange,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val msg = inputText.trim()
                            if (msg.isNotEmpty()) {
                                viewModel.sendMessage(msg)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !uiState.isLoading) BioOrange else CardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_send),
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessageEntity) {
    val isUser = message.role.equals("user", ignoreCase = true)

    val citations = remember(message.citations) {
        parseCitations(message.citations)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) BioOrange else PureWhite
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (!isUser) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                )
            } else null,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isUser) PureWhite else TextDark
                )

                if (citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "DATA EVIDENCE CITATIONS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = if (isUser) PureWhite.copy(alpha = 0.85f) else BioOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    citations.forEach { citation ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isUser) PureWhite.copy(alpha = 0.7f) else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${citation.metric ?: "Record"}: ${citation.value} (#${citation.id})",
                                fontSize = 11.sp,
                                color = if (isUser) PureWhite.copy(alpha = 0.9f) else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseCitations(citationsJson: String?): List<Citation> {
    if (citationsJson.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(citationsJson)
        val list = mutableListOf<Citation>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Citation(
                    id = obj.optString("id", obj.optString("recordId", "")),
                    timestamp = java.time.Instant.parse(obj.optString("timestamp", java.time.Instant.now().toString())),
                    value = obj.optDouble("value", 0.0),
                    metric = obj.optString("metric", obj.optString("metricType", "Vitals")),
                    baseline = if (obj.has("baseline")) obj.optString("baseline") else null
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}
