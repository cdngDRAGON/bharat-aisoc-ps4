# Bharat AI-SoC (Problem Statement 4): Documentation Index

## Project: Personalised AI Learning Companion

Welcome to the technical documentation repository for the **Bharat AI-SoC PS4 Personalised AI Learning Companion**. This documentation provides a comprehensive breakdown of the on-device AI system, recent architectural enhancements, multilingual pedagogical features, and verified hardware benchmarks.

---

## 📚 Documentation Table of Contents

| Document | Description | Key Topics |
| :--- | :--- | :--- |
| **[1. Decisions vs. Achieved Tracker](DECISIONS_VS_ACHIEVED.md)** | Living tracking document comparing initial strategy decisions vs actual code & benchmarks. | Strategic choices, what was achieved, edge cases solved, checklist for Phase 1 & 2. |
| **[2. Implementation Path & Architecture](IMPLEMENTATION_PATH.md)** | End-to-end technical evolution, stages 0 to 6, sequence flow, and future roadmap. | Step-by-step implementation path, pipeline diagram, Phase 1-3 roadmap milestones. |
| **[3. Changelog & Recent Modifications](./CHANGELOG.md)** | Full accounting of all recent commits, unstaged working tree changes, new source files, and build updates. | Git history, file-by-file diffs, commit `9859a77`, commit `f726143`, untracked files. |
| **[4. System Architecture & Pipeline](./ARCHITECTURE.md)** | End-to-end technical pipeline from user interaction to ExecuTorch INT4 execution. | ExecuTorch 1.1.0, SpinQuant INT4, BKT mathematical formulation, Micro-RAG grounding, zero-crash JSON synthesis. |
| **[5. UI/UX 3-Tab Interface & Multilingual Parser](./UI_UX_3TAB_DESIGN.md)** | Jetpack Compose UI architecture, interactive tabs, telemetry overlay, and resilient multilingual parser. | 3-tab layout (Tutor, Graph, Quiz), 6-language switcher, quick domain chips, regex parser, structured tutor cards. |
| **[6. Curriculum Micro-RAG & Vernacular Database](./CURRICULUM_AND_RAG.md)** | Embedded SQLite FTS5 database schema, 6 domains, 8 skill nodes, and Indic cultural analogies. | `curriculum_v1.db`, `build_curriculum_db.py`, FTS5 virtual tables, diagnostic quiz pools, adaptive hour allocation. |
| **[7. Hardware Benchmarks & PS4 Verification](./BENCHMARKS_AND_VERIFICATION.md)** | Rigorous verification of all non-negotiable Problem Statement 4 criteria on Arm64 hardware. | TPS (82.6–89.7 tok/s), RAM (1631–1700 MB), Load time (338–405 ms), 100% offline verification in Airplane Mode. |

---

## 🚀 Key Highlights of the Changes

1. **100% Offline On-Device Neuro-Symbolic Companion**:
   - Zero cloud connectivity required (`OFFLINE=true` verified in Airplane Mode).
   - Powered by Meta's **Llama-3.2-1B-Instruct** quantized with **SpinQuant INT4** and executed on **ExecuTorch 1.1.0** via Arm Neon SIMD acceleration.

2. **Bayesian Knowledge Tracing (BKT) Engine**:
   - Probabilistic student modeling tracking latent mastery $P(L_t)$ with parameters $P(T)=0.20$, $P(S)=0.10$, $P(G)=0.25$.
   - Automatically detects foundational gaps ($P(L_t) < 0.45$) and assigns dynamic remedial workloads (+2h/week).

3. **Curriculum Micro-RAG with SQLite FTS5**:
   - Embedded database (`curriculum_v1.db`) providing full-text search across 6 curriculum domains and 6 Indian languages (English, Hindi, Tamil, Telugu, Bengali, Marathi).
   - Vernacular cultural analogies (e.g., Indian Railways routing, bazaar ledgers, ST bus depots) grounding complex concepts in everyday Indian experiences.

4. **Modern 3-Tab Compose Experience**:
   - **Tutor Tab**: Dynamic chat streaming with rich Markdown, interactive diagnostic quiz widgets, and roadmap JSON inspector.
   - **Graph Tab**: Visual BKT skill graph showing prerequisite dependencies, mastery percentages, and study hour allocations.
   - **Quiz Tab**: Interactive diagnostic assessment suite with instantaneous Bayesian state updating upon option selection.

5. **Robust Multilingual Response Parser**:
   - Tolerant regex-based extraction supporting Hindi Devanagari script, Latin text, markdown formatting variations, and localized synonyms to guarantee 0% UI crashes.
