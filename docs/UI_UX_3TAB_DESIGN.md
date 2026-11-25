# UI/UX 3-Tab Architecture & Multilingual Parser

The user interface of the Bharat AI-SoC Companion is built entirely using **Jetpack Compose** and **Material 3**, designed for intuitive mobile learning on Android hardware.

---

## 1. TopAppBar & Live SoC Telemetry

The application features a persistent, dark-themed TopAppBar providing real-time telemetry and immediate control:
- **Title & Telemetry Pill:** Displays `🇮🇳 Bharat AI-SoC | {tps} t/s | {ram}MB`.
  - Token speed is updated live during ExecuTorch inference.
  - RAM is queried from `/proc/self/status` (`VmRSS`) via `ragEngine.getTrueProcessRssMb()`.
- **BKT Reset Action (`Icons.Filled.Refresh`):** Resets all learner skill priors back to default $P(L_0)$ values for repeatable benchmarking.
- **Settings Action (`Icons.Filled.Settings`):** Allows inspection of underlying ExecuTorch module settings and model configurations.

---

## 2. 3-Tab Learning Hub Design

A navigation tab bar is positioned immediately below the TopAppBar, providing instant access to the three core operational modes:

| Tab | Icon | Purpose |
| :--- | :--- | :--- |
| **0. Tutor** | `Icons.Filled.School` | Interactive conversational companion with rich markdown streaming, adaptive study roadmaps, and inline diagnostic cards. |
| **1. Graph** | `Icons.Filled.AutoGraph` | Visual **Neuro-Symbolic BKT Skill Graph** displaying mastery progression, prerequisites, and study hour allocations across all domains. |
| **2. Quiz** | `Icons.Filled.Quiz` | Standalone diagnostic assessment environment allowing manual evaluation of any skill node with instantaneous Bayesian updates. |

---

## 3. Tab 0: Tutor Interface

### 3.1 Language Selector Bar
A horizontal scrollable chip row allows learners to switch between 6 supported Indian languages at any moment:
- 🇬🇧 **English** (`en`)
- 🇮🇳 **हिंदी (Hindi)** (`hi`)
- 🇮🇳 **தமிழ் (Tamil)** (`ta`)
- 🇮🇳 **తెలుగు (Telugu)** (`te`)
- 🇮🇳 **বাংলা (Bengali)** (`bn`)
- 🇮🇳 **मराठी (Marathi)** (`mr`)

Selecting a language automatically updates the active language state in `ChatViewModel`, refreshes localized UI labels, and triggers a localized onboarding summary.

### 3.2 Quick Domain Chips
A horizontal row of 8 pre-configured domain chips provides instant one-tap access to primary learning roadmaps:
1. `🛡️ Cybersec` — Zero-trust IAM & Cloud Security
2. `⚡ Edge AI INT4` — ExecuTorch & Arm SIMD Quantization
3. `🔥 PyTorch` — Tensors & Autograd Optimization
4. `📐 Linear Algebra` — Matrix Multiplication for Neural Networks
5. `💻 GATE OS` — Process Scheduling & Synchronization
6. `📊 SQL Career` — Data Analytics Transition & KPIs
7. `🐍 Python DS` — Pandas DataFrames & NumPy
8. `🧠 Habit Loop` — Spaced Repetition & Daily Sprints

### 3.3 Welcome Onboarding Card
Upon initial launch or reset, the companion presents an onboarding card:
- Displays runtime specifications: **ExecuTorch 1.1.0 • SpinQuant INT4 • BKT Engine**.
- Highlights that the app operates **100% offline with zero cloud dependency**.
- Provides 4 direct starter buttons in Hindi, Tamil, Telugu, and Marathi.

### 3.4 Structured Tutor Card
Tutor responses are rendered as structured cards comprising:
- **Quiz Feedback Banner (if applicable):** Displays green or red banner with the Bayesian probability jump (e.g. `P(L_t) updated from 25% → 64%`).
- **Domain & Language Badge:** Displays domain category, skill ID, and language code.
- **BKT Mastery Indicator:** Visual progress bar color-coded based on gap status:
  - 🔴 **Red ($<45\%$):** Critical Foundational Gap (Remedial study $+2\text{h/week}$).
  - 🟡 **Amber ($45\%-80\%$):** In Progress (Standard progression).
  - 🟢 **Green ($\ge 80\%$):** Mastered (Review ready).
- **Vernacular Cultural Analogy Box:** Highlighted callout box explaining the technical concept via everyday Indian analogies.
- **Study Roadmap (RichText CommonMark):** Formatted multi-step learning path rendered with native code blocks.
- **Embedded Diagnostic Quiz:** Inline multiple-choice options with one-tap `Answer: X` submission.
- **Collapsible Roadmap JSON Inspector:** Toggleable section displaying validated JSON data for debugging.

---

## 4. Tab 1: BKT Skill Graph

The **Skill Graph** provides a holistic view of the learner's entire knowledge state:
- **Average Mastery Banner:** Displays overall average mastery across all 8 skill nodes.
- **Skill Nodes List:**
  - Displays skill domain, unique ID, and localized title.
  - Visual mastery progress bar showing current $P(L_t)$ vs. baseline prior $P(L_0)$.
  - Displays prerequisite dependencies (e.g., `ml_02_pytorch` requires `ml_01_linalg`).
  - Displays dynamic weekly study hours (automatically increased for identified gaps).
  - Cultural analogy preview.
- **Interactive Actions per Node:**
  - `📚 Study in [LANG]`: Immediately switches to the Tutor tab and prompts the model to generate a targeted tutorial.
  - `🎯 Diagnostic Quiz`: Immediately switches to the Quiz tab focused on that specific skill node.

---

## 5. Tab 2: Diagnostic Assessment Suite

The **Quiz Tab** offers a focused test-taking environment:
- **Skill Node Carousel:** Allows selecting any of the 8 curriculum nodes.
- **Active Node Assessment Card:**
  - Displays current mastery percentage and status.
  - Displays diagnostic question text and 4 multiple-choice options.
- **Instant Bayesian Feedback:**
  - Tapping an option immediately computes the Bayesian posterior update using $P(T)=0.20$, $P(S)=0.10$, $P(G)=0.25$.
  - Displays immediate result banner with previous vs. new mastery percentages.
  - Adjusts recommended study hours in real time.
- **One-Tap LLM Study Plan Generation:**
  - Button: `🤖 Generate AI Tutor Study Plan for Updated Mastery →`.
  - Automatically transitions back to Tab 0 with an updated prompt reflecting the new mastery score.

---

## 6. Robust Multilingual Regex Parser

### 6.1 Problem Solved
Early implementations relied on strict string matching for English keys (`Domain:`, `Module Title:`). When prompting in Hindi or other Indic languages, the model produced localized headers (e.g., `क्षेत्र:`, `मॉड्यूल:`), causing the parser to fail and fallback to plain unformatted text.

### 6.2 Implementation Details (`MessageItem.kt`)
The parser employs tolerant regular expressions handling case insensitivity, asterisks, bolding, and multilingual synonyms:

```kotlin
val domainRegex = Regex("(?i)(?:domain|क्षेत्र|विषय|डोमेन).*?[:：]\\s*[*_]*([^*_\\n(]+)")
val titleRegex = Regex("(?i)(?:module|title|शीर्षक|मॉड्यूल|विषय).*?[:：]\\s*[*_]*([^*_\\n]+)")
val masteryRegex = Regex("(?i)(?:mastery|निपुणता|स्तर|प्रवीणता|P\\(L_t\\)).*?[:：]\\s*[*_]*(\\d+)")
val analogyRegex = Regex("(?i)(?:analogy|उपमा|उदाहरण|समानता|सादृश्य).*?[:：]\\s*[*_]*([^*_\\n]+)")
val quizRegex = Regex("(?i)(?:quiz|प्रश्नोत्तरी|सवाल|प्रश्न).*?[:：]\\s*[*_]*(.+?)(?=\\s*\\(A\\.|$)")
```

### 6.3 Multilingual Study Plan Delimiters
The parser recognizes study plan start markers across all 6 target languages:
- English: `**AI Tutor Personalized Study Plan:**`, `Personalized Plan`
- Hindi: `**AI ट्यूटर व्यक्तिगत अध्ययन योजना:**`, `अध्ययन योजना`, `मार्गदर्शन`
- Tamil: `**AI ஆசிரியர் ஆய்வுத் திட்டம்:**`
- Telugu: `**AI ట్యూటర్ వ్యక్తిగత అధ్యయన ప్రణాళిక:**`
- Bengali: `**AI টিউটর ব্যক্তিগত অধ্যয়ন পরিকল্পনা:**`
- Marathi: `**AI ट्यूटर वैयक्तिक अभ्यास योजना:**`

This guarantees that structured UI cards render properly regardless of the requested language.
