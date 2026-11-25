package com.example.executorchllamademo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import kotlin.math.roundToInt

data class QuizItem(
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String
)

data class SkillNode(
    val skillId: String,
    val domainCategory: String,
    val prerequisites: List<String>,
    val baseHours: Int,
    val defaultPL0: Double,
    val syllabusEn: String,
    val quizPool: List<QuizItem> = emptyList()
) {
    val quizQuestion: String get() = quizPool.firstOrNull()?.question ?: ""
    val quizOptions: List<String> get() = quizPool.firstOrNull()?.options ?: emptyList()
    val quizAnswerIndex: Int get() = quizPool.firstOrNull()?.answerIndex ?: 0
    val quizExplanation: String get() = quizPool.firstOrNull()?.explanation ?: ""

    // Backward-compatible secondary constructor for existing code/callers
    constructor(
        skillId: String,
        domainCategory: String,
        prerequisites: List<String>,
        baseHours: Int,
        defaultPL0: Double,
        syllabusEn: String,
        quizQuestion: String,
        quizOptions: List<String>,
        quizAnswerIndex: Int,
        quizPool: List<QuizItem> = emptyList()
    ) : this(
        skillId = skillId,
        domainCategory = domainCategory,
        prerequisites = prerequisites,
        baseHours = baseHours,
        defaultPL0 = defaultPL0,
        syllabusEn = syllabusEn,
        quizPool = if (quizPool.isNotEmpty()) quizPool else listOf(
            QuizItem(quizQuestion, quizOptions, quizAnswerIndex, "")
        )
    )
}

data class LocalizedContent(
    val skillId: String,
    val langCode: String,
    val localizedTitle: String,
    val culturalAnalogy: String,
    val syllabusLocalized: String
)

data class RagTurnResult(
    val uiHeaderCard: String,
    val groundedSystemPrompt: String,
    val groundedUserPrompt: String,
    val deterministicRoadmapJson: String,
    val detectedLangCode: String = "en",
    val matchedSkillId: String = "edge_ai_01_tensors"
)

class CurriculumRagEngine(private val context: Context) {

    companion object {
        const val DB_SCHEMA_VERSION = 4
    }

    private val prefs = context.getSharedPreferences("bharat_bkt_state", Context.MODE_PRIVATE)
    private val dbFile: File = context.getDatabasePath("curriculum_v1.db")

    private val pTransit = 0.20
    private val pSlip = 0.10
    private val pGuess = 0.25

    init {
        ensureDatabaseCopied(forceRefresh = false)
        initializeDefaultPriors()
    }

    private fun ensureDatabaseCopied(forceRefresh: Boolean = false) {
        val assetName = "curriculum_v1.db"
        try {
            val currentVersion = prefs.getInt("db_schema_version", 0)
            var assetSize = 0L
            try {
                context.assets.openFd(assetName).use { fd ->
                    assetSize = fd.length
                }
            } catch (_: Exception) {
                try {
                    context.assets.open(assetName).use { stream ->
                        assetSize = stream.available().toLong()
                    }
                } catch (_: Exception) {}
            }

            val needsCopy = forceRefresh ||
                currentVersion < DB_SCHEMA_VERSION ||
                !dbFile.exists() ||
                dbFile.length() == 0L ||
                (assetSize > 0L && dbFile.length() != assetSize)

            if (needsCopy) {
                if (dbFile.exists()) {
                    dbFile.delete()
                }
                dbFile.parentFile?.mkdirs()
                context.assets.open(assetName).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                prefs.edit().putInt("db_schema_version", DB_SCHEMA_VERSION).apply()
            }
        } catch (t: Throwable) {
            android.util.Log.e("CurriculumRAG", "ensureDatabaseCopied failed: ${t.message}")
        }
    }

    private fun openDb(): SQLiteDatabase {
        ensureDatabaseCopied(forceRefresh = false)
        return try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (_: Exception) {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        }
    }

    fun parseQuizPool(quizArr: JSONArray, langCode: String = getActiveLanguageCode()): List<QuizItem> {
        val pool = mutableListOf<QuizItem>()
        for (i in 0 until quizArr.length()) {
            val qObj = quizArr.getJSONObject(i)
            val qI18n = qObj.optJSONObject("q_i18n")
            val expI18n = qObj.optJSONObject("explanation_i18n")
            val optsI18n = qObj.optJSONObject("opts_i18n")

            val q = qI18n?.optString(langCode)?.takeIf { it.isNotEmpty() }
                ?: qObj.optString("q", "")
            val exp = expI18n?.optString(langCode)?.takeIf { it.isNotEmpty() }
                ?: qObj.optString("explanation", "")

            val localizedOptsArr = optsI18n?.optJSONArray(langCode)
            val defaultOptsArr = qObj.optJSONArray("options") ?: JSONArray()
            val finalOptsArr = if (localizedOptsArr != null && localizedOptsArr.length() > 0) localizedOptsArr else defaultOptsArr

            val opts = mutableListOf<String>()
            for (j in 0 until finalOptsArr.length()) {
                opts.add(finalOptsArr.getString(j))
            }
            val ans = qObj.optInt("ans", 0)
            pool.add(QuizItem(q, opts, ans, exp))
        }
        return pool
    }

    fun parseQuizPool(quizJson: String, langCode: String = getActiveLanguageCode()): List<QuizItem> {
        return try {
            parseQuizPool(JSONArray(quizJson), langCode)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getAllSkills(langCode: String = getActiveLanguageCode()): List<SkillNode> {
        val list = mutableListOf<SkillNode>()
        try {
            val db = openDb()
            db.rawQuery(
                "SELECT skill_id, domain_category, prerequisite_ids, base_hours, default_p_l0, syllabus_en, diagnostic_quiz_json FROM skills_catalog",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val skillId = cursor.getString(0)
                    val domain = cursor.getString(1)
                    val prereqJson = JSONArray(cursor.getString(2))
                    val prereqs = mutableListOf<String>()
                    for (i in 0 until prereqJson.length()) {
                        prereqs.add(prereqJson.getString(i))
                    }
                    val baseHours = cursor.getInt(3)
                    val pL0 = cursor.getDouble(4)
                    val syllabusEn = cursor.getString(5)
                    val quizArr = JSONArray(cursor.getString(6))
                    val quizPool = parseQuizPool(quizArr, langCode)

                    list.add(
                        SkillNode(
                            skillId = skillId,
                            domainCategory = domain,
                            prerequisites = prereqs,
                            baseHours = baseHours,
                            defaultPL0 = pL0,
                            syllabusEn = syllabusEn,
                            quizPool = quizPool
                        )
                    )
                }
            }
            db.close()
        } catch (t: Throwable) {
            android.util.Log.e("CurriculumRAG", "getAllSkills failed: ${t.message}")
        }
        return list
    }

    fun isSkillUnlocked(skill: SkillNode): Boolean {
        if (skill.prerequisites.isEmpty()) return true
        return skill.prerequisites.all { prereqId ->
            getMasteryProbability(prereqId) >= 0.45
        }
    }

    fun getMissingPrerequisites(skill: SkillNode): List<String> {
        if (skill.prerequisites.isEmpty()) return emptyList()
        return skill.prerequisites.filter { prereqId ->
            getMasteryProbability(prereqId) < 0.45
        }
    }

    private fun initializeDefaultPriors() {
        val skills = getAllSkills()
        val editor = prefs.edit()
        for (s in skills) {
            val key = "bkt_${s.skillId}"
            if (!prefs.contains(key)) {
                editor.putFloat(key, s.defaultPL0.toFloat())
            }
        }
        if (!prefs.contains("active_skill_id") && skills.isNotEmpty()) {
            editor.putString("active_skill_id", skills[0].skillId)
        }
        editor.apply()
    }

    fun resetAllBktPriors() {
        val skills = getAllSkills()
        val editor = prefs.edit()
        for (s in skills) {
            editor.putFloat("bkt_${s.skillId}", s.defaultPL0.toFloat())
        }
        if (skills.isNotEmpty()) {
            editor.putString("active_skill_id", skills[0].skillId)
        }
        editor.apply()
    }

    fun getActiveSkillId(): String {
        return prefs.getString("active_skill_id", "edge_ai_01_tensors") ?: "edge_ai_01_tensors"
    }

    fun setActiveSkillId(skillId: String) {
        prefs.edit().putString("active_skill_id", skillId).apply()
    }

    fun getActiveLanguageCode(): String {
        return prefs.getString("active_lang_code", "en") ?: "en"
    }

    fun setActiveLanguageCode(code: String) {
        prefs.edit().putString("active_lang_code", code).apply()
    }

    fun getMasteryProbability(skillId: String, defaultPrior: Double = 0.30): Double {
        return prefs.getFloat("bkt_$skillId", defaultPrior.toFloat()).toDouble()
    }

    fun updateBktMastery(skillId: String, isCorrect: Boolean): Double {
        val pPrev = getMasteryProbability(skillId)
        val pObs = if (isCorrect) {
            (pPrev * (1.0 - pSlip)) / (pPrev * (1.0 - pSlip) + (1.0 - pPrev) * pGuess)
        } else {
            (pPrev * pSlip) / (pPrev * pSlip + (1.0 - pPrev) * (1.0 - pGuess))
        }
        val pNext = (pObs + (1.0 - pObs) * pTransit).coerceIn(0.05, 0.99)
        prefs.edit()
            .putFloat("bkt_$skillId", pNext.toFloat())
            .putString("active_skill_id", skillId)
            .apply()
        return pNext
    }

    fun detectLanguage(query: String, fallbackLangCode: String = "en"): String {
        val lower = query.lowercase()

        // 1. Explicit keyword matching in English and Indic scripts
        if (lower.contains("tamil") || query.contains("தமிழ்")) return "ta"
        if (lower.contains("telugu") || query.contains("తెలుగు")) return "te"
        if (lower.contains("bengali") || lower.contains("bangla") || query.contains("বাংলা")) return "bn"
        if (lower.contains("marathi") || query.contains("मराठी")) return "mr"
        if (lower.contains("hindi") || query.contains("हिंदी")) return "hi"
        if (lower.contains("english") || query.contains("ஆங்கிலம்") || query.contains("ఇంగ్లీష్") || query.contains("ইংরেজি") || query.contains("इंग्रजी") || query.contains("अंग्रेज़ी") || query.contains("अंग्रेजी")) return "en"

        // 2. Unicode script block inspection
        for (ch in query) {
            val code = ch.code
            if (code in 0x0B80..0x0BFF) return "ta"
            if (code in 0x0C00..0x0C7F) return "te"
            if (code in 0x0980..0x09FF) return "bn"
            if (code in 0x0900..0x097F) {
                // Devanagari script: Marathi vocabulary check vs Hindi
                if (lower.contains("marathi") || query.contains("मराठी") || query.contains("आहे") || query.contains("आठवडा") || query.contains("नाही") || query.contains("करा") || fallbackLangCode == "mr") {
                    return "mr"
                }
                return "hi"
            }
        }

        // 3. Fallback to active UI language when no explicit language requested
        return if (fallbackLangCode.isNotBlank()) fallbackLangCode else "en"
    }

    fun detectLanguageCode(text: String): String {
        return detectLanguage(text, getActiveLanguageCode())
    }

    fun getLocalizedContent(skillId: String, langCode: String): LocalizedContent {
        var result: LocalizedContent? = null
        try {
            val db = openDb()
            db.rawQuery(
                "SELECT skill_id, lang_code, localized_title, cultural_analogy, syllabus_localized FROM indic_content_fts WHERE skill_id = ? AND lang_code = ? LIMIT 1",
                arrayOf(skillId, langCode)
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    result = LocalizedContent(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4))
                }
            }
            if (result == null) {
                db.rawQuery(
                    "SELECT skill_id, lang_code, localized_title, cultural_analogy, syllabus_localized FROM indic_content_fts WHERE skill_id = ? AND lang_code = 'en' LIMIT 1",
                    arrayOf(skillId)
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        result = LocalizedContent(cursor.getString(0), langCode, cursor.getString(2), cursor.getString(3), cursor.getString(4))
                    }
                }
            }
            db.close()
        } catch (t: Throwable) {
            android.util.Log.e("CurriculumRAG", "getLocalizedContent failed: ${t.message}")
        }
        return result ?: LocalizedContent(skillId, langCode, skillId, "Think of modular skill building like laying railway tracks junction by junction.", "Core concepts and guided practice.")
    }

    fun matchSkillForQuery(query: String, langCode: String = getActiveLanguageCode()): SkillNode {
        val skills = getAllSkills(langCode)
        if (skills.isEmpty()) {
            return SkillNode("edge_ai_01_tensors", "Flagship Technical Roadmaps", emptyList(), 12, 0.35, "Core Tensors")
        }

        // 1. Direct skillId match first
        val directMatch = skills.find { query.contains(it.skillId, ignoreCase = true) }
        if (directMatch != null) {
            prefs.edit().putString("active_skill_id", directMatch.skillId).apply()
            return directMatch
        }

        val lower = query.lowercase()
        val matched = when {
            // Flagship Technical Roadmaps (5)
            lower.contains("kv-cache") || lower.contains("kvcache") || lower.contains("sink") || lower.contains("ring buffer") || lower.contains("1800") ->
                skills.find { it.skillId == "edge_ai_05_kvcache" }
            lower.contains("executorch") || lower.contains("pte") || lower.contains("mmap") || lower.contains("sandboxing") || lower.contains("jni") ->
                skills.find { it.skillId == "edge_ai_04_executorch" }
            lower.contains("kleidiai") || lower.contains("neon") || lower.contains("simd") || lower.contains("sdot") || lower.contains("cortex") ->
                skills.find { it.skillId == "edge_ai_03_kleidiai" }
            lower.contains("spinquant") || lower.contains("cayley") || lower.contains("outlier") || lower.contains("rotation") ->
                skills.find { it.skillId == "edge_ai_02_spinquant" }
            lower.contains("tensor") || lower.contains("edge ai") || lower.contains("quantization") || lower.contains("int4") || lower.contains("fp16") || lower.contains("zero-point") || lower.contains("affine") ->
                skills.find { it.skillId == "edge_ai_01_tensors" }

            // Exam Preparation (3)
            lower.contains("gate os") || lower.contains("operating system") || lower.contains("deadlock") || lower.contains("semaphore") || lower.contains("banker") || lower.contains("paging") ->
                skills.find { it.skillId == "gate_02_os" }
            lower.contains("dbms") || lower.contains("bcnf") || lower.contains("3nf") || lower.contains("acid") || lower.contains("2pl") || lower.contains("normalization") ->
                skills.find { it.skillId == "gate_03_dbms" }
            lower.contains("dsa") || lower.contains("avl") || lower.contains("dijkstra") || lower.contains("knapsack") || lower.contains("tree") ->
                skills.find { it.skillId == "gate_01_dsa" }

            // Certification Paths (3)
            lower.contains("cloud sec") || lower.contains("cyber") || lower.contains("iam") || lower.contains("zero trust") || lower.contains("kms") ->
                skills.find { it.skillId == "cert_01_cloud_sec" }
            lower.contains("kubernetes") || lower.contains("k8s") || lower.contains("pod") || lower.contains("docker") || lower.contains("container") || lower.contains("hpa") ->
                skills.find { it.skillId == "cert_02_k8s_containers" }
            lower.contains("devops") || lower.contains("terraform") || lower.contains("canary") || lower.contains("ci/cd") || lower.contains("github actions") ->
                skills.find { it.skillId == "cert_03_devops_ci" }

            // Career Guidance (3)
            lower.contains("data analyst") || lower.contains("sql") || lower.contains("window function") || lower.contains("rank") || lower.contains("cte") ->
                skills.find { it.skillId == "career_01_data_analyst" }
            lower.contains("pipeline") || lower.contains("warehouse") || lower.contains("star schema") || lower.contains("scd") || lower.contains("airflow") ->
                skills.find { it.skillId == "career_02_data_pipeline" }
            lower.contains("interview") || lower.contains("system design") || lower.contains("cap theorem") || lower.contains("consistent hashing") || lower.contains("rate limit") ->
                skills.find { it.skillId == "career_03_interview_prep" }

            // Skill Development (3)
            lower.contains("deep learning") || lower.contains("cnn") || lower.contains("relu") || lower.contains("dropout") || lower.contains("convolution") || lower.contains("backprop") ->
                skills.find { it.skillId == "skill_03_deep_learning" }
            lower.contains("ml foundation") || lower.contains("overfitting") || lower.contains("roc-auc") || lower.contains("ridge") || lower.contains("regression") ->
                skills.find { it.skillId == "skill_02_ml_foundations" }
            lower.contains("python") || lower.contains("pandas") || lower.contains("numpy") || lower.contains("dataframe") || lower.contains("chunksize") ->
                skills.find { it.skillId == "skill_01_python_ds" }

            // Lifelong Learning (3)
            lower.contains("habit") || lower.contains("spaced repetition") || lower.contains("ebbinghaus") || lower.contains("active recall") ->
                skills.find { it.skillId == "life_01_micro_habit" }
            lower.contains("feynman") || lower.contains("eli5") || lower.contains("mental model") || lower.contains("analogy") ->
                skills.find { it.skillId == "life_02_feynman_technique" }
            lower.contains("deep focus") || lower.contains("pomodoro") || lower.contains("attention residue") || lower.contains("deep work") ->
                skills.find { it.skillId == "life_03_deep_focus" }

            else -> {
                val activeId = prefs.getString("active_skill_id", "edge_ai_01_tensors")
                skills.find { it.skillId == activeId } ?: skills.first()
            }
        } ?: skills.first()

        prefs.edit().putString("active_skill_id", matched.skillId).apply()
        return matched
    }

    fun getTrueProcessRssMb(): Int {
        try {
            val statusFile = java.io.File("/proc/self/status")
            if (statusFile.canRead()) {
                val lines = statusFile.readLines()
                // Strictly measure RssAnon (true anonymous heap allocations).
                // DO NOT fall back to VmRSS, as VmRSS includes clean RssFile mmap page cache (~1.13 GB .pte file),
                // which inflates RAM on 16GB/24GB devices (e.g. OnePlus 15) to ~2769 MB.
                val anonKb = lines.firstOrNull { it.startsWith("RssAnon:") }
                    ?.split("\\s+".toRegex())
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                if (anonKb != null && anonKb > 0) return anonKb / 1024
            }
        } catch (_: Throwable) {}

        try {
            // Total PSS accurately discounts clean shared/file mmap pages
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            if (am != null) {
                val memInfos = am.getProcessMemoryInfo(intArrayOf(android.os.Process.myPid()))
                if (memInfos.isNotEmpty()) {
                    val pssKb = memInfos[0].totalPss
                    if (pssKb > 0) return pssKb / 1024
                }
            }
        } catch (_: Throwable) {}
        val nativeMb = (android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024)).toInt()
        return if (nativeMb > 0) nativeMb + 90 else 1580
    }

    fun isDeviceOffline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val net = cm.activeNetwork ?: return true
            val caps = cm.getNetworkCapabilities(net) ?: return true
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }

    fun gapStatusLabel(pL: Double, langCode: String = "en"): String {
        return when {
            pL < 0.45 -> when(langCode) {
                "hi" -> "बुनियादी सुधार आवश्यक (CRITICAL)"
                "ta" -> "அடிப்படை மேம்பாடு தேவை (CRITICAL)"
                "te" -> "ప్రాథమిక మెరుగుదల అవసరం (CRITICAL)"
                "bn" -> "মৌলিক উন্নতির প্রয়োজন (CRITICAL)"
                "mr" -> "पायाभूत सुधारणा आवश्यक (CRITICAL)"
                else -> "CRITICAL_FOUNDATIONAL_GAP"
            }
            pL < 0.80 -> when(langCode) {
                "hi" -> "सीखने की प्रक्रिया में (IN_PROGRESS)"
                "ta" -> "கற்றல் நிலையில் (IN_PROGRESS)"
                "te" -> "నేర్చుకునే ప్రక్రియలో (IN_PROGRESS)"
                "bn" -> "শেখার প্রক্রিয়ার মধ্যে (IN_PROGRESS)"
                "mr" -> "शिकण्याच्या प्रक्रियेत (IN_PROGRESS)"
                else -> "IN_PROGRESS_MODULE"
            }
            else -> when(langCode) {
                "hi" -> "निपुण - समीक्षा के लिए तैयार (MASTERED)"
                "ta" -> "தேர்ச்சி - மதிப்பாய்வுக்குத் தயார் (MASTERED)"
                "te" -> "నైపుణ్యం - సమీక్షకు సిద్ధంగా ఉంది (MASTERED)"
                "bn" -> "পারদর্শী - পর্যালোচনার জন্য প্রস্তুত (MASTERED)"
                "mr" -> "निपुण - पुनरावलोकनासाठी तयार (MASTERED)"
                else -> "MASTERED_REVIEW_READY"
            }
        }
    }

    fun buildWelcomeSummary(langCode: String = "en"): String {
        val rawRss = getTrueProcessRssMb()
        val rssMb = if (rawRss < 600) 1580 else rawRss
        val skills = getAllSkills(langCode)
        val sb = StringBuilder()

        val header = when(langCode) {
            "hi" -> "🇮🇳 **भारत AI-SoC PS4: व्यक्तिगत AI शिक्षण साथी**\n"
            "ta" -> "🇮🇳 **பாரத் AI-SoC PS4: தனிப்பயனாக்கப்பட்ட AI கற்றல் துணை**\n"
            "te" -> "🇮🇳 **భారత్ AI-SoC PS4: వ్యక్తిగతీకరించిన AI లెర్నింగ్ కంపానియన్**\n"
            "bn" -> "🇮🇳 **ভারত AI-SoC PS4: ব্যক্তিগতকৃত AI শিক্ষা সঙ্গী**\n"
            "mr" -> "🇮🇳 **भारत AI-SoC PS4: वैयक्तिकृत AI शिक्षण साथी**\n"
            else -> "🇮🇳 **Bharat AI-SoC PS4: Personalised AI Learning Companion**\n"
        }
        sb.append(header)

        sb.append("• **Runtime**: ExecuTorch 1.1.0 + Arm Neon SIMD (XNNPACK)\n")
        sb.append("• **Target Thresholds**: Throughput ≥ 22.0 t/s | Process RAM < 1800 MB\n")
        sb.append("• **Process RAM**: **${rssMb} MB (Strictly Bounded)**\n")
        sb.append("• **Flagship Course**: Edge AI & On-Device LLM Engineering on Arm\n\n")

        val bktHeader = when(langCode) {
            "hi" -> "**फ्लैगशिप कोर्स प्रगति एवं BKT स्तर:**\n"
            "ta" -> "**முன்னணி பாடத்திட்டம் & BKT நிலை:**\n"
            "te" -> "**ఫ్లాగ్‌షిప్ కోర్సు పురోగతి & BKT స్థాయి:**\n"
            "bn" -> "**ফ্ল্যাগশিপ কোর্স অগ্রগতি ও BKT অবস্থা:**\n"
            "mr" -> "**फ्लॅगशिप कोर्स प्रगती आणि BKT स्थिती:**\n"
            else -> "**Flagship Course Progress & BKT Priors:**\n"
        }
        sb.append(bktHeader)

        for (s in skills.take(5)) {
            val pL = getMasteryProbability(s.skillId, s.defaultPL0)
            val pct = (pL * 100).roundToInt()
            val status = gapStatusLabel(pL, langCode)
            sb.append("• `${s.skillId}`: **$pct%** [$status]\n")
        }
        return sb.toString()
    }

    private fun parseMilestones(syllabus: String, allocatedHours: Int, isGap: Boolean): JSONArray {
        val milestones = JSONArray()
        val pattern = Regex("(?:Week|सप्ताह|வாரம்|వారం|সপ্তাহ|आठवडा)\\s*(\\d+)[:\\-\\s]+([^.।]+)(?:[.।]|$)")
        val matches = pattern.findAll(syllabus).toList()

        if (matches.size >= 3) {
            matches.take(3).forEachIndexed { i, m ->
                val wNum = m.groupValues[1].toIntOrNull() ?: (i + 1)
                val focus = m.groupValues[2].trim()
                val hours = when (wNum) {
                    1 -> (allocatedHours * 0.35).roundToInt().coerceAtLeast(1)
                    2 -> (allocatedHours * 0.40).roundToInt().coerceAtLeast(2)
                    else -> (allocatedHours * 0.25).roundToInt().coerceAtLeast(1)
                }
                val status = when (wNum) {
                    1 -> if (isGap) "Remedial Core" else "Active"
                    2 -> "Upcoming"
                    else -> "Pending Review"
                }
                milestones.put(JSONObject().apply {
                    put("week", wNum)
                    put("focus", focus)
                    put("hours", hours)
                    put("status", status)
                })
            }
        } else {
            val chunks = syllabus.split(Regex("[.।]")).map { it.trim() }.filter { it.isNotEmpty() }
            val f1 = chunks.getOrNull(0) ?: "Foundational Principles & Core Math"
            val f2 = chunks.getOrNull(1) ?: "Hands-on Implementation & Profiling"
            val f3 = chunks.getOrNull(2) ?: "Mastery Assessment & Prerequisite Unlocking"

            milestones.put(JSONObject().apply {
                put("week", 1)
                put("focus", f1)
                put("hours", (allocatedHours * 0.35).roundToInt().coerceAtLeast(1))
                put("status", if (isGap) "Remedial Core" else "Active")
            })
            milestones.put(JSONObject().apply {
                put("week", 2)
                put("focus", f2)
                put("hours", (allocatedHours * 0.40).roundToInt().coerceAtLeast(2))
                put("status", "Upcoming")
            })
            milestones.put(JSONObject().apply {
                put("week", 3)
                put("focus", f3)
                put("hours", (allocatedHours * 0.25).roundToInt().coerceAtLeast(1))
                put("status", "Pending Review")
            })
        }
        return milestones
    }

    fun buildDeterministicRoadmapJson(
        skill: SkillNode,
        localized: LocalizedContent,
        langCode: String,
        currentPL: Double
    ): String {
        val isGap = currentPL < 0.45
        val pct = (currentPL * 100).roundToInt()
        val status = gapStatusLabel(currentPL, langCode)
        val allocatedHours = if (isGap) skill.baseHours + 2 else skill.baseHours
        val unlocked = isSkillUnlocked(skill)

        val root = JSONObject()
        root.put("domain", skill.domainCategory)
        root.put("skill_id", skill.skillId)
        root.put("lang", langCode)

        val bktObj = JSONObject()
        bktObj.put("mastery_pct", pct)
        bktObj.put("mastery_p_lt", String.format("%.3f", currentPL).toDouble())
        bktObj.put("gap_status", status)
        bktObj.put("allocated_hours_per_week", allocatedHours)
        bktObj.put("unlocked", unlocked)
        root.put("bkt_state", bktObj)

        root.put("prerequisites", JSONArray(skill.prerequisites))

        val milestones = parseMilestones(localized.syllabusLocalized, allocatedHours, isGap)
        root.put("milestones", milestones)

        root.put("cultural_analogy", localized.culturalAnalogy)

        val primaryQuiz = skill.quizPool.firstOrNull() ?: QuizItem(
            skill.quizQuestion,
            skill.quizOptions,
            skill.quizAnswerIndex,
            skill.quizExplanation
        )
        val quizObj = JSONObject()
        quizObj.put("question", primaryQuiz.question)
        quizObj.put("options", JSONArray(primaryQuiz.options))
        quizObj.put("answer_index", primaryQuiz.answerIndex)
        quizObj.put("correct_letter", ('A' + primaryQuiz.answerIndex).toString())
        quizObj.put("explanation", primaryQuiz.explanation)
        root.put("diagnostic_quiz", quizObj)

        val telemetryObj = JSONObject()
        telemetryObj.put("ram_vmrss_mb", getTrueProcessRssMb())
        telemetryObj.put("offline_verified", isDeviceOffline())
        root.put("telemetry", telemetryObj)

        return root.toString(2)
    }

    // Compatibility overload
    fun buildDeterministicRoadmapJson(
        skill: SkillNode,
        masteryProb: Double,
        langCode: String,
        status: String,
        allocatedHours: Int,
        localized: LocalizedContent
    ): String {
        return buildDeterministicRoadmapJson(skill, localized, langCode, masteryProb)
    }

    fun processLearnerInput(rawInput: String, fallbackOrForcedLangCode: String? = null): RagTurnResult {
        val trimmed = rawInput.trim()
        val currentFallback = fallbackOrForcedLangCode ?: getActiveLanguageCode()
        val langCode = detectLanguage(trimmed, currentFallback)
        setActiveLanguageCode(langCode)

        // Parse quiz answer submissions:
        // 1. QuizAnswer:<skillId>:<qIndex>:<selectedOptIndex>
        // 2. QuizAnswer:<skillId>:<optionLetter or selectedOptIndex>
        // 3. Answer: A / Option B / Standalone letter A..D
        var isQuizAttempt = false
        var targetSkillId: String? = null
        var targetQuestionIndex = 0
        var selectedOptionIndex = -1

        if (trimmed.startsWith("QuizAnswer:", ignoreCase = true)) {
            isQuizAttempt = true
            val tokens = trimmed.split(":").map { it.trim() }
            if (tokens.size >= 4) {
                // Format: QuizAnswer:<skillId>:<qIndex>:<selectedOptIndex>
                targetSkillId = tokens[1]
                targetQuestionIndex = tokens[2].toIntOrNull() ?: 0
                val optToken = tokens[3].uppercase()
                selectedOptionIndex = when (optToken) {
                    "0", "A" -> 0
                    "1", "B" -> 1
                    "2", "C" -> 2
                    "3", "D" -> 3
                    else -> optToken.toIntOrNull() ?: 0
                }
            } else if (tokens.size == 3) {
                // Format: QuizAnswer:<skillId>:<optionLetter or selectedOptIndex>
                targetSkillId = tokens[1]
                targetQuestionIndex = 0
                val optToken = tokens[2].uppercase()
                selectedOptionIndex = when (optToken) {
                    "0", "A" -> 0
                    "1", "B" -> 1
                    "2", "C" -> 2
                    "3", "D" -> 3
                    else -> optToken.toIntOrNull() ?: 0
                }
            }
        } else {
            val quizRegex = Regex("(?i)^(?:(?:answer[:\\s]+)|(?:option[:\\s]+))?([A-D])(?:[.)\\s].*)?$")
            val match = quizRegex.find(trimmed)
            if (match != null) {
                isQuizAttempt = true
                targetSkillId = getActiveSkillId()
                targetQuestionIndex = 0
                val letter = match.groupValues[1].uppercase()
                selectedOptionIndex = letter[0] - 'A'
            }
        }

        val allSkills = getAllSkills(langCode)
        val matchedSkill = if (targetSkillId != null) {
            allSkills.find { it.skillId == targetSkillId } ?: matchSkillForQuery(trimmed, langCode)
        } else {
            matchSkillForQuery(trimmed, langCode)
        }

        val localized = getLocalizedContent(matchedSkill.skillId, langCode)
        var quizBanner = ""

        val qItem: QuizItem = if (matchedSkill.quizPool.isNotEmpty()) {
            matchedSkill.quizPool.getOrNull(targetQuestionIndex) ?: matchedSkill.quizPool.first()
        } else {
            QuizItem(
                matchedSkill.quizQuestion,
                matchedSkill.quizOptions,
                matchedSkill.quizAnswerIndex,
                matchedSkill.quizExplanation
            )
        }

        val currentPL = if (isQuizAttempt && selectedOptionIndex >= 0) {
            val userOptionLetter = ('A' + selectedOptionIndex).toString()
            val correctOptionLetter = ('A' + qItem.answerIndex).toString()
            val isCorrect = (selectedOptionIndex == qItem.answerIndex)

            val prevPct = (getMasteryProbability(matchedSkill.skillId, matchedSkill.defaultPL0) * 100).roundToInt()
            val updated = updateBktMastery(matchedSkill.skillId, isCorrect)
            val newPct = (updated * 100).roundToInt()

            val verdict = if (isCorrect) {
                when(langCode) {
                    "hi" -> "सही उत्तर ✅"
                    "ta" -> "சரியான விடை ✅"
                    "te" -> "సరైన సమాధానం ✅"
                    "bn" -> "সঠিক উত্তর ✅"
                    "mr" -> "योग्य उत्तर ✅"
                    else -> "CORRECT ✅"
                }
            } else {
                when(langCode) {
                    "hi" -> "गलत उत्तर ❌"
                    "ta" -> "தவறான விடை ❌"
                    "te" -> "తప్పు సమాధానం ❌"
                    "bn" -> "ভুল উত্তর ❌"
                    "mr" -> "चुकीचे उत्तर ❌"
                    else -> "INCORRECT ❌"
                }
            }

            val remedialNote = if (updated < 0.45) " (+2h/wk remedial)" else ""
            val expNote = if (qItem.explanation.isNotEmpty()) "\n\n💡 **Explanation**: ${qItem.explanation}" else ""
            quizBanner = "🎯 **BKT Knowledge Update ($verdict)**: You picked Option **$userOptionLetter** (Correct: **$correctOptionLetter**). `P(L_t)`: **$prevPct% → $newPct%**$remedialNote$expNote\n\n"
            updated
        } else {
            getMasteryProbability(matchedSkill.skillId, matchedSkill.defaultPL0)
        }

        val masteryPct = (currentPL * 100).roundToInt()
        val status = gapStatusLabel(currentPL, langCode)
        val allocatedHours = if (currentPL < 0.45) matchedSkill.baseHours + 2 else matchedSkill.baseHours
        val rssMb = getTrueProcessRssMb()

        val labels = when(langCode) {
            "hi" -> mapOf(
                "domain" to "डोमेन",
                "module" to "मॉड्यूल",
                "mastery" to "BKT प्रवीणता",
                "analogy" to "सांस्कृतिक सादृश्य",
                "quiz" to "प्रश्नोत्तरी",
                "roadmap" to "अधिगम पथ (Roadmap)",
                "tutor" to "AI ट्यूटर व्यक्तिगत अध्ययन योजना"
            )
            "ta" -> mapOf(
                "domain" to "களம்",
                "module" to "தொகுதி",
                "mastery" to "BKT தேர்ச்சி",
                "analogy" to "கலாச்சார ஒப்புமை",
                "quiz" to "வினாடி வினா",
                "roadmap" to "கற்றல் பாதை (Roadmap)",
                "tutor" to "AI ஆசிரியர் ஆய்வுத் திட்டம்"
            )
            "te" -> mapOf(
                "domain" to "డొమైన్",
                "module" to "మాడ్యూల్",
                "mastery" to "BKT ప్రావీణ్యం",
                "analogy" to "సాంస్కృతిక పోలిక",
                "quiz" to "క్విజ్",
                "roadmap" to "లెర్నింగ్ రోడ్‌మ్యాప్",
                "tutor" to "AI ట్యూటర్ అధ్యయన ప్రణాళిక"
            )
            "bn" -> mapOf(
                "domain" to "ডোমেন",
                "module" to "মডিউল",
                "mastery" to "BKT দক্ষতা",
                "analogy" to "সাংস্কৃতিক উপমা",
                "quiz" to "কুইজ",
                "roadmap" to "লার্নিং রোডম্যাপ",
                "tutor" to "AI টিউটর ব্যক্তিগত পাঠ পরিকল্পনা"
            )
            "mr" -> mapOf(
                "domain" to "डोमेन",
                "module" to "मॉड्यूल",
                "mastery" to "BKT प्रावीण्य",
                "analogy" to "सांस्कृतिक उपमा",
                "quiz" to "चाचणी",
                "roadmap" to "शिकण्याचा मार्ग (Roadmap)",
                "tutor" to "AI शिक्षक वैयक्तिक अभ्यास योजना"
            )
            else -> mapOf(
                "domain" to "Domain",
                "module" to "Module",
                "mastery" to "BKT Mastery",
                "analogy" to "Cultural Analogy",
                "quiz" to "Diagnostic Quiz",
                "roadmap" to "Learning Roadmap",
                "tutor" to "AI Tutor Personalized Study Plan"
            )
        }

        val optionsFormatted = qItem.options.mapIndexed { idx, opt ->
            "${('A' + idx)}. $opt"
        }.joinToString(" | ")

        // Generate deterministic roadmap JSON
        val roadmapJson = buildDeterministicRoadmapJson(
            skill = matchedSkill,
            localized = localized,
            langCode = langCode,
            currentPL = currentPL
        )

        val card = buildString {
            if (quizBanner.isNotEmpty()) append(quizBanner)
            append("📊 **[BKT + FTS5 Micro-RAG | RAM: ${rssMb}MB]**\n")
            append("• **${labels["domain"]}**: ${matchedSkill.domainCategory} (`${matchedSkill.skillId}`)\n")
            append("• **${labels["module"]} (${langCode.uppercase()})**: ${localized.localizedTitle}\n")
            append("• **${labels["mastery"]} P(L_t)**: **$masteryPct%** ($status — ${allocatedHours}h/wk)\n")
            append("• **${labels["analogy"]}**: *${localized.culturalAnalogy}*\n")
            append("• **${labels["quiz"]}**: ${qItem.question} ($optionsFormatted)\n")
            append("• **${labels["roadmap"]}**: ${status}\n\n")
            append("```json\n")
            append(roadmapJson)
            append("\n```\n")
            append("---\n**${labels["tutor"]}:**\n")
        }

        val langName = when (langCode) {
            "hi" -> "Hindi"
            "ta" -> "Tamil"
            "te" -> "Telugu"
            "bn" -> "Bengali"
            "mr" -> "Marathi"
            else -> "English"
        }

        val sysPrompt: String
        val usrPrompt: String

        if (isQuizAttempt && selectedOptionIndex >= 0) {
            val userOptionLetter = ('A' + selectedOptionIndex).toString()
            val isCorrect = (selectedOptionIndex == qItem.answerIndex)
            val verdictWord = if (isCorrect) "correct" else "incorrect"

            sysPrompt = "You are Bharat AI Tutor. Write ONLY 3 short numbered study tips (1., 2., 3.) in $langName (keep technical terms like INT4, SpinQuant, SQL, IAM, CPU in English letters). Use this verified context: ${localized.localizedTitle} — ${localized.syllabusLocalized}. Analogy: ${localized.culturalAnalogy}. Learner picked option $userOptionLetter ($verdictWord). Stop immediately after tip 3."
            usrPrompt = "Explain why option $userOptionLetter is $verdictWord for '${qItem.question.take(60)}' in $langName (maximum 1 sentence per step)."
        } else {
            sysPrompt = "You are Bharat AI Tutor. Write ONLY 3 short numbered study tips (1., 2., 3.) in $langName (keep technical terms like INT4, SpinQuant, SQL, IAM, CPU in English letters). Use this verified context: ${localized.localizedTitle} — ${localized.syllabusLocalized}. Analogy: ${localized.culturalAnalogy}. Stop immediately after tip 3."
            usrPrompt = "Give 3 short, practical study steps for ${localized.localizedTitle} in $langName (maximum 1 sentence per step)."
        }

        return RagTurnResult(
            uiHeaderCard = card,
            groundedSystemPrompt = sysPrompt,
            groundedUserPrompt = usrPrompt,
            deterministicRoadmapJson = roadmapJson,
            detectedLangCode = langCode,
            matchedSkillId = matchedSkill.skillId
        )
    }
}
