# BioTrace 🛡️
### *Local-First, Zero-Server AI Health & Biomarker Intelligence for Android*

BioTrace is a private, zero-backend Android application that unifies wearable data from **Google Health Connect** (data comes from apps that write to Health Connect) with **blood/lab report parsing** and **meal photo logging**. It delivers deep biometric visualization and conversational health insights powered by client-side Large Language Models (LLMs) using your own API key (BYOK) or on-device AI.

All health data, raw time-series metrics, and Q&A histories remain strictly on your physical device in an encrypted SQLite/Room database. No personal cloud servers, Docker containers, or third-party relays are required.

---

## 🌟 Key Pillars

1. **Zero-Server Simplicity**: No self-hosted servers, Docker containers, or database instances to manage. Install the APK, grant permissions, and run everything locally.
2. **Direct Health Connect Sync**: Native ingestion of health data from apps that write to Health Connect (heart rate, HRV, sleep stages, steps, active calories, and SpO2).
3. **Clinical Lab & Blood Report Parsing**: Camera/gallery capture of paper or digital lab results. A multimodal Vision LLM extracts 50+ structured blood markers (CBC, metabolic panel, lipid panel, thyroid, vitamins) into normalized clinical units.
4. **Conversational AI Health Agent**: A streaming, record-grounded ReAct agent capable of correlating wearable sleep/HRV trends with blood markers (e.g., ferritin levels vs. resting HR trends) while citing verified local data.
5. **Photo Meal Logging (Smart Macro Scan)**: Snap a photo of a meal to estimate macronutrients, calories, and glycemic impact via Vision LLM.
6. **Air-Gapped Privacy & Keystore Security**: All metrics live in an on-device Room database. API keys are encrypted via Android Keystore (AES-256-GCM). Data only leaves the device when you explicitly invoke an LLM action.

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
- [ ] Scaffold Kotlin project with Jetpack Compose, Material 3, Hilt DI, and Coroutines/Flow.
- [ ] Implement `Room` local database (tables: `vitals_records`, `lab_biomarkers`, `meals`, `chat_history`).
- [ ] Implement `Android Keystore` security manager for AES-256-GCM encryption of user API keys.
- [ ] Integrate `HealthConnectClient` SDK:
  - Permission negotiation flow for Steps, Heart Rate, HRV, Sleep, and Activity.
  - Background synchronization worker via `WorkManager`.
- [ ] Build core Compose charting widgets (time-series line charts, sleep stage bars, daily step heatmaps).

### Phase 2: BYOK LLM Engine & Health Reasoning Agent
- [ ] Build unified client-side `LlmClient` supporting:
  - OpenAI-compatible API protocol (OpenAI, DeepSeek, Local Ollama).
  - Anthropic Messages API (Claude).
  - Google Gemini API.
- [ ] Implement Server-Sent Events (SSE) streaming consumer for real-time token rendering.
- [ ] Build the ReAct reasoning agent loop with strict guardrails:
  - Tool budget caps (maximum 5 turns, 6 tool calls).
  - Read-only tools for local Room queries.
  - Mandatory source citation blocks (`[Record #ID: Date, Value, Baseline]`).

### Phase 3: Vision LLM Lab & Blood Report Ingestion
- [ ] Build camera capture & document picker with edge detection and downsampling.
- [ ] Implement Vision LLM extraction prompt enforcing JSON-schema outputs for medical reports.
- [ ] Develop the `ClinicalSchemaNormalizer`:
  - 110+ clinical alias dictionaries.
  - 25+ certified medical unit conversions.
  - Interval parsing logic (`<` and `>` boundaries).
- [ ] Build interactive "Verification & Edit" UI for user confirmation before database write.
- [ ] Add biomarker trend charts showing historical lab results over time alongside healthy ranges.

### Phase 4: Food Vision & Nutritional Logging
- [ ] Create photo meal capture screen with multi-item dish segmentation prompt.
- [ ] Implement food identification and macro estimation pipeline (calories, protein, carbs, fats, fiber).
- [ ] Build daily macro tracker UI with target progress rings.
- [ ] Add cross-domain correlation: AI prompt injection comparing meal timing/macros against wearable sleep/HRV metrics.

### Phase 5: On-Device AI, Anomaly Alerts & Open Source Release
- [ ] Integrate on-device LLM inference via Google AICore / Gemini Nano Prompt API (Android 14+) and LiteRT for 100% offline summaries.
- [ ] Implement Three-Rule Anomaly Detection Engine (Out-of-range clinical flags, 180-day trend shifts, personal baseline median deviations).
- [ ] Add system calendar integration for medication and follow-up lab test reminders.
- [ ] Prepare reproducible F-Droid build pipeline, verify zero third-party telemetry, and publish initial v1.0.0 release.

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
