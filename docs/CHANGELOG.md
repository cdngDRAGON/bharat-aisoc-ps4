# Detailed Changelog of Recent Modifications

This document details all modifications made to the Bharat AI-SoC PS4 codebase, spanning git commits, unstaged working tree enhancements, new files, and build configuration updates.

---

## 1. Summary of Changed Files

| File Path | Status | Impact / Purpose |
| :--- | :--- | :--- |
| [`CurriculumRagEngine.kt`](../android/app/src/main/java/com/example/executorchllamademo/CurriculumRagEngine.kt) | **New File (Untracked)** | Core Bayesian Knowledge Tracing (BKT) engine, SQLite FTS5 Micro-RAG query runner, offline network detector, and process RSS memory profiler. |
| [`curriculum_v1.db`](../android/app/src/main/assets/curriculum_v1.db) | **New File (Untracked)** | Pre-compiled SQLite 3 database bundled in Android assets containing 8 skill catalog nodes and multilingual FTS5 tables. |
| [`ChatScreen.kt`](../android/app/src/main/java/com/example/executorchllamademo/ui/screens/ChatScreen.kt) | **Modified** | Re-architected into a 3-Tab UI (Tutor, Graph, Quiz), 6-language horizontal switcher, quick domain launcher chips, and real-time SoC telemetry top bar. |
| [`MessageItem.kt`](../android/app/src/main/java/com/example/executorchllamademo/ui/components/MessageItem.kt) | **Modified + 2 Commits** | Multilingual regex parsing engine, structured tutor cards with mastery progress bars, vernacular cultural callouts, interactive quiz submission, and roadmap JSON inspector. |
| [`ChatViewModel.kt`](../android/app/src/main/java/com/example/executorchllamademo/ui/viewmodel/ChatViewModel.kt) | **Modified** | Auto-loads quantized model and tokenizer, integrates Micro-RAG prompt grounding, sets 384 token sequence limit, and logs `BHARAT_PS4_*` telemetry tags. |
| [`ModuleSettings.kt`](../android/app/src/main/java/com/example/executorchllamademo/ModuleSettings.kt) | **Modified** | Sets default model path to `/data/local/tmp/llama/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` and tokenizer path to `/data/local/tmp/llama/tokenizer.model`. |
| [`AndroidManifest.xml`](../android/app/src/main/AndroidManifest.xml) | **Modified** | Declares `android.permission.ACCESS_NETWORK_STATE` and designates `MainActivity` as the default `MAIN` / `LAUNCHER` activity. |
| [`MainActivity.kt`](../android/app/src/main/java/com/example/executorchllamademo/MainActivity.kt) | **Modified** | Keeps `MainActivity` active in the back stack by removing `finish()` call on navigation to `WelcomeActivity`. |
| [`gradle.properties`](../android/gradle.properties) | **Modified** | Configures JVM arguments (`-Xmx2048m`) and optimized compiler daemon settings. |
| [`gradle-wrapper.properties`](../android/gradle/wrapper/gradle-wrapper.properties) | **Modified** | Extends network timeout to `120000ms` for robust dependency downloads. |
| [`build_curriculum_db.py`](../curriculum/build_curriculum_db.py) | **Created (Root)** | Python generation script for creating `curriculum_v1.db` with schema, initial skills, and localized Indic analogies. |
| [`deliverables/`](../deliverables/) | **Updated Deliverables** | Added `Bharat_AISoC_PS4_Companion.apk` (built Oct 31 17:41), `pixel9_3tab_ui_screenshot.png`, `pixel9_bkt_tutor_screenshot.png`, and `TECHNICAL_REPORT.md`. |

---

## 2. Commit Breakdown in `executorch-examples`

### Commit `9859a77837576ebc03dca9e4bd71e3286c682c26`
**Author:** cdngDRAGON <weber69developer@gmail.com>  
**Date:** Sat Oct 31 18:02:14 2026 +0530  
**Title:** *Fix Hindi tutor response parsing by adding multilingual support and robust field matching*

- **Problem Addressed:** The initial response parser strictly assumed English markdown keys (e.g. `Domain:`, `Module Title:`, `Mastery:`). When the prompt requested Hindi output (Devanagari script), the parser failed to recognize headers, resulting in unstyled raw text fallbacks or missing cards.
- **Key Code Additions:**
  - Expanded regex patterns to detect Devanagari synonyms: `क्षेत्र`, `विषय`, `डोमेन` for Domain; `शीर्षक`, `मॉड्यूल` for Title; `निपुणता`, `स्तर`, `प्रवीणता` for Mastery.
  - Added Hindi study plan delimiter detection: `**AI ट्यूटर व्यक्तिगत अध्ययन योजना:**`, `**AI शिक्षक व्यक्तिगत अध्ययन योजना:**`, `अध्ययन योजना`, `मार्गदर्शन`.
  - Added initial multi-lingual gap status and quiz update parsing.

### Commit `f726143340dcd4a518ea389536dda7c2eb0eda7b`
**Author:** cdngDRAGON <weber69developer@gmail.com>  
**Date:** Sat Oct 31 18:25:52 2026 +0530  
**Title:** *Enhance tutor parser with robust Regex for Hindi support and formatting variations*

- **Problem Addressed:** Minor formatting variations in LLM token streaming (such as asterisks around values, different colon styles `:`, `：`, or trailing whitespace) could still disrupt field extraction.
- **Key Code Additions:**
  - Standardized tolerant matching using `(?i)` case insensitivity, `[^*_\n(]+`, and `removeSurrounding("**")`.
  - Added support for alternative Indian language markers across Tamil, Telugu, Bengali, and Marathi study plans.
  - Enhanced quiz option extraction to safely isolate question text from `(A. ...)` option strings.

---

## 3. Detailed Working Tree Changes (Unstaged)

### A. `CurriculumRagEngine.kt` (New Architecture Component)
- Implements `CurriculumRagEngine` class that manages:
  1. **SQLite Asset Copying:** Ensures `curriculum_v1.db` is copied from assets to the app's private databases directory.
  2. **BKT State Storage:** Stores skill mastery probabilities in `SharedPreferences` (`bharat_bkt_state`) with key `bkt_<skill_id>`.
  3. **Bayesian Knowledge Tracing Math:**
     - Computes updated probability $P(L_t)$ upon quiz answers:
       ```kotlin
       val pObs = if (isCorrect) {
           (pPrev * (1.0 - pSlip)) / (pPrev * (1.0 - pSlip) + (1.0 - pPrev) * pGuess)
       } else {
           (pPrev * pSlip) / (pPrev * pSlip + (1.0 - pPrev) * (1.0 - pGuess))
       }
       val pNext = (pObs + (1.0 - pObs) * pTransit).coerceIn(0.05, 0.99)
       ```
  4. **Vernacular Micro-RAG Grounding:** Matches user queries against target skills, retrieves cultural analogies and syllabus context via SQLite, and injects structured context into the prompt:
     ```kotlin
     val sysPrompt = "You are Bharat AI Tutor running offline on Arm ExecuTorch. " +
         "Syllabus Context: ${localized.syllabusLocalized}. " +
         "Learner BKT mastery is $masteryPct% ($status). " +
         "Provide technical instruction and a 3-step roadmap STRICTLY in $langName. DO NOT use JSON format. Respond with natural text and bullet points."
     ```
  5. **Telemetry & Environment Profiling:** Reads `/proc/self/status` for actual Linux process `VmRSS` memory usage and queries `ConnectivityManager` to verify offline status.

### B. `ChatScreen.kt` (UI Modernization)
- Transformed from a basic single chat view into a multi-tab learning hub:
  - **Tab Row Navigation:** High-contrast tab selector switching between `Tutor` (chat), `Graph` (BKT progress), and `Quiz` (diagnostic tests).
  - **TopAppBar Telemetry:** Real-time badge showing `| {tps} t/s | {ram}MB` alongside quick BKT Reset and Settings buttons.
  - **Language Selector Bar:** Horizontal carousel for instant switching between 🇬🇧 English, 🇮🇳 Hindi, 🇮🇳 Tamil, 🇮🇳 Telugu, 🇮🇳 Bengali, and 🇮🇳 Marathi.
  - **Quick Domain Chips:** Fast launcher chips for 8 skill domains (Cybersecurity, Edge AI, PyTorch, Linear Algebra, GATE OS, SQL Career, Python DS, Habit Loop).
  - **Graph Tab:** Dynamic mastery bars with color coding (Red for $<45\%$, Amber for $45\%-80\%$, Green for $\ge 80\%$) and direct actions to study or test.
  - **Quiz Tab:** Dedicated quiz assessment panel with instant Bayesian update banners.

### C. `MessageItem.kt` (Visual Rendering & Parser Refinements)
- Added `ParsedTutorMessage` data model and parser function `parseTutorResponse()`.
- Implemented `WelcomeOnboardingCard` to display offline runtime specs and instant launcher prompts.
- Implemented `StructuredTutorCard`:
  - Visual BKT Quiz update banner (Green/Red).
  - Domain category chip, skill ID tag, and language badge.
  - Mastery percentage bar with weekly hour allocation.
  - Cultural analogy box with amber theme and lightbulb icon.
  - RichText CommonMark Markdown renderer for study roadmaps.
  - Interactive quiz option rows with `onQuickAction("Answer: X")` callbacks.
  - Live token speed chip (`xx.x tok/s`).
  - Collapsible **Roadmap JSON Inspector** to view deterministic JSON structure.

### D. `ChatViewModel.kt` (Orchestration & Performance)
- Added `autoLoadDefaultModelIfNeeded()` to auto-load the 1B SpinQuant INT4 model on startup without manual picker interactions.
- Hardcoded inference sequence length to 384 tokens (`module?.generate(finalPrompt, 384, this, false)`) to enforce strict sub-1.8 GB memory bounds and fast response turnaround.
- Pre-populates `resultMessage` with the Micro-RAG UI header card before generation starts.
- Added structured log tags (`BHARAT_PS4_READY`, `BHARAT_PS4_METRICS`, `BHARAT_PS4_OUTPUT`, `BHARAT_PS4_TURN_DONE`) for automated evaluation and CI benchmarking.

### E. `ModuleSettings.kt` & `AndroidManifest.xml`
- `ModuleSettings.kt`: Configured default model path to `/data/local/tmp/llama/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` and tokenizer path to `/data/local/tmp/llama/tokenizer.model`.
- `AndroidManifest.xml`: Added permission `ACCESS_NETWORK_STATE` and switched the launcher activity to `MainActivity`.
