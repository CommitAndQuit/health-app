# Implementation Plan - BioTrace MVP (Phases 1-2)

## Problem Statement
Users need a privacy-first Android application that consolidates health data from wearables via Health Connect and provides AI-powered health insights using their own LLM API keys, with all data stored locally on-device in an encrypted database. No backend servers, no cloud sync - complete data sovereignty.

## Requirements Summary

| Aspect | Decision |
|--------|----------|
| **Development Environment** | Command-line tools only |
| **Target SDK** | API 34 (Android 14+) - Health Connect system module |
| **LLM Provider** | Google Gemini (BYOK) |
| **Build System** | Gradle Kotlin DSL (build.gradle.kts) |
| **Testing** | JUnit + Mockito + Turbine |
| **Signing** | Debug builds first |
| **Package Name** | `io.biotrace.app` |
| **Scope** | Phases 1-2 (MVP: Health Connect + LLM Agent) |

## Architecture

- **Three-layer Clean Architecture**: presentation, domain, data
- **MVVM with Jetpack Compose**
- **Hilt for dependency injection**
- **Room for local database**
- **Health Connect SDK for wearable data**
- **Google Gen AI Kotlin SDK for Gemini streaming**

## Task Breakdown

### Task 1: Project Scaffolding and Build Configuration
**Objective:** Create the Android project structure with Gradle Kotlin DSL, configure dependencies, and establish the Clean Architecture package layout.

**Implementation Details:**
- Create project using manual directory structure
- Set up `build.gradle.kts` files (project-level and app-level)
- Configure Gradle Kotlin DSL with:
  - `compileSdk = 36`
  - `minSdk = 34` (target API 34+)
  - `targetSdk = 36`
  - Kotlin 2.0+, Hilt plugin, KSP for annotation processing
- Add dependencies:
  - Jetpack Compose BOM 2024.11.00
  - Hilt DI 2.52
  - Room 2.6.1 (with KSP)
  - Health Connect SDK: `androidx.health.connect:connect-client:1.2.0-alpha06`
  - Google Gen AI: `com.google.genai:kotlin-genai:0.4.0`
  - OkHttp 4.12.0 for networking
  - JUnit, Mockito, Turbine for testing
- Create package structure:
  ```
  io.biotrace.app/
  ├── ui/           (Compose screens, ViewModels)
  ├── domain/       (use cases, models, repository interfaces)
  ├── data/         (repositories, Room DAOs, Health Connect client)
  ├── di/           (Hilt modules)
  └── BioTraceApplication.kt
  ```
- Configure `AndroidManifest.xml` with:
  - `android:allowBackup="false"` (per privacy requirements)
  - Health Connect permissions declarations
  - Privacy policy activity for Health Connect

**Test Requirements:**
- Unit test: Verify Hilt application component builds
- Build verification: `./gradlew assembleDebug` succeeds

**Demo:** Empty app builds and launches showing "BioTrace" app name with a placeholder Compose screen.

**Status:** ✅ COMPLETE

---

### Task 2: Android Keystore Security Manager
**Objective:** Implement secure API key storage using Android Keystore with AES-256-GCM encryption.

**Implementation Details:**
- Create `KeystoreManager` interface in `domain/security/`:
  ```kotlin
  interface KeystoreManager {
      suspend fun storeApiKey(provider: String, apiKey: String)
      suspend fun getApiKey(provider: String): String?
      suspend fun hasApiKey(provider: String): Boolean
      suspend fun deleteApiKey(provider: String)
  }
  ```
- Implement `KeystoreManagerImpl` in `data/security/`:
  - Use `KeyGenParameterSpec.Builder` with:
    - `setKeySize(256)`
    - `setBlockModes(KeyProperties.BLOCK_MODE_GCM)`
    - `setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)`
    - `setUserAuthenticationRequired(false)` (key available without auth)
  - Alias pattern: `biotrace_apikey_{provider}`
  - Store IV with ciphertext (Base64 encoded)
- Create Hilt module `SecurityModule` to bind implementation
- Handle edge cases:
  - Key already exists scenario
  - Device doesn't support StrongBox (fallback to regular Keystore)

**Test Requirements:**
- Unit test: `KeystoreManagerImpl` encrypts and decrypts correctly
- Unit test: Stored key survives app restart (using fake Keystore or Robolectric)
- Unit test: Deleting key removes it completely
- Unit test: Different providers use different keys

**Demo:** A test screen with:
- Input field for API key
- "Store" button that encrypts and saves
- "Retrieve" button that decrypts and displays
- "Delete" button that removes the key
- Status showing "API Key stored: Yes/No"

**Status:** 🔲 NOT STARTED

---

### Task 3: Room Database Schema and Basic Entities
**Objective:** Define Room database with tables for vitals, lab biomarkers, meals, and chat history.

**Implementation Details:**
- Create entity classes in `data/local/entity/`:
  ```kotlin
  @Entity(tableName = "vitals_records")
  data class VitalsRecordEntity(
      @PrimaryKey val id: String,
      val metricType: String, // "HEART_RATE", "HRV", "STEPS", etc.
      val value: Double,
      val unit: String,
      val timestamp: Instant,
      val source: String, // "HEALTH_CONNECT", "MANUAL"
      val createdAt: Instant
  )

  @Entity(tableName = "lab_biomarkers")
  data class LabBiomarkerEntity(
      @PrimaryKey val id: String,
      val biomarkerName: String, // normalized name
      val rawName: String, // original name from report
      val value: Double,
      val unit: String,
      val referenceMin: Double?,
      val referenceMax: Double?,
      val flag: String?, // "HIGH", "LOW", "NORMAL"
      val recordedAt: Instant,
      val source: String
  )

  @Entity(tableName = "meals")
  data class MealEntity(
      @PrimaryKey val id: String,
      val name: String,
      val calories: Int,
      val proteinGrams: Double,
      val carbsGrams: Double,
      val fatsGrams: Double,
      val fiberGrams: Double?,
      val consumedAt: Instant,
      val photoPath: String?
  )

  @Entity(tableName = "chat_history")
  data class ChatMessageEntity(
      @PrimaryKey val id: String,
      val role: String, // "USER", "ASSISTANT"
      val content: String,
      val timestamp: Instant,
      val toolCalls: String?, // JSON of tool calls made
      val citations: String? // JSON of data sources cited
  )
  ```
- Create DAOs with suspend functions:
  - `VitalsDao`: insert, query by type and time range, delete old records
  - `LabBiomarkerDao`: insert, query by name, get latest values
  - `MealDao`: insert, query by date range
  - `ChatHistoryDao`: insert, get recent messages, clear history
- Create `BioTraceDatabase` extending `RoomDatabase`
- Add type converters for `Instant`, `ZoneOffset`
- Create Hilt module `DatabaseModule` providing database and DAOs

**Test Requirements:**
- Unit test: Each entity can be inserted and retrieved
- Unit test: Time range queries work correctly
- Unit test: Type converters serialize/deserialize properly
- Migration test: Verify database creation succeeds

**Demo:** A debug screen showing:
- "Insert Test Data" button
- "Query All Vitals" button
- Display of all records in a simple list

**Status:** 🔲 NOT STARTED

---

### Task 4: Health Connect Permission Flow
**Objective:** Implement Health Connect availability check and permission request UI.

**Implementation Details:**
- Create `HealthConnectManager` in `data/healthconnect/`:
  ```kotlin
  class HealthConnectManager @Inject constructor(
      private val context: Context
  ) {
      val healthConnectClient: HealthConnectClient?
      
      suspend fun checkAvailability(): HealthConnectAvailability
      suspend fun hasAllPermissions(): Boolean
      fun getRequiredPermissions(): Set<String>
  }
  
  enum class HealthConnectAvailability {
      AVAILABLE, NOT_INSTALLED, NOT_SUPPORTED
  }
  ```
- Define required permissions in `AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.health.READ_HEART_RATE"/>
  <uses-permission android:name="android.permission.health.READ_HEART_RATE_VARIABILITY"/>
  <uses-permission android:name="android.permission.health.READ_STEPS"/>
  <uses-permission android:name="android.permission.health.READ_SLEEP"/>
  <uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED"/>
  <uses-permission android:name="android.permission.health.READ_OXYGEN_SATURATION"/>
  ```
- Create `HealthConnectPermissionScreen` Composable:
  - Show availability status (available/not installed)
  - "Grant Permissions" button
  - Display current permission status
  - Link to Health Connect settings if needed
- Use `PermissionController.createRequestPermissionResultContract()` in ViewModel
- Create permission set:
  ```kotlin
  val permissions = setOf(
      HealthPermission.getReadPermission(HeartRateRecord::class),
      HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
      HealthPermission.getReadPermission(StepsRecord::class),
      HealthPermission.getReadPermission(SleepSessionRecord::class),
      HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
      HealthPermission.getReadPermission(OxygenSaturationRecord::class)
  )
  ```
- Add privacy policy activity for Health Connect requirement

**Test Requirements:**
- Unit test: `HealthConnectManager` returns correct availability states
- Unit test: Permission set contains all required permissions
- UI test: Permission screen renders correctly
- UI test: Permission request can be triggered (with fake Health Connect)

**Demo:** On app launch:
- Health Connect availability is checked
- If available, show "Grant Permissions" screen
- After granting, show "Permissions Granted ✓" with list of granted permissions
- User can revoke and re-request permissions

**Status:** 🔲 NOT STARTED

---

### Task 5: Health Connect Data Sync Service
**Objective:** Implement background synchronization of health data from Health Connect to local Room database.

**Implementation Details:**
- Create `HealthConnectSyncService` in `data/healthconnect/`:
  ```kotlin
  class HealthConnectSyncService @Inject constructor(
      private val healthConnectClient: HealthConnectClient,
      private val vitalsDao: VitalsDao
  ) {
      suspend fun syncHeartRate(startTime: Instant, endTime: Instant): SyncResult
      suspend fun syncSteps(startTime: Instant, endTime: Instant): SyncResult
      suspend fun syncSleep(startTime: Instant, endTime: Instant): SyncResult
      suspend fun syncHRV(startTime: Instant, endTime: Instant): SyncResult
      suspend fun syncAll(): SyncResult
  }
  
  data class SyncResult(
      val recordsSynced: Int,
      val errors: List<String>
  )
  ```
- Implement reading from Health Connect:
  ```kotlin
  suspend fun syncHeartRate(startTime: Instant, endTime: Instant): SyncResult {
      val request = ReadRecordsRequest(
          recordType = HeartRateRecord::class,
          timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
      )
      val response = healthConnectClient.readRecords(request)
      // Map to VitalsRecordEntity and insert to Room
  }
  ```
- Create `HealthSyncWorker` using WorkManager:
  - Periodic sync every 6 hours
  - Constraints: Device charging optional, network not required
  - Sync last 7 days of data
- Handle Health Connect's 30-day read limit (use `PERMISSION_READ_HEALTH_DATA_HISTORY` if needed)
- Map Health Connect record types to our `VitalsRecordEntity`:
  - `HeartRateRecord` → samples with timestamps
  - `HeartRateVariabilityRmssdRecord` → single HRV value
  - `StepsRecord` → use `aggregate()` for daily totals
  - `SleepSessionRecord` → sleep stages and duration

**Test Requirements:**
- Unit test: `syncHeartRate` correctly maps records to entities
- Unit test: Duplicate records are handled (upsert logic)
- Unit test: `HealthSyncWorker` enqueues correctly
- Integration test: Sync from Health Connect to Room succeeds (with fake client)

**Demo:** A sync status screen showing:
- Last sync timestamp
- "Sync Now" button
- Progress indicator during sync
- Count of records synced per metric type
- Any errors encountered

**Status:** 🔲 NOT STARTED

---

### Task 6: Vitals Dashboard UI with Charts
**Objective:** Create the main dashboard displaying health metrics with time-series visualizations.

**Implementation Details:**
- Create `VitalsRepository` in `domain/repository/`:
  ```kotlin
  interface VitalsRepository {
      fun getVitalsFlow(metricType: String, timeRange: TimeRange): Flow<List<VitalPoint>>
      suspend fun getLatestValue(metricType: String): VitalPoint?
      suspend fun getAggregatedStats(metricType: String, period: Period): AggregatedStats
  }
  ```
- Create `GetVitalsUseCase`:
  ```kotlin
  class GetVitalsUseCase @Inject constructor(
      private val vitalsRepository: VitalsRepository
  ) {
      operator fun invoke(metricType: String, timeRange: TimeRange): Flow<List<VitalPoint>>
  }
  ```
- Create `DashboardViewModel`:
  ```kotlin
  @HiltViewModel
  class DashboardViewModel @Inject constructor(
      private val getVitalsUseCase: GetVitalsUseCase
  ) : ViewModel() {
      private val _uiState = MutableStateFlow(DashboardUiState())
      val uiState: StateFlow<DashboardUiState> = _uiState
      
      fun loadData(timeRange: TimeRange) { ... }
      fun selectMetric(metric: MetricType) { ... }
  }
  ```
- Create Compose chart components:
  - Use `androidx.compose.ui.graphics` for custom drawing or integrate a chart library (MPAndroidChart or custom Canvas)
  - `TimeSeriesChart`: Line chart with time on X-axis
  - `SleepStageChart`: Stacked bar for sleep stages
  - `DailyStepsChart`: Bar chart with daily totals
  - Support zoom and pan for detailed exploration
- Create `DashboardScreen` Composable:
  - Top app bar with time range selector (Day/Week/Month)
  - Metric cards showing latest values with trend indicators
  - Expandable chart sections for each metric type
  - Pull-to-refresh to trigger sync
- Implement time range aggregation:
  - Hourly for day view
  - Daily for week view
  - Weekly for month view

**Test Requirements:**
- Unit test: `DashboardViewModel` emits correct UI states
- Unit test: `GetVitalsUseCase` aggregates data correctly
- Unit test: Repository returns proper time range data
- UI test: Dashboard renders with sample data
- Snapshot test: Chart rendering (optional)

**Demo:** Dashboard screen displaying:
- Heart rate card with latest value and 7-day trend chart
- Steps card with daily count and weekly bar chart
- Sleep card with last night's stages and duration
- HRV card with current value and 30-day trend
- Time range selector that updates all charts
- Pull-to-refresh that triggers sync

**Status:** 🔲 NOT STARTED

---

### Task 7: Gemini API Client with Streaming Support
**Objective:** Implement a streaming Gemini API client with proper error handling and token management.

**Implementation Details:**
- Create `LlmClient` interface in `domain/llm/`:
  ```kotlin
  interface LlmClient {
      fun generateContentStream(prompt: String): Flow<String>
      suspend fun generateContent(prompt: String): String
      suspend fun isConfigured(): Boolean
  }
  ```
- Create `GeminiClient` implementation in `data/llm/`:
  ```kotlin
  class GeminiClient @Inject constructor(
      private val keystoreManager: KeystoreManager
  ) : LlmClient {
      private val client: Client? // Lazy initialized with API key
      
      override fun generateContentStream(prompt: String): Flow<String> {
          val apiKey = keystoreManager.getApiKey("gemini")
              ?: throw LlmNotConfiguredException()
          
          return client.models.generateContentStream(
              model = "gemini-2.0-flash",
              text = prompt
          ).map { it.text ?: "" }
      }
  }
  ```
- Handle streaming with Kotlin Flow:
  ```kotlin
  fun generateContentStream(prompt: String): Flow<String> = flow {
      client.use { client ->
          client.models.generateContentStream(
              model = "gemini-2.0-flash",
              text = prompt
          ).collect { response ->
              emit(response.text ?: "")
          }
      }
  }
  ```
- Implement error handling:
  - Network timeout (30-second read timeout)
  - Rate limiting (429 responses)
  - Invalid API key (401 responses)
  - Malformed responses
- Create `LlmModule` Hilt module to bind implementation
- Add system instruction for health context:
  ```kotlin
  val config = GenerateContentConfig(
      systemInstruction = Content.fromText(
          "You are a health assistant. Always cite data sources. Be conservative with medical advice."
      )
  )
  ```

**Test Requirements:**
- Unit test: `generateContentStream` emits tokens in order
- Unit test: Network errors are caught and wrapped
- Unit test: Missing API key throws `LlmNotConfiguredException`
- Integration test: Mock Gemini API response streams correctly
- Use MockWebServer for HTTP testing

**Demo:** A test screen with:
- Text input for prompts
- "Send" button that triggers streaming
- Real-time token display as they arrive
- Error message display when API key is missing
- Loading indicator during streaming

**Status:** 🔲 NOT STARTED

---

### Task 8: ReAct Agent with Health Data Tools
**Objective:** Implement a reasoning agent that can query local health data and provide cited responses.

**Implementation Details:**
- Define tool interfaces in `domain/agent/`:
  ```kotlin
  interface AgentTool {
      val name: String
      val description: String
      suspend fun execute(params: Map<String, Any>): ToolResult
  }
  
  data class ToolResult(
      val success: Boolean,
      val data: String,
      val citations: List<Citation>
  )
  ```
- Implement health data tools:
  ```kotlin
  class QueryVitalsTool @Inject constructor(
      private val vitalsRepository: VitalsRepository
  ) : AgentTool {
      override val name = "query_vitals_summary"
      override val description = "Query health vitals like heart rate, HRV, steps, sleep"
      
      override suspend fun execute(params: Map<String, Any>): ToolResult {
          val metric = params["metric"] as String
          val startDate = Instant.parse(params["startDate"] as String)
          val endDate = Instant.parse(params["endDate"] as String)
          
          val vitals = vitalsRepository.getVitalsFlow(metric, TimeRange(startDate, endDate))
              .first()
          
          return ToolResult(
              success = true,
              data = formatAsJson(vitals),
              citations = vitals.map { Citation(it.id, it.timestamp, it.value) }
          )
      }
  }
  ```
- Create tools:
  - `QueryVitalsTool`: Query heart rate, HRV, steps, sleep
  - `QueryLabBiomarkersTool`: Query lab results by biomarker name
  - `GetActiveAnomaliesTool`: Get out-of-range values
  - `GetNutritionIntakeTool`: Query meal data by date
- Create `HealthAgent` in `domain/agent/`:
  ```kotlin
  class HealthAgent @Inject constructor(
      private val llmClient: LlmClient,
      private val tools: Map<String, AgentTool>
  ) {
      suspend fun processQuery(
          userMessage: String,
          chatHistory: List<ChatMessage>
      ): Flow<AgentResponse> = flow {
          // 1. Construct prompt with tool definitions
          // 2. Send to Gemini with function declarations
          // 3. If function call requested, execute tool
          // 4. Send tool result back to model
          // 5. Stream final response with citations
      }
  }
  ```
- Implement ReAct loop:
  - Max 5 reasoning turns
  - Max 6 tool calls per query
  - Mandatory citation format: `[Record #ID: Date, Value, Baseline]`
  - Tool budget tracking to prevent infinite loops

**Test Requirements:**
- Unit test: Each tool executes correctly with mock data
- Unit test: `HealthAgent` handles single tool call
- Unit test: `HealthAgent` handles multi-turn reasoning
- Unit test: Citations are properly formatted
- Unit test: Tool budget limits are enforced
- Integration test: End-to-end query with real Gemini (optional, use mock)

**Demo:** A debug screen showing:
- Query input: "What's my average heart rate this week?"
- Tool execution log: "Calling query_vitals_summary(heart_rate, ...)"
- Tool result: JSON data returned
- Final response: "Your average heart rate was 72 bpm [Record #123: 2026-11-01, 72, 70-100]"

**Status:** 🔲 NOT STARTED

---

### Task 9: Chat UI with Streaming Responses
**Objective:** Create the conversational AI interface with real-time streaming and source citations.

**Implementation Details:**
- Create `ChatRepository`:
  ```kotlin
  interface ChatRepository {
      fun getChatHistory(): Flow<List<ChatMessage>>
      suspend fun saveMessage(message: ChatMessage)
      suspend fun clearHistory()
  }
  ```
- Create `ChatViewModel`:
  ```kotlin
  @HiltViewModel
  class ChatViewModel @Inject constructor(
      private val healthAgent: HealthAgent,
      private val chatRepository: ChatRepository
  ) : ViewModel() {
      private val _uiState = MutableStateFlow(ChatUiState())
      val uiState: StateFlow<ChatUiState> = _uiState
      
      fun sendMessage(userMessage: String) {
          viewModelScope.launch {
              _uiState.update { it.copy(isLoading = true, currentResponse = "") }
              
              healthAgent.processQuery(userMessage, chatHistory)
                  .catch { e -> _uiState.update { it.copy(error = e.message) } }
                  .collect { response ->
                      _uiState.update { 
                          it.copy(currentResponse = it.currentResponse + response.text)
                      }
                  }
              
              _uiState.update { it.copy(isLoading = false) }
          }
      }
  }
  ```
- Create `ChatScreen` Composable:
  - Message list with user and assistant messages
  - Different styling for user (right-aligned) vs assistant (left-aligned)
  - Streaming text indicator with typing animation
  - Input field with send button
  - Source citations displayed as expandable cards
  - Example prompts: "What's my heart rate trend?", "Any anomalies in my labs?"
- Implement citation rendering:
  ```kotlin
  @Composable
  fun CitationCard(citation: Citation) {
      Card {
          Row {
              Icon(Icons.Default.DataObject, contentDescription = null)
              Column {
                  Text("Record #${citation.id}")
                  Text("${citation.timestamp}: ${citation.value}")
              }
          }
      }
  }
  ```
- Add quick action chips:
  - "Analyze my sleep"
  - "Heart rate trends"
  - "Lab results summary"

**Test Requirements:**
- Unit test: `ChatViewModel` handles streaming correctly
- Unit test: Messages are persisted to repository
- Unit test: Error states are handled
- UI test: Chat screen renders messages
- UI test: Send message triggers streaming
- UI test: Citations are displayed correctly

**Demo:** Chat interface showing:
- User types: "How's my sleep been lately?"
- Assistant streams response: "Looking at your sleep data..."
- Tool call indicator (optional debug view)
- Final response with sleep statistics
- Citation cards showing the source records
- User can tap citation to see full record details

**Status:** 🔲 NOT STARTED

---

### Task 10: Settings Screen and API Key Configuration
**Objective:** Create the settings interface for users to configure their Gemini API key and app preferences.

**Implementation Details:**
- Create `SettingsRepository`:
  ```kotlin
  interface SettingsRepository {
      suspend fun setApiKey(provider: String, apiKey: String)
      suspend fun getApiKey(provider: String): String?
      suspend fun hasApiKey(provider: String): Boolean
      suspend fun setDefaultModel(model: String)
      fun getSettings(): Flow<AppSettings>
  }
  ```
- Create `SettingsViewModel`:
  ```kotlin
  @HiltViewModel
  class SettingsViewModel @Inject constructor(
      private val settingsRepository: SettingsRepository,
      private val keystoreManager: KeystoreManager
  ) : ViewModel() {
      fun saveApiKey(provider: String, apiKey: String)
      fun deleteApiKey(provider: String)
      fun testApiKey(provider: String): Flow<TestResult>
  }
  ```
- Create `SettingsScreen` Composable:
  - "LLM Provider" section:
    - Currently selected: Gemini
    - API key input field (masked by default)
    - "Save API Key" button
    - "Test Connection" button
    - Status indicator: ✓ Connected / ✗ Invalid key
  - "Data Management" section:
    - "Sync Health Connect Data" button
    - "Clear All Data" button with confirmation
    - "Export Data" button (future feature)
  - "About" section:
    - App version
    - Privacy policy link
    - License information (GPL-3.0 or MIT)
- Implement API key validation:
  ```kotlin
  suspend fun testApiKey(apiKey: String): TestResult {
      return try {
          val client = Client(apiKey = apiKey)
          client.models.generateContent(
              model = "gemini-2.0-flash",
              text = "Hello"
          )
          TestResult.Success
      } catch (e: Exception) {
          TestResult.InvalidKey(e.message)
      }
  }
  ```
- Add security notice:
  - "Your API key is encrypted and stored locally on this device only"
  - "It is never sent to any server except the LLM provider"

**Test Requirements:**
- Unit test: `SettingsViewModel` saves API key correctly
- Unit test: `testApiKey` validates keys properly
- Unit test: API key is encrypted before storage
- UI test: Settings screen renders all options
- UI test: Save button stores key and shows success

**Demo:** Settings screen showing:
- "Gemini API Key" field (empty initially)
- User enters key and taps "Save"
- Status shows "✓ API Key saved securely"
- User taps "Test Connection"
- Progress indicator, then "✓ Connected successfully"
- "Clear All Data" button with confirmation dialog
- App version and privacy info at bottom

**Status:** 🔲 NOT STARTED

---

### Task 11: Navigation and App Integration
**Objective:** Wire together all screens with proper navigation, integrate Health Connect sync with dashboard, and ensure the complete MVP flow works.

**Implementation Details:**
- Set up Jetpack Compose Navigation:
  ```kotlin
  @Composable
  fun BioTraceApp() {
      val navController = rememberNavController()
      
      NavHost(navController, startDestination = "onboarding") {
          composable("onboarding") { OnboardingScreen(navController) }
          composable("permissions") { HealthConnectPermissionScreen(navController) }
          composable("dashboard") { DashboardScreen(navController) }
          composable("chat") { ChatScreen(navController) }
          composable("settings") { SettingsScreen(navController) }
      }
  }
  ```
- Implement onboarding flow:
  - Welcome screen explaining app purpose
  - Check if API key is configured → if not, prompt to go to Settings
  - Check if Health Connect permissions granted → if not, go to Permissions
  - If all setup, go to Dashboard
- Create bottom navigation:
  ```kotlin
  @Composable
  fun MainScreen() {
      Scaffold(
          bottomBar = { BottomNavigationBar(navController) }
      ) { padding ->
          NavHost(...) {
              composable("dashboard") { DashboardScreen() }
              composable("chat") { ChatScreen() }
              composable("settings") { SettingsScreen() }
          }
      }
  }
  ```
- Integrate sync with dashboard:
  - Dashboard observes sync status from `HealthSyncWorker`
  - Show last sync time in dashboard header
  - Pull-to-refresh triggers immediate sync
- Add error handling for missing setup:
  - Chat shows "Configure API key in Settings" if not set
  - Dashboard shows "Grant Health Connect permissions" if not granted
- Implement deep links for Health Connect:
  - Handle `ACTION_SHOW_PERMISSIONS_RATIONALE`
  - Handle `ACTION_SHOW_ONBOARDING`

**Test Requirements:**
- UI test: Navigation flows correctly between screens
- UI test: Onboarding completes successfully
- Integration test: Full user flow from launch to chat
- Unit test: Navigation destinations are correct
- End-to-end test: Complete MVP flow works

**Demo:** Complete app flow:
1. Fresh install launches to onboarding
2. User sets up Gemini API key in Settings
3. User grants Health Connect permissions
4. Dashboard shows with "Sync Now" option
5. User syncs data, sees vitals on dashboard
6. User opens Chat, asks health question
7. Assistant responds with streaming text and citations
8. User can navigate between all screens smoothly

**Status:** 🔲 NOT STARTED

---

### Task 12: Final Polish, Testing, and Debug Build
**Objective:** Complete end-to-end testing, polish UI/UX, add proper error handling, and create a distributable debug APK.

**Implementation Details:**
- Comprehensive error handling:
  - Network errors in LLM calls
  - Health Connect unavailable scenarios
  - Database errors
  - Permission denied scenarios
- UI polish:
  - Consistent Material 3 theming
  - Proper loading states for all async operations
  - Empty states for screens with no data
  - Error states with retry options
  - Accessibility: content descriptions, font scaling
- Performance optimization:
  - Lazy column for long lists
  - Proper Flow collection in ViewModels
  - Database indexes on timestamp and metric_type columns
- Add logging:
  ```kotlin
  object Logger {
      fun d(tag: String, message: String) { /* Debug logs */ }
      fun e(tag: String, message: String, error: Throwable?) { /* Error logs */ }
  }
  ```
- Create debug build:
  - Sign with debug keystore
  - Enable debugging
  - Include test data generation option
- Write comprehensive tests:
  - Achieve >70% code coverage on domain layer
  - All critical paths have integration tests
  - UI tests for main user flows
- Create README.md for the project:
  - Build instructions
  - Project structure
  - Development setup
  - Testing instructions

**Test Requirements:**
- Full test suite passes: `./gradlew test`
- All UI tests pass: `./gradlew connectedAndroidTest`
- Build succeeds: `./gradlew assembleDebug`
- Lint passes with no errors: `./gradlew lint`

**Demo:** Final MVP demonstrating:
1. Install debug APK on Android 14+ device
2. Launch app, see onboarding
3. Configure Gemini API key (can use test key)
4. Grant Health Connect permissions
5. Dashboard displays with sync option
6. Sync health data from Health Connect
7. View charts showing vitals trends
8. Open Chat, ask health questions
9. See streaming responses with citations
10. Navigate between all screens
11. Adjust settings, verify persistence
12. Restart app, verify data persists

**Status:** 🔲 NOT STARTED

---

## Progress Tracking

| Task | Status | Completion |
|------|--------|------------|
| Task 1: Project Scaffolding | ✅ COMPLETE | 100% |
| Task 2: Keystore Security | ✅ COMPLETE | 100% |
| Task 3: Room Database | ✅ COMPLETE | 100% |
| Task 4: Health Connect Permissions | ✅ COMPLETE | 100% |
| Task 5: Health Connect Sync | ✅ COMPLETE | 100% |
| Task 6: Dashboard UI | ✅ COMPLETE | 100% |
| Task 7: Gemini API Client | ✅ COMPLETE | 100% |
| Task 8: ReAct Agent | ✅ COMPLETE | 100% |
| Task 9: Chat UI | ✅ COMPLETE | 100% |
| Task 10: Settings Screen | ✅ COMPLETE | 100% |
| Task 11: Navigation | ✅ COMPLETE | 100% |
| Task 12: Final Polish | ✅ COMPLETE | 100% |

**Overall Progress: 12/12 tasks (100%)**
