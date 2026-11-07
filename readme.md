# BioTrace 🛡️

BioTrace is a private application that unifies wearable data from **Google Health Connect** (data comes from apps that write to Health Connect) with **blood/lab report parsing** and **meal photo logging**. It delivers deep biometric visualization and conversational health insights powered by client-side Large Language Models (LLMs) using your own API key (BYOK) or on-device AI.

All health data, raw time-series metrics, and Q&A histories remain strictly on your physical device in an encrypted SQLite/Room database. No personal cloud servers, Docker containers, or third-party relays are required.

---

## 🏗️ Architecture Overview

```text
┌────────────────────────────────────────────────────────────────────────┐
│                          BioTrace Android App                       │
├────────────────────────────────────────────────────────────────────────┤
│ Presentation Layer (Jetpack Compose + Material 3)                      │
│  ├── Vitals & Wearable Dashboard (Time-Series Charts, Day/Week/Month)   │
│  ├── Lab & Biomarker Hub (Reference Range Gauges, Normalized Trends)   │
│  ├── Nutritional Log (Meal Scans, Macro Distribution)                  │
│  ├── Biometric Insights Hub (Overview, Correlations, Risks, AI Dive)   │
│  └── AI Chatbot & Assistant (Streaming SSE, Source Citations, ReAct)   │
├────────────────────────────────────────────────────────────────────────┤
│ Domain Layer                                                           │
│  ├── Health Connect Ingestion Service (Aggregation, Time Windows)      │
│  ├── Clinical Schema Normalizer (110+ Aliases, Unit Conversions)       │
│  ├── Clinical Safety Rule Engine (AHA/ACC, ADA, WHO, NSF Guidelines)   │
│  ├── Statistical & Time-Series Engine (SMA/EWMA, Circadian Dip, Corr)  │
│  ├── Predictive Risk Calculators (Atherogenic, Metabolic, Sleep Debt)  │
│  ├── Unsupervised Lifestyle Clusterer (K-Means Behavioral Archetypes)  │
│  ├── Multivariate Anomaly Engine (Compound Autonomic Strain Badges)    │
│  ├── Hybrid Safety Arbitration Engine (Medical Precedence & Citations) │
│  └── Health Agent Orchestrator (8 Read-Only Tools, Guardrails)         │
├────────────────────────────────────────────────────────────────────────┤
│ Data Layer (100% Local-First Storage & Secure Networking)              │
│  ├── Room Database (Vitals, Lab Records, Meals, Chat History)          │
│  ├── Android Keystore (AES-256-GCM encrypted BYOK keys)                │
│  └── Direct LLM Engine (OkHttp SSE: OpenAI / Gemini / Claude / Nano)   │
└────────────────────────────────────────────────────────────────────────┘
            ▲                                         │ (Explicit User Action)
            │ (On-Device IPC)                         ▼
┌───────────────────────┐                 ┌──────────────────────────────┐
│ Android Health Connect│                 │ Cloud LLM Providers / Nano   │
│   ▲                   │                 │  • Google Gemini             │
│   │ (Syncs Vitals)    │                 │  • Anthropic Claude          │
│ ┌───────────────────┐ │                 │  • OpenAI GPT-4o             │
│ │ Health Connect /  │ │                 │  • DeepSeek / Local Ollama   │
│ │ Smart Wearables   │ │                 │  • On-Device Gemini Nano     │
│ └───────────────────┘ │                 └──────────────────────────────┘
```

---

## 🔬 Core Features & Pipeline

### 1. Wearable Sync & Telemetry (Health Connect)
* **Background Ingestion**: Uses Android's `HealthConnectClient` to periodically aggregate:
  * Resting Heart Rate (RHR) and Continuous Heart Rate.
  * Heart Rate Variability (HRV / RMSSD).
  * Sleep architecture (Deep, Light, REM, Awake duration and sleep efficiency).
  * Daily activity (Steps, active calories, cadence, exercise sessions).
* **Interactive Visualization**: Native Jetpack Compose charts with zoom, pan, and custom aggregation windows (hourly, rolling 7-day, 30-day medians).

### 2. Multimodal Blood & Lab Report Parser
* **Report Capture**: In-app camera and document picker with on-device perspective correction and OOM-safe downsampling (max edge 1600px).
* **Vision Extraction**: Uses Vision LLMs in strict structured JSON mode to parse:
  * Metric Name, Raw String, Numerical Value, Unit, Standard Reference Interval, Flag (Normal, High, Low).
* **Clinical Schema Normalization** *(Inspired by HomeHealth)*:
  * **110+ Alias Mappings**: Automatically normalizes clinical name variations (e.g., `HbA1c`, `Glycated Hemoglobin`, `Hemoglobin A1c` → `HBA1C`).
  * **Strict Unit Normalization**: Standardizes 25+ unit spellings (e.g., `mg/dL` vs. `mmol/L`, `ng/mL` vs. `nmol/L`) using clinically verified conversion factors.
  * **Conservative Interval Handling**: Retains interval comparison operators (`<0.01` or `>100`) without clipping or guessing.
* **Human-in-the-Loop Verification**: Displays extracted markers in editable review cards with high/low visual badges before committing to the local Room database.

### 3. Nutrition & Meal Vision Scan
* **Instant Meal Analysis**: Snap a picture of your plate to estimate meal breakdown, estimated portion weights, calories, and macronutrients (Protein, Carbs, Fats, Fiber).
* **Correlation with Vitals**: Correlate high-sodium or late dinners with overnight resting HR and HRV fluctuations.

### 4. Record-Grounded AI Health Intelligence
* **ReAct Agent**: A local deterministic agent equipped with 8 read-only internal query tools:
  * `query_vitals_summary(metric, startDate, endDate)`
  * `query_lab_biomarkers(biomarkerName, count)`
  * `get_active_anomalies(memberId)`
  * `get_nutrition_intake(date)`
  * `query_correlation_matrix(metrics, windowDays)`
  * `query_clinical_risk_scores()`
  * `query_circadian_decomposition(metricType)`
  * `query_lifestyle_clusters(lookbackDays)`
* **Streaming SSE Chat**: First-token streaming display via Server-Sent Events.
* **Data Basis Citations**: Before emitting conclusions, the assistant previews the exact database records and timestamp ranges backing the recommendation.

### 5. Deterministic Statistical Intelligence & Clinical Rule Engine
* **Clinical Safety Rules**: Real-time evaluation of vitals (Resting HR, SpO2) and clinical blood biomarkers (glycemic, lipid panels, hematology) against AHA/ACC, ADA, WHO, and National Sleep Foundation thresholds.
* **Longitudinal Trends & Demographic Percentiles**: Exponential moving averages (EWMA), median absolute deviation (MAD), week-over-week z-score shifts, and Cooper Institute / population HRV percentiles.
* **External Medical Benchmarking**: Automated compliance tracking against WHO physical activity guidelines (steps, active burn) and National Sleep Foundation sleep stage architecture (REM 20-25%, Deep 15-20%).

### 6. Multi-Metric Correlation & Circadian Time-Series Decomposition
* **Automated Correlation Matrix**: Pairwise Pearson ($r$) and Spearman rank ($\rho$) correlation engine with Student's t-test p-value significance testing across vitals, meals, and lab markers with day-lag alignment.
* **Circadian Diurnal Decomposition**: 24-hour diurnal profiling, nocturnal heart rate dipping percentage ($\frac{\text{Day Mean} - \text{Night Min}}{\text{Day Mean}} \times 100\%$), and autonomic dipping pattern classification (`EXTREME_DIPPER`, `NORMAL_DIPPER`, `NON_DIPPER`, `REVERSE_DIPPER`).
* **Dynamic Visualizations**: Jetpack Compose Canvas scatter plots with linear regression trendlines ($y = mx + b$) and chronobiology diurnal curve cards.

### 7. Predictive Clinical Risk Calculators & Lifestyle Clustering
* **Available-Data Clinical Risk Models** (strictly zero blood pressure dependencies):
  * **Atherogenic Dyslipidemia Triad**: TG/HDL ratio, Non-HDL cholesterol, and TC/HDL ratios.
  * **Harmonized Metabolic Syndrome Index**: 4-criteria metabolic syndrome assessment (Fasting Glucose, Triglycerides, HDL, BMI).
  * **Sleep Debt & Allostatic Strain**: Rolling 14-day cumulative sleep deficit balanced against autonomic strain from HRV RMSSD depression.
* **Lifestyle Pattern Clustering**: On-device K-Means algorithm vectorizing daily habits into behavioral archetypes (`PEAK_PERFORMANCE`, `DEEP_RECOVERY`, `STRAIN_DEPLETED`, `SEDENTARY_MAINTENANCE`).
* **In-App Compound Anomaly Engine**: Synchronous detection of acute multi-system strain (concurrent HRV drop >1.8σ, RHR spike >1.5σ, and curtailed sleep <6h) surfaced strictly as in-app badges.

### 8. Biometric Insights Hub & Hybrid Safety Arbitration
* **4-Tab Bento Navigation**:
  * *Overview & Longevity*: Biological Age, Profile Triptych, Daily Readiness.
  * *Correlations & Diurnal*: Scatter plot, discovered correlations, Circadian Dipping curves, Lifestyle Archetype cards.
  * *Clinical Risks & Safety*: Radial lipid triad gauges, metabolic checklist, sleep debt strain, in-app anomaly badges.
  * *AI Deep Dive*: Streaming health report with evidence citations.
* **Hybrid Safety Arbitration**: Injects clinical safety priority, ensuring validated medical alerts take visual precedence over LLM generative responses.

---

## 🛣️ Phased Development Roadmap

### Phase 1: Foundation, Local Storage & Health Connect Ingestion
- [x] Scaffold Kotlin project with Jetpack Compose, Material 3, Hilt DI, and Coroutines/Flow.
- [x] Implement `Room` local database (tables: `vitals_records`, `lab_biomarkers`, `meals`, `chat_history`).
- [x] Implement `Android Keystore` security manager for AES-256-GCM encryption of user API keys.
- [x] Integrate `HealthConnectClient` SDK (permission flow & background worker).
  - Permission negotiation flow for Steps, Heart Rate, HRV, Sleep, and Activity.
  - Background synchronization worker via `WorkManager`.
- [x] Build core Compose charting widgets (time-series line charts, sleep stage bars, daily step heatmaps).

### Phase 2: BYOK LLM Engine & Health Reasoning Agent
- [x] Build unified client-side `LlmClient` supporting:
  - OpenAI-compatible API protocol (OpenAI, DeepSeek, Local Ollama).
  - Anthropic Messages API (Claude).
  - Google Gemini API.
- [x] Implement Server-Sent Events (SSE) streaming consumer for real-time token rendering.
- [x] Build the ReAct reasoning agent loop with strict guardrails:
  - Tool budget caps (maximum 5 turns, 6 tool calls).
  - Read-only tools for local Room queries.
  - Mandatory source citation blocks (`[Record #ID: Date, Value, Baseline]`).

### Phase 3: Vision LLM Lab & Blood Report Ingestion
- [x] Build camera capture & document picker with edge detection and downsampling.
- [x] Implement Vision LLM extraction prompt enforcing JSON-schema outputs for medical reports.
- [x] Develop the `ClinicalSchemaNormalizer`:
  - 110+ clinical alias dictionaries.
  - 25+ certified medical unit conversions.
  - Interval parsing logic (`<` and `>` boundaries).
- [x] Build interactive "Verification & Edit" UI for user confirmation before database write.
- [x] Add biomarker trend charts showing historical lab results over time alongside healthy ranges.

### Phase 4: Food Vision & Nutritional Logging
- [x] Create photo meal capture screen with multi-item dish segmentation prompt.
- [x] Implement food identification and macro estimation pipeline (calories, protein, carbs, fats, fiber).
- [x] Build daily macro tracker UI with target progress rings.
- [x] Add cross-domain correlation: AI prompt injection comparing meal timing/macros against wearable sleep/HRV metrics.

### Phase 5: Deterministic Statistical Intelligence & Clinical Rule Engine
- [x] Implement deterministic `ClinicalSafetyRuleEngine` evaluating vitals and blood biomarkers against validated medical guidelines (AHA/ACC, ADA, WHO, National Sleep Foundation).
- [x] Build `RollingStatsCalculator` computing SMA, EWMA, sample standard deviation, median, and median absolute deviation (MAD).
- [x] Build `LongitudinalShiftEvaluator` measuring week-over-week rate-of-change and statistical z-scores.
- [x] Implement `DemographicPercentileEngine` mapping RHR and HRV against age- and sex-stratified population cohorts.
- [x] Implement external medical benchmarks: `WhoActivityBenchmark` and `SleepArchitectureBenchmark`.

### Phase 6: Multi-Metric Biometric Correlation & Time-Series Decomposition
- [x] Build automated on-device `CorrelationMatrixCalculator` computing Pearson $r$ and Spearman rank $\rho$ with Student's t-test p-values.
- [x] Build `MetricPairwiseCorrelator` for time-series alignment with configurable day lags (e.g., night sleep vs next-day activity).
- [x] Implement 24-hour `CircadianDecompositionService` and `DiurnalDipCalculator` classifying autonomic dipping patterns (`EXTREME_DIPPER`, `NORMAL_DIPPER`, `NON_DIPPER`, `REVERSE_DIPPER`).
- [x] Create Jetpack Compose Canvas components: `CorrelationScatterPlot` (linear regression best-fit line) and `CircadianRhythmCard`.

### Phase 7: Predictive Health Risk Calculators & Lifestyle Pattern Clustering
- [x] Implement available-data clinical risk models (strictly zero blood pressure dependencies):
  - `AtherogenicRiskCalculator`: Lipid triad indices (TG/HDL, Non-HDL, TC/HDL).
  - `HarmonizedMetabolicRiskCalculator`: 4-criteria harmonized metabolic syndrome index.
  - `SleepDebtCalculator`: 14-day cumulative sleep deficit balanced against autonomic strain from HRV depression.
- [x] Develop on-device unsupervised `LifestyleKMeansClusterer` vectorizing daily habits into behavioral archetypes (`PEAK_PERFORMANCE`, `DEEP_RECOVERY`, `STRAIN_DEPLETED`, `SEDENTARY_MAINTENANCE`).
- [x] Implement in-app `evaluateCompoundStrain` in `AnomalyDetectionEngine` for simultaneous HRV depression, RHR elevation, and sleep fragmentation.

### Phase 8: Hybrid Grounded AI Intelligence, ReAct Tool Expansion & UI Hub Overhaul
- [x] Expand ReAct agent toolset (`HealthAgent`) with 4 read-only analytical query tools:
  - `query_correlation_matrix`
  - `query_clinical_risk_scores`
  - `query_circadian_decomposition`
  - `query_lifestyle_clusters`
- [x] Implement `InsightArbitrator` enforcing medical priority banners and grounded citations over LLM generative responses.
- [x] Overhaul `BiometricInsightsScreen` and `BiometricInsightsViewModel` with a 4-tab Bento interface (`Overview & Longevity`, `Correlations & Diurnal`, `Clinical Risks & Safety`, `AI Deep Dive`).
- [x] Build `RiskScoreGaugeCard` and `LifestyleArchetypeCard` UI widgets.

## 🔒 Privacy & Threat Model

* **No Middleman Server**: There is no backend service, telemetry daemon, and cloud syncing proxy. The source code contains zero analytics SDKs.
* **Encrypted Secrets**: API keys never sit in plaintext in `SharedPreferences`; they are encrypted in hardware‑backed Android Keystore storage.
* **Backup Immunity**: `android:allowBackup="false"` is enforced to prevent unencrypted health record extraction via ADB or cloud device backups.
* **Granular Data Transmission**: Network access is restricted exclusively to user‑configured LLM provider endpoints. Health data is only sent in transient payloads when the user clicks "Analyze" or sends a chat query.

## 🛠️ Tech Stack & Requirements

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.3+ |
| **UI Framework** | Jetpack Compose + Material 3 (Single Activity) |
| **Architecture** | MVVM + Clean Architecture (`ui`, `domain`, `data`) |
| **Dependency Injection** | Hilt |
| **Local Persistence** | Android Room v7 (SQLite) |
| **Wearable Pipeline** | Android Health Connect SDK (androidx.health.connect) |
| **Networking** | OkHttp 4 (raw SSE streaming, JSON mode validation) |
| **Security** | Android Keystore (AES‑256‑GCM cipher) |
| **Min SDK** | Android 8.0 (API 26) — *Recommended Android 14+ for system Health Connect* |

## 🔮 Future Roadmap (Backlog)

* **100% Air‑Gapped Edge AI**: Integrate `LiteRT` / MediaPipe LLM Inference API to run quantized local models (Gemma 3 / Qwen) on‑device alongside local PDF/ML Kit OCR pre‑processing for zero‑network lab parsing.
* **Fast Nutrition & Barcode Logging**: OpenFoodFacts barcode scanner, "Saved Meal" templates for recurring daily staples, and bidirectional write‑back (`NutritionRecord`) to Android Health Connect.
* **Zero‑Knowledge Vault & Clinical Exports**: Passphrase‑encrypted (Argon2id + AES‑256‑GCM) database export/import via Android Storage Access Framework, one‑tap Physician PDF summary generation, and Jetpack Glance home‑screen widgets.

## 📊 Supported Data Schema

| Source | Supported Metrics & Records | Ingestion Method |
| :--- | :--- | :--- |
| **Health Connect** | Heart Rate (`HeartRateRecord`), HRV (`HeartRateVariabilityRmssdRecord`), Sleep Stages (`SleepSessionRecord`), Steps (`StepsRecord`), Active Calories (`ActiveCaloriesBurnedRecord`), SpO2 (`OxygenSaturationRecord`), Weight (`WeightRecord`) | Automated Background `WorkManager` Sync |
| **Blood & Lab Panels** | Complete Blood Count (CBC), Comprehensive Metabolic Panel (CMP), Lipid Panel, Thyroid (TSH, Free T3/T4), Vitamins (D, B12, Folate), Iron/Ferritin, Hormones & Inflammation (`hs‑CRP`, `HbA1c`) | Camera / Document Picker → Vision LLM → `ClinicalSchemaNormalizer` |
| **Nutrition & Meals** | Calories, Protein, Carbohydrates, Dietary Fats, Fiber, Meal Timing, Multi‑Item Dish Breakdown | In‑App Meal Vision Scan & Manual Entry |
| **Computed & Intelligence** | Daily Readiness Score, Biological Age (PhenoAge), Rolling 30-Day Baselines (SMA/EWMA/MAD), Atherogenic Lipid Triad (TG/HDL, Non-HDL), Harmonized Metabolic Risk Score, Circadian Diurnal Dip %, Cumulative Sleep Debt & Autonomic Strain, Lifestyle Archetypes (K-Means), Pearson/Spearman Cross-Correlations | Local Domain Calculators & Room Entities |

## 📄 License
This project is licensed under the **GNU General Public License v3.0 (GPL‑3.0)** or **MIT License**.
