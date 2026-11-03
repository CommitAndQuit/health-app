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
import io.biotrace.app.ui.theme.BioRed
import io.biotrace.app.ui.theme.DarkBackground
import io.biotrace.app.ui.theme.DarkInputBg
import io.biotrace.app.ui.theme.DarkSurface
import io.biotrace.app.ui.theme.DarkSurfaceBorder
import io.biotrace.app.ui.theme.DarkSurfaceElevated
import io.biotrace.app.ui.theme.StatusGreen
import io.biotrace.app.ui.theme.TextGray
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextWhite
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
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = BioRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BioTrace AI Copilot",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (uiState.isConfigured) StatusGreen else BioRed,
                                    shape = CircleShape,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (uiState.isConfigured) "ALL DATA CONNECTED • AES-256" else "API KEY REQUIRED",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.clear),
                                tint = TextGray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1618)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5A2024))
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
                            tint = BioRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini API key is required to query your vitals, lab reports, and meals.",
                                fontSize = 12.sp,
                                color = TextWhite
                            )
                        }
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BioRed,
                                contentColor = TextWhite
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
                                tint = BioRed,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cross-Telemetry Health Intelligence",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your AI assistant synthesizes Health Connect vitals, scanned blood reports, and food calories into actionable plans and insights.",
                                fontSize = 12.5.sp,
                                color = TextGray,
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
                                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceElevated, labelColor = TextWhite),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceBorder),
                                    modifier = Modifier.padding(4.dp)
                                )
                                AssistChip(
                                    onClick = { viewModel.sendMessage("Analyze my resting heart rate and HRV against my latest lab test biomarkers to find any correlations.") },
                                    label = { Text("🔬 Correlate Vitals with Labs", fontSize = 11.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceElevated, labelColor = TextWhite),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceBorder),
                                    modifier = Modifier.padding(4.dp)
                                )
                                AssistChip(
                                    onClick = { viewModel.sendMessage("Evaluate my calorie and macronutrient intake against my daily activity, sleep, and vitals.") },
                                    label = { Text("🥗 Meal & Calorie Analysis", fontSize = 11.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceElevated, labelColor = TextWhite),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceBorder),
                                    modifier = Modifier.padding(4.dp)
                                )
                                AssistChip(
                                    onClick = { viewModel.sendMessage("Check for any active anomalies across my vitals, lab biomarkers, and meals, and recommend next steps.") },
                                    label = { Text("⚡ Full Health Anomaly Scan", fontSize = 11.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceElevated, labelColor = TextWhite),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceBorder),
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
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = BioRed
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Synthesizing vitals, blood markers, and meals...",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                }
                                if (uiState.currentStreamingResponse.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = uiState.currentStreamingResponse,
                                        color = TextWhite,
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
                color = DarkBackground,
                tonalElevation = 4.dp,
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
                        placeholder = { Text("Ask about plans, vitals, labs, or food...", fontSize = 13.sp, color = TextGray) },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkInputBg,
                            unfocusedContainerColor = DarkInputBg,
                            focusedBorderColor = BioRed,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
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
                            .background(if (inputText.isNotBlank() && !uiState.isLoading) BioRed else DarkSurfaceBorder)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_send),
                            tint = TextWhite,
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
                containerColor = if (isUser) BioRed else DarkSurface
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (!isUser) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            } else null,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextWhite
                )

                if (citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "DATA EVIDENCE CITATIONS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = if (isUser) Color.White.copy(alpha = 0.8f) else BioRed
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
                                tint = TextGray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${citation.metric ?: "Record"}: ${citation.value} (#${citation.id})",
                                fontSize = 11.sp,
                                color = TextGray
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
