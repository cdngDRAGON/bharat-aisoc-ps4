# Bharat AI-SoC Challenge (Problem Statement 4): Personalised AI Learning Companion

## 1. Executive Summary & Architecture
This project delivers a **100% offline, on-device Personalised AI Learning Companion** designed for Arm-based mobile SoCs. Built using **ExecuTorch 1.1.0**, **Llama-3.2-1B-Instruct quantized via SpinQuant INT4**, **Neuro-Symbolic Bayesian Knowledge Tracing (BKT)**, and an embedded **SQLite Curriculum & Vernacular Analogy Micro-RAG Engine**, the solution runs entirely on-device without cloud dependencies, telemetries, or external API calls.

### End-to-End System Pipeline
```
[Learner Goal / Quiz Selection]
               │
               ▼
[CurriculumRagEngine: Bidirectional Intent & Script Detection]
               │
               ▼
[Neuro-Symbolic BKT Engine: Bayesian Latent State P(L_t)]
               │
               ▼
[SQLite Micro-RAG: 20 Skills × 6 Domains × 6 Languages]
               │
               ▼
[ExecuTorch C++ JNI Runtime + Arm XNNPACK / KleidiAI INT4 Kernels]
               │
               ▼
[Deterministic Structured Card + Streaming Socratic Guidance]
               │
               ▼
[3-Tab Jetpack Compose Reactive UI: Tutor • Skill Graph • Quiz Arena]
```

---

## 2. Dual Environment Benchmarks (Physical OnePlus 15 & Pixel 9 AVD Emulator)
All physical device benchmarks were empirically measured and verified in **Android Airplane Mode (100% offline)** on our **OnePlus 15 (`CPH2745`, Qualcomm `SM8850` Snapdragon 8 Elite Gen 5, Android 17 / OxygenOS 17.0, 16 GB RAM)**. Early prototyping and CI checks also ran on an **Android Studio Pixel 9 Arm64 AVD Emulator (Apple Silicon host)**:

| Evaluation Metric | PS4 Requirement | Physical OnePlus 15 (`CPH2745`)<br/>SM8850 • Android 17 | Pixel 9 AVD Emulator<br/>Apple Silicon Host | Compliance Status |
| :--- | :--- | :--- | :--- | :---: |
| **Model & Quantization** | Llama family INT4 / INT8 | `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` (1.13 GB) | Same 1.13 GB `.pte` | **PASS** |
| **Hardware Backend** | Arm Neon SIMD / NPU | `XNNPACK` (Arm64 Neon SIMD assembly) | `XNNPACK` (Arm64 SIMD) | **ACHIEVED** |
| **Initialization Latency** | Fast local cold-start | **220 ms – 238 ms** (zero-copy flash mmap) | 338 ms – 405 ms | **PASS** |
| **Token Throughput** | **$\ge 22.0$ tokens/sec** | **42.8 – 50.1 tok/s** (38.4–41.1 tok/s under 8 Mbps capture) | 82.6 – 89.7 tok/s | **PASS (2x margin)** |
| **Resident Memory (RSS)** | **Strictly $< 1800$ MB** | **1567 MB – 1592 MB** `RssAnon` (1647 MB PSS) | 1631 MB – 1700 MB `VmRSS` | **PASS ($>200$ MB headroom)** |
| **Max Sequence Length** | Standard context | **768 tokens** (`targetSeqLen = 768`) | 768 tokens | **ACHIEVED** |
| **Inline Quiz Latency** | Interactive grading | **$< 10$ ms** (local Bayesian evaluation) | $< 10$ ms | **PASS** |
| **Offline Privacy** | **100% Offline** | **Verified (`OFFLINE=true` in Airplane Mode)** | Verified offline | **ACHIEVED** |

---

## 3. Pedagogical & Multilingual Innovation

### 3.1 Neuro-Symbolic Bayesian Knowledge Tracing (BKT)
Rather than delegating probabilistic state estimation to an erratic 1B LLM context, the companion computes student mastery mathematically in Kotlin:
- **Latent Mastery Probability $P(L_t) \in [0.05, 0.99]$**:
  $$\begin{aligned}
  P(L_t \mid \text{correct}) &= \frac{P(L_{t-1}) \cdot (1 - P_S)}{P(L_{t-1}) \cdot (1 - P_S) + (1 - P_{prev}) \cdot P_G} \\
  P(L_t \mid \text{incorrect}) &= \frac{P(L_{t-1}) \cdot P_S}{P(L_{t-1}) \cdot P_S + (1 - P_{prev}) \cdot (1 - P_G)} \\
  P(L_t) &= \text{clamp}(P_{obs} + (1 - P_{obs}) \cdot P_T, 0.05, 0.99)
  \end{aligned}$$
- **Calibrated Parameters**: Transit $P(T) = 0.20$, Slip $P(S) = 0.10$, Guess $P(G) = 0.25$, skill-calibrated priors $P(L_0) \in [0.15, 0.45]$.
- **Pedagogical Action Thresholds**:
  - **Foundational Gap ($P(L_t) < 0.45$):** Automatically injects remedial prerequisites and increases weekly commitment by $+2$ h/week.
  - **In Progress ($0.45 \le P(L_t) < 0.80$):** Focuses on application exercises and Socratic prompts.
  - **Mastered ($P(L_t) \ge 0.80$):** Unlocks downstream dependent skills across the curriculum DAG.

### 3.2 20 Skill Nodes across 6 Domains
The embedded indexed SQLite database (`curriculum_v1.db`, 844 KiB / 864,256 bytes, schema version 4) defines 20 topologically sorted curriculum nodes (`indic_content_fts` retains its legacy table name with B-tree indexes after dropping FTS5 to prevent `no such module: fts5` on Android):
1. **Flagship Arm SoC Edge AI (5 skills):** `edge_ai_01_tensors`, `edge_ai_02_spinquant`, `edge_ai_03_kleidiai`, `edge_ai_04_executorch`, `edge_ai_05_kvcache`.
2. **Exam Preparation - GATE CS (3 skills):** `gate_01_dsa`, `gate_02_os`, `gate_03_dbms`.
3. **Certification Paths - Cloud & DevOps (3 skills):** `cert_01_cloud_sec`, `cert_02_k8s_containers`, `cert_03_devops_ci`.
4. **Career Guidance - Data Analytics (3 skills):** `career_01_data_analyst`, `career_02_data_pipeline`, `career_03_interview_prep`.
5. **Skill Development - Machine Learning (3 skills):** `skill_01_python_ds`, `skill_02_ml_foundations`, `skill_03_deep_learning`.
6. **Lifelong Learning Habits (3 skills):** `life_01_micro_habit`, `life_02_feynman_technique`, `life_03_deep_focus`.

### 3.3 Vernacular Grounding & Multilingual Reach
- **6 Indian Languages:** English (`en`), Hindi (`hi`), Tamil (`ta`), Telugu (`te`), Bengali (`bn`), Marathi (`mr`).
- **6-Language Localized Diagnostic Quiz Engine:** All 60 diagnostic questions (3 per skill $\times$ 20 skills = 360 localized variants) feature native-script question text (`q`), 4 choices (`opts`), and pedagogical explanations (`explanation`) across all 6 languages in `curriculum_v1.db`.
- **Symmetrical 25% Answer Distribution:** Answer indices ($0, 1, 2, 3$) are mathematically preserved across all languages via `(skill_idx * 3 + q_idx) % 4`, ensuring exact 25% distribution (15 As, 15 Bs, 15 Cs, 15 Ds) and language-agnostic BKT evaluation.
- **120 Localized Indic Records:** Verified native-script titles, cultural analogies (e.g., *Silambam* stick rotation for SpinQuant, *Kumbhar* potter's wheel for Cayley rotations, Mumbai *Dabbawala* containerization for Kubernetes, *Mandi* ledger double-entry for database ACID).
- **Latin-Script Acronym Anchoring:** Technical acronyms (`INT4`, `SpinQuant`, `KleidiAI`, `ExecuTorch`, `KV-Cache`, `BCNF`, `IAM`, `SQL`, `CNN`, `mTLS`, `Docker`) are preserved in Latin script within Indic sentences, completely eliminating 1B transliteration hallucinations and fake words.
- **Dual Multilingual Quiz UX:** Both Tab 0 `StructuredTutorCard` and Tab 2 `Quiz Arena` reactively adapt questions, options, badges (`✓ सही`, `✓ சரி`), question counter (`प्रश्न 1 / 3`), and instant feedback banners to the active language with `<10ms` Kotlin BKT grading.

---

## 4. UI/UX Architecture & Reactive State Management

### 4.1 Reactive Whole-Screen Localization
`ChatViewModel` serves as the single source of truth (`activeLangCode`, `activeSkillId`, `bktRefreshTick`). When the user switches languages or mentions a language in natural language:
- The top language selector chip, active course banner, action buttons, and all 3 bottom tab labels immediately adapt to the target Indic script.
- Cold-start welcome cards reactively update in place without requiring an app restart.

### 4.2 Structured Tutor Card (`MessageItem.kt`)
Each curriculum roadmap is rendered as a clean, crash-proof native card containing:
1. **Header & Metadata:** Domain category, skill ID, language badge, and real-time BKT mastery bar.
2. **Native 3-Week Syllabus Milestones:** Numbered roadmap steps (`W1`, `W2`, `W3`) displayed immediately from deterministic JSON metadata.
3. **Vernacular Cultural Analogy:** Grounded in everyday Indian concepts for intuitive mental models.
4. **Instant (<10ms) Localized Inline Quiz Grading:** Interactive options ($A, B, C, D$) graded immediately in Kotlin with localized green/red banners, posterior jumps ($P(L_t): 25\% \to 72\%$), and native explanations—without triggering redundant 1B model calls.
5. **AI Tutor Socratic Guidance:** Streamed bullet points at 40–50 tok/s with automatic sentence completion guards (0 chars dropped).

### 4.3 3-Tab Architecture
- **Tab 0 (`AI Tutor`):** Conversational AI tutor with 14 quick launcher chips, active course banner, and structured tutor cards.
- **Tab 1 (`Skill Graph`):** Comprehensive DAG visualization of all 20 skills grouped by domain, featuring real-time color-coded mastery progress bars and `🔓 Unlocked` / `🔒 Locked` dependency badges.
- **Tab 2 (`Quiz Arena`):** Dedicated diagnostic assessment arena allowing students to test mastery across the 3-question pool per skill with localized question text, localized options, instant BKT feedback, and question cycling (`Next Question →`).

### 4.4 Process-Wide Native Singleton & Single-Task Memory Guard
To prevent memory spikes (e.g. hitting 2769 MB when an activity relaunches or configuration changes occur on 16 GB Android 17 devices like the OnePlus 15):
1. **Single-Task Activity (`AndroidManifest.xml`)**: `MainActivity` enforces `android:launchMode="singleTask"` and `configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"`. Repeated `am start` intents or screen rotations are routed to `onNewIntent`, preventing secondary `MainActivity` or `ChatViewModel` instances within the process.
2. **Process-Wide Static Singleton (`ChatViewModel.kt`)**: The C++ native pointer `sharedNativeModule` and `globalModelLoadLock` are stored in `companion object`. If a ViewModel re-initializes while the model is already memory-mapped, it instantly reuses `sharedNativeModule` with zero re-allocation.
3. **Strict `RssAnon` Linux Metric (`CurriculumRagEngine.kt`)**: The memory tracker strictly inspects `/proc/self/status` `RssAnon` (anonymous heap memory where XNNPACK weights reside, ~1567–1592 MB), eliminating false inflation from clean file-backed page cache (`RssFile`, ~1.13 GB `.pte` mmap).

---

## 5. Deliverables Directory Summary

All official Phase 1 deliverables are verified and bundled in `deliverables/`:

| Deliverable Asset | Size | Description |
| :--- | :--- | :--- |
| **`Bharat_AISoC_PS4_Companion.apk`** | 103.18 MB | Standalone release-ready debug APK with embedded `curriculum_v1.db` and ExecuTorch runtime. |
| **`Bharat_AISoC_PS4_Demo_1080p_16x9.mp4`** | 18.17 MB | Broadcast-mastered 111.33s (1:51) 1920x1080 H.264 16:9 demo with live Airplane Mode chapters. |
| **`Bharat_AISoC_PS4_Demo.mp4`** | 41.63 MB | Raw 93.35s 1272×2772 OnePlus 15 screen capture recorded in Airplane Mode. |
| **`Bharat_AISoC_PS4_Final_Project_Report.pdf`** | 2.13 MB | 6-page A4 Final Project Report covering Methodology, Results, Hardware Utilization, and Optimization Techniques. |
| **`screenshot_tab0_tutor.png`** | 500 KB | Tab 0 Airplane Mode screenshot showing TopAppBar telemetry (`38.5 t/s \| 1571MB Resident`), Hindi course banner, 3-week milestones, and cultural analogy. |
| **`screenshot_tab1_graph.png`** | 529 KB | Tab 1 Airplane Mode screenshot showing TopAppBar telemetry (`1582MB Resident`), 20-skill DAG graph, BKT mastery bars, and unlocked badges in Hindi. |
| **`screenshot_tab2_quiz.png`** | 567 KB | Tab 2 Airplane Mode screenshot showing TopAppBar telemetry (`1590MB Resident`), Quiz Arena (`प्रश्न 2 / 3`), and SpinQuant mastered state (`89%`). |
| **`SHA256SUMS.txt`** | 761 B | Cryptographic SHA-256 verification checksums for all 8 submission deliverables. |
| **`TECHNICAL_REPORT.md`** | — | This comprehensive architectural, mathematical, and benchmark report. |
