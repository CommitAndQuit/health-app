# BioTrace 🛡️
### *Local-First, Zero-Server AI Health & Biomarker Intelligence for Android*

BioTrace is a private, zero-backend Android application that unifies wearable data from **Google Health Connect** (data comes from apps that write to Health Connect) with **blood/lab report parsing** and **meal photo logging**. It delivers deep biometric visualization and conversational health insights powered by client-side Large Language Models (LLMs) using your own API key (BYOK) or on-device AI.

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
│  └── AI Chatbot & Insights (Streaming SSE, Source Citations, ReAct)   │
├────────────────────────────────────────────────────────────────────────┤
│ Domain Layer                                                           │
│  ├── Health Connect Ingestion Service (Aggregation, Time Windows)      │
│  ├── Clinical Schema Normalizer (110+ Aliases, Unit Conversions)       │
│  ├── Three-Rule Anomaly Engine (Out-of-Range, Trends, Personal Base)   │
│  └── Health Agent Orchestrator (Tool Calling, Prompt Injection Guard)  │
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
* **ReAct Agent**: A local deterministic agent equipped with 4 read-only internal query tools:
  * `query_vitals_summary(metric, startDate, endDate)`
  * `query_lab_biomarkers(biomarkerName, count)`
  * `get_active_anomalies(memberId)`
  * `get_nutrition_intake(date)`
* **Streaming SSE Chat**: First-token streaming display via Server-Sent Events.
* **Data Basis Citations**: Before emitting conclusions, the assistant previews the exact database records and timestamp ranges backing the recommendation.

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
---

## 🔒 Privacy & Threat Model

* **No Middleman Server**: There is no backend service, telemetry daemon, or cloud syncing proxy. The source code contains zero analytics SDKs.
* **Encrypted Secrets**: API keys never sit in plaintext in `SharedPreferences`; they are encrypted in hardware-backed Android Keystore storage.
* **Backup Immunity**: `android:allowBackup="false"` is enforced to prevent unencrypted health record extraction via ADB or cloud device backups.
* **Granular Data Transmission**: Network access is restricted exclusively to user-configured LLM provider endpoints. Health data is only sent in transient payloads when the user clicks "Analyze" or sends a chat query.

---

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
| **Security** | Android Keystore (AES-256-GCM cipher) |
| **Min SDK** | Android 8.0 (API 26) — *Recommended Android 14+ for system Health Connect* |

---

## 📄 License
This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** or **MIT License**.

## Future RoadMap

### Insights

**Possible ways to generate health insights**

- **Rule‑based thresholds** – simple if/else checks against medically‑validated cut‑offs.
- **Statistical trends & percentiles** – moving averages, week‑over‑week change, age‑group percentile comparisons.
- **Correlation & regression analysis** – Pearson/Spearman correlation or linear regression between metrics.
- **Time‑series decomposition** – isolate trend, seasonality, residual (e.g., circadian HRV patterns).
- **Predictive / risk models** – lightweight on‑device models (logistic regression, gradient‑boosted trees) to produce risk scores.
- **Clustering / pattern discovery** – unsupervised grouping of daily feature vectors.
- **Anomaly detection** – statistical z‑score or Isolation Forest to flag outliers.
- **Large Language Model (LLM) narrative** – LLM‑driven health report (already implemented as the “AI Health Report” card).
- **Hybrid rule + ML** – combine safety‑critical alerts with ML‑driven personalization.
- **External reference integration** – compare against public benchmarks (WHO step recommendation, etc.).

**Implemented in the current codebase**

- LLM‑generated health report (`AiReportCard` & `GenerateHealthReportUseCase`).
- Simple correlation example (Sleep & Glycemic Energy Expenditure) in `BiometricInsightsViewModel`.
- Rule‑based alerts are not yet present but can be added in future phases.

## Available Data

### Health Connect
- **Heart Rate** – `HeartRateRecord`
- **Heart Rate Variability (HRV)** – `HeartRateVariabilityRmssdRecord`
- **Steps** – `StepsRecord`
- **Sleep** – `SleepSessionRecord`
- **Active Calories Burned** – `ActiveCaloriesBurnedRecord`
- **Oxygen Saturation** – `OxygenSaturationRecord`
- **Weight** – `WeightRecord`

### Blood Report (Lab Results)
- Parsed via Vision LLM and normalized by `ClinicalSchemaNormalizer`. Includes CBC, metabolic panel, lipid panel, thyroid, vitamins, etc. (see `domain/clinical` package).

### Manual Reports
- User‑entered entries such as **Weight** and **Height** entered directly in the app UI (stored in local Room tables).
