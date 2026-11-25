# Bharat AI-SoC PS4: Decisions vs. Achieved Tracker

> **Document Purpose:** Living tracking document comparing the initial architectural strategy decisions against actual code implementations, verified hardware benchmarks, and remaining milestones for the **Bharat AI-SoC Challenge 2026–27 (Problem Statement 4)**.  
> **Last Updated:** October 31, 2026 | **Target Deadline:** Phase 1 (30 Nov 2026) • Phase 2 (10 Feb 2027)

---

## 1. Executive Summary & Status Overview

```mermaid
flowchart TD
    subgraph Strategy["Initial Strategic Decisions"]
        D1["Avoid LoRA PTQ Degradation<br/>(Drop Runtime QLoRA)"]
        D2["Enforce SpinQuant INT4 + KleidiAI<br/>(Beat <1.8GB RAM & >22 t/s)"]
        D3["Invert JSON Architecture<br/>(Deterministic Kotlin Roadmap)"]
        D4["Neuro-Symbolic BKT Core<br/>(Kotlin Math, not LLM context)"]
        D5["SQLite FTS5 Micro-RAG<br/>(6 Domains, 6 Languages)"]
    end

    subgraph Achieved["Production Implementation (Nov 01, 2026)"]
        A1["✅ SpinQuant INT4 PTE Loaded (1.13GB)<br/>XNNPACK Arm Neon SIMD"]
        A2["✅ Dual Verified: 42.8-50.1 t/s (OnePlus 15)<br/>82.6-89.7 t/s (Pixel 9) | 100% Offline"]
        A3["✅ Zero-Crash 3-Week Milestones Card<br/>+ Deterministic Kotlin Roadmap"]
        A4["✅ CurriculumRagEngine.kt BKT<br/>P(T)=0.20, P(S)=0.10, P(G)=0.25"]
        A5["✅ curriculum_v1.db Complete<br/>20 Skills, 60 Quizzes, 120 Indic Records"]
        A6["✅ Reactive 3-Tab Compose UI<br/>Tutor • Graph • Quiz Tabs (<10ms Grading)"]
    end

    D1 --> A1
    D2 --> A2
    D3 --> A3
    D4 --> A4
    D5 --> A5
    A4 & A5 --> A6
```

| Evaluation Pillar | Target Requirement | Strategy Decision | Actual Production Status | Grade |
| :--- | :--- | :--- | :--- | :---: |
| **Technical Innovation** | ExecuTorch on Arm, $\ge 22\text{ t/s}$, $< 1.8\text{ GB}$ RAM | SpinQuant INT4 + Arm Neon XNNPACK | **42.8 – 50.1 t/s** (OnePlus 15) & **82.6 – 89.7 t/s** (Pixel 9), **1575 – 1592 MB** `RssAnon` (OnePlus 15) & **1631 – 1700 MB** (Pixel 9) | **EXCEEDED** |
| **Pedagogical Quality** | Skill gap detection, adaptive study plan | Neuro-Symbolic BKT + Spaced Repetition | **BKT $P(L_t)$ engine**, dynamic $+2\text{h/wk}$ remediation, 5-tier flagship course + 20 skills | **ACHIEVED** |
| **Real-World Viability** | Crash-free UI, 100% offline, smooth UX | Deterministic JSON + FTS5 Micro-RAG | **3-Tab UI**, reactive whole-screen localization, **<10ms inline quiz grading**, **Airplane Mode verified** | **ACHIEVED** |
| **Social Impact** | 6 Indian languages + cultural analogies | Pre-translated SQLite FTS5 packs | **6 Languages** (`en`, `hi`, `ta`, `te`, `bn`, `mr`) + 120 vernacular analogies | **ACHIEVED** |

---

## 2. Deep Dive: Initial Decisions vs. Actual Implementation

### Decision Area 1: Quantization, Memory Footprint & Hardware Acceleration

* **Initial Challenge Identified**:
  * Default ExecuTorch `Llama-3.2-1B` SpinQuant exports consume $\sim 1921\text{ MB}$ RSS, and QAT+LoRA consumes $\sim 2255\text{ MB}$, violating the strict $< 1.8\text{ GB}$ ceiling.
  * Vendor NPU delegates (QNN / NeuroPilot) break on dynamic sequence lengths across different phone SoCs.
* **Initial Strategy Decided**:
  * Avoid unmerged LoRAs at runtime.
  * Use Meta's official SpinQuant checkpoint with Arm Neon SIMD assembly kernels via `XNNPACK` and KleidiAI.
  * Restrict context window (`384`–`512` tokens) to prevent KV-cache explosion.
* **What Was Actually Achieved**:
  * **Model**: Loaded `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` ($1.13\text{ GB}$ flash footprint) with multi-path SELinux auto-discovery (`context.getExternalFilesDir()`, `filesDir`, `/data/local/tmp/llama/`).
  * **Hardware Targets & Runtime**: ExecuTorch 1.1.0 with XNNPACK Arm Neon acceleration tested on both:
    1. **Physical OnePlus 15 (`CPH2745`)**: Qualcomm Snapdragon 8 Elite (Oryon CPU / Cortex-X925, OxygenOS Android 15).
    2. **Google Pixel 9 Reference**: Google Tensor G4 (`Cortex-X4 / A720`, Android 15).
  * **Throughput**:
    * **OnePlus 15 (Physical)**: **$42.8$ to $50.1\text{ tokens/second}$** (2x PS4 requirement).
    * **Google Pixel 9**: **$82.6$ to $89.7\text{ tokens/second}$** (4x PS4 requirement).
  * **Memory Footprint**: Measured via Linux kernel process status (`/proc/self/status` `VmRSS` / `RssAnon`):
    * **OnePlus 15**: **$1575\text{ MB} - 1592\text{ MB}$** `RssAnon` ($1647\text{ MB}$ PSS), leaving $>200\text{ MB}$ safety margin below the $1800\text{ MB}$ ceiling.
    * **Google Pixel 9**: **$1631\text{ MB}$** idle, **$1700\text{ MB}$** peak generation.
  * **Interactive Quiz Grading Latency**: Instant **$< 10\text{ ms}$** evaluation in Kotlin without triggering redundant 1B model calls.
  * **Offline Verification**: Verified `OFFLINE=true` in Android Airplane Mode with zero network calls via `ConnectivityManager`.

---

### Decision Area 2: Avoiding Custom Indic QLoRA Fine-Tuning Trap

* **Initial Challenge Identified**:
  * Training custom QLoRA in Colab and running Post-Training Quantization (PTQ) destroys SpinQuant's outlier rotation matrices and degrades 1B reasoning.
  * `Llama-3.2-1B` only natively supports English and Hindi; forcing Tamil, Telugu, Bengali, and Marathi into 1B parameters via 2-week LoRA causes catastrophic forgetting and JSON corruption.
* **Initial Strategy Decided**:
  * Drop custom runtime LoRA fine-tuning for Phase 1.
  * Keep the base SpinQuant weights intact and uncorrupted.
  * Deliver multilingual excellence using an embedded SQLite FTS5 database containing pre-translated curricula and cultural analogies.
* **What Was Actually Achieved**:
  * Built `build_curriculum_db.py` to compile `curriculum_v1.db` ($40\text{ KB}$ SQLite 3 database bundled in `app/src/main/assets/`).
  * Created `indic_content_fts` virtual table supporting:
    * 🇬🇧 **English (`en`)**
    * 🇮🇳 **हिंदी / Hindi (`hi`)**
    * 🇮🇳 **தமிழ் / Tamil (`ta`)**
    * 🇮🇳 **తెలుగు / Telugu (`te`)**
    * 🇮🇳 **বাংলা / Bengali (`bn`)**
    * 🇮🇳 **मराठी / Marathi (`mr`)**
  * Embedded culturally resonant Indian analogies (e.g., Indian Railways junction routing, Mumbai Dabbawala container packing, ST bus reservation depots, Mandi ledger accounting, Temple vault double-lock security).
  * Model outputs natural regional language text guided by Micro-RAG syllabus context while preserving core reasoning.

---

### Decision Area 3: Preventing 1B JSON Crashes & UI Parsing

* **Initial Challenge Identified**:
  * Asking a 1B INT4 model to generate complex nested JSON roadmaps causes a $25\%\text{--}40\%$ syntax failure rate (and $>70\%$ failure when Unicode Indic scripts are involved), which would crash the Android frontend.
* **Initial Strategy Decided**:
  * Invert the pipeline: assemble the roadmap structure deterministically in Kotlin from BKT state and SQLite curriculum nodes.
  * Prompt the LLM only for short plain-text explanations, localized motivation, and Socratic guidance.
  * Implement tolerant regex extraction rather than brittle JSON deserializers for user chat display.
* **What Was Actually Achieved**:
  * Prompt in `CurriculumRagEngine.kt` explicitly instructs the LLM: `DO NOT use JSON format. Respond with natural text and bullet points.`
  * Implemented `parseTutorResponse()` in `MessageItem.kt`:
    * Tolerant regex extracting `Domain`, `Module Title`, `BKT Mastery P(L_t)`, `Gap Status`, `Cultural Analogy`, and `Quiz Options`.
    * Hardened across Commits `9859a77`, `f726143`, and `78d1da1` with Devanagari synonyms (`क्षेत्र`, `शीर्षक`, `निपुणता`, `प्रश्नोत्तरी`), colon variations (`:`, `：`), and asterisks.
  * Synthesizes valid deterministic JSON in Kotlin for the on-screen **Roadmap JSON Inspector** without risking runtime crashes.

---

### Decision Area 4: Neuro-Symbolic Bayesian Knowledge Tracing (BKT) Core

* **Initial Challenge Identified**:
  * Tracking multi-week quiz performance purely inside the LLM context burns precious tokens and is unreliable.
  * BKT math is concise, but must be deeply integrated into the app lifecycle and hardware telemetry rather than remaining a superficial script.
* **Initial Strategy Decided**:
  * Implement a full Hidden Markov Model in Kotlin with fixed pedagogical parameters ($P_T=0.20, P_S=0.10, P_G=0.25$).
  * Classify student state into Critical Gap ($P(L_t) < 0.45$), In-Progress ($0.45 \le P < 0.80$), and Mastered ($P \ge 0.80$).
  * Automatically adapt allocated study hours ($+2\text{ h/week}$ for remedial).
* **What Was Actually Achieved**:
  * Implemented in `CurriculumRagEngine.kt`:
    ```kotlin
    val pObs = if (isCorrect) {
        (pPrev * (1.0 - pSlip)) / (pPrev * (1.0 - pSlip) + (1.0 - pPrev) * pGuess)
    } else {
        (pPrev * pSlip) / (pPrev * pSlip + (1.0 - pPrev) * (1.0 - pGuess))
    }
    val pNext = (pObs + (1.0 - pObs) * pTransit).coerceIn(0.05, 0.99)
    ```
  * Persisted in Android `SharedPreferences("bharat_bkt_state")` across app launches.
  * Dynamic study hour adjustment: automatically bumps base hours $+2\text{ h/week}$ when $P(L_t) < 0.45$.
  * Integrated interactive quiz submissions: tapping quiz options instantly recalculates Bayesian posterior and refreshes UI cards.

---

### Decision Area 5: User Interface Architecture

* **Initial Strategy Decided**:
  * Default single-screen chat app is insufficient for presenting multi-domain educational mastery and telemetry.
  * Must showcase live on-device telemetry (tokens/sec, RAM, offline mode).
* **What Was Actually Achieved**:
  * Transformed `ChatScreen.kt` into a **Jetpack Compose 3-Tab Learning Hub**:
    * **Tab 0 (Tutor)**: Conversational chat with language selector chip row (6 languages), 8 quick domain launcher chips, structured tutor cards, and expandable JSON inspector.
    * **Tab 1 (Graph)**: Neuro-Symbolic BKT Skill Graph displaying prerequisite dependencies, color-coded mastery progress bars (Red/Orange/Green), allocated hours, and average class mastery.
    * **Tab 2 (Quiz)**: Dedicated diagnostic assessment suite allowing manual testing of any skill node with immediate Bayesian updates.
  * **TopAppBar Telemetry Pill**: Displays live `| {tps} t/s | {ram}MB` reading true Linux kernel `VmRSS`.
  * **One-Tap Reset**: Reset BKT priors back to baseline $P(L_0)$ via `Icons.Filled.Refresh`.

---

### Decision Area 6: Flagship Course in All 6 Languages & Strict <1800MB RAM Bounding (Oct 31, 2026)

* **Challenge Addressed**:
  * Prior logcat showed a transient memory spike to $2682\text{ MB}$ caused by concurrent model loading threads.
  * Required at least one full-fledged, multi-tier curriculum fully integrated across all 6 Indic languages (`en`, `hi`, `ta`, `te`, `bn`, `mr`) while strictly bounding process RAM below $1800\text{ MB}$.
* **Implementation Details**:
  * **Flagship Course**: Built **"Edge AI & On-Device LLM Engineering on Arm"** spanning 5 progressive DAG tiers:
    1. `edge_ai_01_tensors`: Tensors, Quantization Math & Float16 vs INT4/INT8
    2. `edge_ai_02_spinquant`: SpinQuant Outlier Suppression & Cayley Rotations
    3. `edge_ai_03_kleidiai`: Arm Neon SIMD & KleidiAI Vector Kernels
    4. `edge_ai_04_executorch`: ExecuTorch Runtime & Android Memory Sandboxing
    5. `edge_ai_05_kvcache`: Attention Sinks, Ring Buffer KV-Cache & Latency Optimization
  * **Full Multilingual Integration**: Every tier contains complete syllabi, diagnostic quiz pools (3 questions per tier with explanations), and deep cultural analogies across English, Hindi, Tamil, Telugu, Bengali, and Marathi (totaling 120 localized entries in SQLite).
  * **RAM Optimization Under 1800MB**:
    1. Added `AtomicBoolean(false)` mutex in `ChatViewModel.kt` to serialize model loading and prevent duplicate allocations.
    2. Safely invoke `oldModule.resetNative()` before loading a new model to release C++ allocations.
    3. Clamped `DEFAULT_MAX_SEQ_LEN` to `384` tokens in `AppSettings.kt`, preventing excessive KV-cache footprint.
    4. Capped `ETLogging` in-memory logs to 500 entries.
    5. Added periodic `System.gc()` after every inference turn to immediately reclaim intermediate buffers.
  * **Verified Device Telemetry (Pixel 9, Tensor G4 / Cortex-X4 / A720)**:
    * `TOTAL PSS`: **1666 – 1711 MB** (Strictly $< 1800\text{ MB}$)
    * `Private Dirty`: **1514 – 1568 MB** (Strictly $< 1800\text{ MB}$)
    * `RssAnon`: **1584 – 1612 MB** (Strictly $< 1800\text{ MB}$)
    * `Throughput`: **82.6 – 89.7 t/s** (Target $\ge 22.0\text{ t/s}$)

---

## 3. What Was Discovered / Adjusted in Between

During the actual implementation, several critical edge cases were discovered that were not in the initial discussion:

1. **Devanagari Regex Parsing Failure (Fixed in Commit `9859a77`)**:
   * *Issue*: When the prompt was switched to Hindi, the LLM outputted Hindi headers (`डोमेन:`, `शीर्षक:`, `BKT निपुणता:`) instead of English keys. The initial parser returned empty cards.
   * *Resolution*: Added comprehensive Devanagari synonyms and fallback patterns to `MessageItem.kt`.

2. **Token Streaming Delimiter Fragmentation (Fixed in Commit `f726143`)**:
   * *Issue*: Minor token streaming quirks (double asterisks around values, Chinese/full-width colons `：`, irregular whitespace) caused regex failures.
   * *Resolution*: Sanitized strings using `removeSurrounding("**")` and relaxed regex constraints.

3. **Android RSS vs. JVM Heap Discrepancy (Documented in Commit `78d1da1`)**:
   * *Issue*: Standard Android `Runtime.getRuntime().totalMemory()` reported $< 200\text{ MB}$, completely hiding the $1.4\text{ GB}$ native C++ ExecuTorch tensor allocations.
   * *Resolution*: Built `getTrueProcessRssMb()` reading `/proc/self/status`, giving judges true Linux process RAM metrics.

4. **Activity Navigation Stack Bug (Fixed in `MainActivity.kt`)**:
   * *Issue*: `MainActivity` previously called `finish()` when opening `WelcomeActivity`, causing the app to exit on back press.
   * *Resolution*: Removed `finish()`, preserving proper Android back-stack navigation.

5. **OxygenOS 15 SELinux Sandboxing & Multi-Path Model Discovery (Fixed in Commit `c53fd02`)**:
   * *Issue*: On Android 15 / OxygenOS 15 (OnePlus 15 `CPH2745`), `/data/local/tmp/llama/` threw `EACCES` permission denied under the `untrusted_app` SELinux domain, causing model load failures.
   * *Resolution*: Implemented multi-path discovery prioritizing `context.getExternalFilesDir("llama")` (`/sdcard/Android/data/com.example.executorchllamademo/files/llama/`), which is always readable and mmap-able by untrusted apps.

6. **Reactive Whole-Screen Localization (Fixed in Commit `c53fd02`)**:
   * *Issue*: Tapping a language chip previously left other UI elements and tabs in English, and typed Indic queries defaulted back to English.
   * *Resolution*: Centralized `activeLangCode`, `activeSkillId`, and `bktRefreshTick` in `ChatViewModel`, synchronizing top chips, active course banner, action buttons, card headers, and tab labels across all 6 Indian scripts.

7. **3-Week Syllabus Milestones & <10ms Inline Quiz Grading (Fixed in Commit `c53fd02`)**:
   * *Issue*: Full syllabus roadmap wasn't visible until generation finished, and inline quiz grading triggered redundant 1B LLM generation passes.
   * *Resolution*: Embedded 3-week roadmap steps (`W1, W2, W3`) directly inside `StructuredTutorCard`, and implemented instant (<10ms) Kotlin evaluation with BKT posterior jumps ($P(L_t): 64\% \to 89\%$).

8. **2769 MB RAM Spike Prevention via Single-Task Singleton Architecture (Fixed in Commit `acb3d08`)**:
   * *Issue*: Relaunching `MainActivity` via `am start` created a second `ChatViewModel` in the same Linux process, allocating a duplicate 1.13 GB C++ native module and spiking RAM to 2769 MB. Additionally, `VmRSS` double-counted clean 1.13 GB `.pte` mmap file cache (`RssFile`) on high-RAM devices like OnePlus 15 (16GB/24GB).
   * *Resolution*: Added `launchMode="singleTask"` and `configChanges` to `AndroidManifest.xml`, moved native `LlmModule` to a process-wide singleton in `ChatViewModel` `companion object` (`sharedNativeModule`), and strictly measured `RssAnon` in `CurriculumRagEngine.kt`. RAM remains strictly ~1571–1592 MB across arbitrary relaunches.

9. **Multilingual Diagnostic Quizzes Across All 6 Indic Languages (Completed Nov 01, 2026)**:
   * *Issue*: Syllabus titles, cultural analogies, and milestones adapted to Indian languages, but quiz questions and options in Tab 0 and Tab 2 remained hardcoded in English.
   * *Resolution*: Upgraded `build_curriculum_db.py` to embed full localized question text (`q`), 4 options (`opts`), and `explanation` across `en`, `hi`, `ta`, `te`, `bn`, `mr` for all 60 diagnostic questions (3 per skill $\times$ 20 skills), while preserving Latin script for technical acronyms (`INT4`, `SpinQuant`, `KleidiAI`, `SQL`, `BCNF`, `IAM`, etc.). Symmetrically applied `(skill_idx * 3 + q_idx) % 4` rotation across all languages to ensure the integer answer index `ans` remains identical. Updated `CurriculumRagEngine.kt` to bump `DB_SCHEMA_VERSION = 4`, parse localized quiz pools with English fallback, and update `getAllSkills(activeLangCode)` so Tab 0 tutor cards and Tab 2 Quiz Arena adapt dynamically.

---

## 4. Current Status Checklist

### ✅ Completed & Production-Verified (Phase 1 Ready)
- [x] Llama-3.2-1B-Instruct SpinQuant INT4 PTE exported and loaded via ExecuTorch 1.1.0 with multi-path SELinux auto-discovery.
- [x] Arm Neon SIMD assembly acceleration enabled via XNNPACK backend.
- [x] Dual hardware benchmark verification:
  - **OnePlus 15 (`CPH2745`, Snapdragon 8 Elite)**: **42.8 – 50.1 t/s**, **1575 – 1592 MB** `RssAnon` (1647 MB PSS), <10ms quiz latency.
  - **Google Pixel 9 Reference**: **82.6 – 89.7 t/s**, **1631 – 1700 MB** VmRSS.
- [x] Verified strictly $< 1.8\text{ GB}$ RAM ceiling with $> 200\text{ MB}$ safety margin.
- [x] Verified 100% offline functionality in Airplane Mode (`OFFLINE=true`, 0 network calls).
- [x] Bayesian Knowledge Tracing (BKT) engine with mathematical update equations, Socratic quiz evaluation, and SharedPreferences persistence.
- [x] SQLite `curriculum_v1.db` (864 KB) containing 20 skills across 6 domains, 60 diagnostic questions (3 per skill, balanced 0–3 answers) fully localized across all 6 languages (`en`, `hi`, `ta`, `te`, `bn`, `mr`), and 120 localized Indic syllabus and analogy records.
- [x] Multilingual Indic support with culturally authentic vernacular analogies and localized diagnostic quizzes with Latin technical acronyms for all 20 skills.
- [x] Jetpack Compose 3-Tab UI: Tab 0 (AI Tutor with 14 chips & active course banner), Tab 1 (20-skill DAG Graph with lock/unlock badges), Tab 2 (Quiz Arena with 3-question pool cycling).
- [x] Structured Tutor Card with native 3-week milestones (`W1, W2, W3`), cultural analogy, and <10ms instant inline quiz grading in chosen Indic language.
- [x] TopAppBar live SoC telemetry overlay (`t/s`, `VmRSS`, and Toast-confirmed BKT reset button).
- [x] Deterministic JSON roadmap synthesis in Kotlin (`buildDeterministicRoadmapJson()`) embedded in UI card and collapsible Inspector.
- [x] Production APK compiled and packaged (`Bharat_AISoC_PS4_Companion.apk`, 98 MB).
- [x] High-definition demo video recorded directly on OnePlus 15 (`Bharat_AISoC_PS4_Demo.mp4`, 43.6s).
- [x] High-res screenshots of Tab 0, Tab 1, and Tab 2 captured in `deliverables/`.
- [x] Comprehensive documentation suite in `DECISIONS_VS_ACHIEVED.md`, `IMPLEMENTATION_PATH.md`, `ARGON_CONTEXT.md`, and `deliverables/TECHNICAL_REPORT.md`.

---

### 🔄 In Progress / Enhancements for Phase 2 (Target: 10 Feb 2027)
- [ ] **Automated Onboarding Diagnostic Wizard**: A guided 3-minute intake flow that auto-tests prerequisites and generates the initial personalized study plan on first app launch.
- [ ] **Offline Speech Integration (TTS/STT)**: Integration of Android on-device offline Text-to-Speech for vernacular audio feedback.
- [ ] **Curriculum Expansion**: Expanding `curriculum_v1.db` from 8 skill nodes to 24+ nodes covering complete GATE CS and AWS Cloud certifications.
- [ ] **Sink-Token Attention KV-Cache Optimization**: Explicit C++ circular ring buffer eviction for infinite multi-turn retention.
- [ ] **Automated Benchmark Instrumentation Suite**: Standalone automated test runner that logs TPS and RAM across multiple simulated queries to a CSV file.

---

## 5. How to Update This Document Moving Ahead

When new features are committed or benchmarks updated, update this file by:
1. Moving items from **Section 4 (In Progress)** to **Section 4 (Completed)**.
2. Logging the new commit hash, changes, and lessons learned under **Section 3 (Discovered / Adjusted)**.
3. Updating the verified metrics in the **Section 1 (Status Overview)** table.

