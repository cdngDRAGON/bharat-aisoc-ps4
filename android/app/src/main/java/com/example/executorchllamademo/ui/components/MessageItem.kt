package com.example.executorchllamademo.ui.components

import android.net.Uri
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.executorchllamademo.CurriculumRagEngine
import com.example.executorchllamademo.Message
import com.example.executorchllamademo.MessageType
import com.example.executorchllamademo.ui.theme.LocalAppColors
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import org.json.JSONArray
import org.json.JSONObject

data class SyllabusMilestone(
    val week: Int,
    val focus: String,
    val hours: Int,
    val status: String
)

private data class InlineQuizFeedback(
    val isCorrect: Boolean,
    val selectedLetter: Char,
    val correctLetter: Char,
    val prevPct: Int,
    val nextPct: Int,
    val explanation: String
)

private data class CardQuizStrings(
    val sectionHeader: String,
    val submit: String,
    val correctBadge: String,
    val chosenBadge: String,
    val correctTitle: (correctLetter: Char) -> String,
    val incorrectTitle: (selectedLetter: Char, correctLetter: Char) -> String,
    val masteryUpdated: (prevPct: Int, nextPct: Int) -> String,
    val remedialSuffix: String,
    val explanationPrefix: String,
    val askAiExplain: (langName: String) -> String,
    val tutorGuidanceHeader: String
)

private fun getCardQuizStrings(langCode: String): CardQuizStrings {
    return when (langCode.lowercase()) {
        "hi" -> CardQuizStrings(
            sectionHeader = "⚡ कौशल जांच प्रश्नोत्तरी",
            submit = "उत्तर सबमिट करें →",
            correctBadge = "✓ सही",
            chosenBadge = "✗ चुना गया",
            correctTitle = { c -> "✅ सही उत्तर! BKT प्रवीणता P(L_t) अद्यतन (विकल्प $c सही है)" },
            incorrectTitle = { s, c -> "❌ गलत उत्तर — सही उत्तर: विकल्प $c (आपने चुना: $s)" },
            masteryUpdated = { p, n -> "BKT प्रवीणता P(L_t) अद्यतन: $p% → $n%" },
            remedialSuffix = " (+2 घंटे/सप्ताह उपचारात्मक)",
            explanationPrefix = "💡 व्याख्या:",
            askAiExplain = { l -> "🤖 AI ट्यूटर से $l में व्याख्या पूछें →" },
            tutorGuidanceHeader = "🤖 AI ट्यूटर मार्गदर्शन"
        )
        "ta" -> CardQuizStrings(
            sectionHeader = "⚡ திறன்றிதல் வினாடி வினா",
            submit = "விடையை சமர்ப்பி →",
            correctBadge = "✓ சரி",
            chosenBadge = "✗ தேர்வு",
            correctTitle = { c -> "✅ சரியான விடை! BKT தேர்ச்சி P(L_t) புதுப்பிக்கப்பட்டது (தேர்வு $c சரியானது)" },
            incorrectTitle = { s, c -> "❌ தவறான விடை — சரியான விடை: தேர்வு $c (நீங்கள் தேர்வு செய்தது: $s)" },
            masteryUpdated = { p, n -> "BKT தேர்ச்சி P(L_t) புதுப்பிக்கப்பட்டது: $p% → $n%" },
            remedialSuffix = " (+2 மணி/வாரம் கூடுதல்)",
            explanationPrefix = "💡 விளக்கம்:",
            askAiExplain = { l -> "🤖 AI ஆசிரியரிடம் $l-ல் விளக்கம் கேட்க →" },
            tutorGuidanceHeader = "🤖 AI ஆசிரியர் வழிகாட்டுதல்"
        )
        "te" -> CardQuizStrings(
            sectionHeader = "⚡ నైపుణ్య పరీక్ష క్విజ్",
            submit = "సమాధానం సమర్పించండి →",
            correctBadge = "✓ సరైనది",
            chosenBadge = "✗ ఎంచుకున్నది",
            correctTitle = { c -> "✅ సరైన సమాధానం! BKT ప్రావీణ్యం P(L_t) నవీకరించబడింది (ఎంపిక $c సరైనది)" },
            incorrectTitle = { s, c -> "❌ తప్పు సమాధానం — సరైన సమాధానం: ఎంపిక $c (మీరు ఎంచుకున్నది: $s)" },
            masteryUpdated = { p, n -> "BKT ప్రావీణ్యం P(L_t) నవీకరించబడింది: $p% → $n%" },
            remedialSuffix = " (+2 గంటలు/వారం అదనపు)",
            explanationPrefix = "💡 వివరణ:",
            askAiExplain = { l -> "🤖 AI ట్యూటర్‌ను $l లో వివరణ అడగండి →" },
            tutorGuidanceHeader = "🤖 AI ట్యూటర్ మార్గదర్శకత్వం"
        )
        "bn" -> CardQuizStrings(
            sectionHeader = "⚡ दक्षता যাচাই কুইজ",
            submit = "জমা দিন →",
            correctBadge = "✓ সঠিক",
            chosenBadge = "✗ নির্বাচিত",
            correctTitle = { c -> "✅ সঠিক উত্তর! BKT দক্ষতা P(L_t) আপডেট হয়েছে (বিকল্প $c সঠিক)" },
            incorrectTitle = { s, c -> "❌ ভুল উত্তর — সঠিক উত্তর: বিকল্প $c (আপনি বেছেছেন: $s)" },
            masteryUpdated = { p, n -> "BKT দক্ষতা P(L_t) আপডেট হয়েছে: $p% → $n%" },
            remedialSuffix = " (+২ ঘণ্টা/সপ্তাহ প্রতিকারমূলক)",
            explanationPrefix = "💡 ব্যাখ্যা:",
            askAiExplain = { l -> "🤖 AI টিউটরের কাছে $l-এ ব্যাখ্যা চান →" },
            tutorGuidanceHeader = "🤖 AI টিউটর নির্দেশিকা"
        )
        "mr" -> CardQuizStrings(
            sectionHeader = "⚡ कौशल्य चाचणी प्रश्नमंजुषा",
            submit = "उत्तर सबमिट करा →",
            correctBadge = "✓ बरोबर",
            chosenBadge = "✗ निवडलेले",
            correctTitle = { c -> "✅ योग्य उत्तर! BKT प्रावीण्य P(L_t) अपडेट झाले (पर्याय $c बरोबर आहे)" },
            incorrectTitle = { s, c -> "❌ चुकीचे उत्तर — योग्य उत्तर: पर्याय $c (तुम्ही निवडलेला पर्याय: $s)" },
            masteryUpdated = { p, n -> "BKT प्रावीण्य P(L_t) अपडेट झाले: $p% → $n%" },
            remedialSuffix = " (+२ तास/आठवडा उपचारात्मक)",
            explanationPrefix = "💡 स्पष्टीकरण:",
            askAiExplain = { l -> "🤖 AI शिक्षकाला $l मध्ये स्पष्टीकरण विचारा →" },
            tutorGuidanceHeader = "🤖 AI शिक्षक मार्गदर्शन"
        )
        else -> CardQuizStrings(
            sectionHeader = "⚡ Diagnostic Skill Check",
            submit = "Submit →",
            correctBadge = "✓ Correct",
            chosenBadge = "✗ Chosen",
            correctTitle = { c -> "✅ Correct! BKT Mastery P(L_t) updated (Option $c is correct)" },
            incorrectTitle = { s, c -> "❌ Incorrect — Correct Answer: Option $c (You picked: $s)" },
            masteryUpdated = { p, n -> "BKT Mastery P(L_t) updated: $p% → $n%" },
            remedialSuffix = " (+2h/wk remedial)",
            explanationPrefix = "💡 Explanation:",
            askAiExplain = { l -> "🤖 Ask AI Tutor to Explain in $l →" },
            tutorGuidanceHeader = "🤖 AI Tutor Guidance"
        )
    }
}

private data class ParsedTutorMessage(
    val quizBanner: String,
    val domain: String,
    val skillId: String,
    val langCode: String,
    val moduleTitle: String,
    val masteryPct: Int,
    val gapStatus: String,
    val allocatedHours: String,
    val culturalAnalogy: String,
    val milestones: List<SyllabusMilestone> = emptyList(),
    val quizQuestion: String,
    val quizOptions: List<String>,
    val quizAnswerIndex: Int,
    val quizExplanation: String,
    val roadmapJson: String,
    val studyPlanMarkdown: String,
    val telemetryFooter: String
)

private fun parseTutorResponse(raw: String): ParsedTutorMessage? {
    // Only parse as structured card if we see the micro-RAG indicator, JSON code fence, or BKT marker
    if (!raw.contains("📊") && !raw.contains("BKT") && !raw.contains("```json") && !raw.contains("```")) return null

    var quizBanner = ""
    var domain = "Curriculum Module"
    var skillId = "edge_ai_01_tensors"
    var langCode = "EN"
    var moduleTitle = ""
    var masteryPct = 30
    var gapStatus = "IN_PROGRESS_MODULE"
    var allocatedHours = "8h/wk"
    var analogy = ""
    var quizQuestion = ""
    var quizOptions = emptyList<String>()
    var quizAnswerIndex = 0
    var quizExplanation = ""
    var roadmapJson = "{}"
    var parsedFromJson = false
    val milestones = mutableListOf<SyllabusMilestone>()

    // 1. Extract Quiz Banner if present
    if (raw.contains("BKT Knowledge Update") || raw.contains("BKT Quiz Update") || raw.contains("प्रश्नोत्तरी अपडेट") || raw.contains("🎯")) {
        val bannerStart = raw.indexOf("🎯")
        if (bannerStart >= 0) {
            val bannerEnd = raw.indexOf("📊", bannerStart)
            if (bannerEnd > bannerStart) {
                quizBanner = raw.substring(bannerStart, bannerEnd).trim()
            } else {
                val bannerLine = raw.substring(bannerStart).lines().firstOrNull() ?: ""
                quizBanner = bannerLine.trim()
            }
        }
    }

    // 2. Extract deterministic JSON block from ```json ... ``` code fence
    val jsonFenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
    val fenceMatch = jsonFenceRegex.find(raw)
    if (fenceMatch != null) {
        val jsonCandidate = fenceMatch.groupValues[1].trim()
        if (jsonCandidate.startsWith("{") && jsonCandidate.endsWith("}")) {
            try {
                val json = JSONObject(jsonCandidate)
                roadmapJson = jsonCandidate
                parsedFromJson = true

                domain = json.optString("domain", domain)
                skillId = json.optString("skill_id", skillId)
                langCode = json.optString("lang", langCode).uppercase()
                analogy = json.optString("cultural_analogy", analogy)

                val bktObj = json.optJSONObject("bkt_state")
                if (bktObj != null) {
                    masteryPct = bktObj.optInt("mastery_pct", masteryPct)
                    gapStatus = bktObj.optString("gap_status", gapStatus)
                    val hoursInt = bktObj.optInt("allocated_hours_per_week", 8)
                    allocatedHours = "${hoursInt}h/wk"
                }

                val milestonesArr = json.optJSONArray("milestones")
                if (milestonesArr != null) {
                    for (i in 0 until milestonesArr.length()) {
                        val mObj = milestonesArr.optJSONObject(i)
                        if (mObj != null) {
                            milestones.add(
                                SyllabusMilestone(
                                    week = mObj.optInt("week", i + 1),
                                    focus = mObj.optString("focus", ""),
                                    hours = mObj.optInt("hours", 2),
                                    status = mObj.optString("status", "Active")
                                )
                            )
                        }
                    }
                }

                val quizObj = json.optJSONObject("diagnostic_quiz")
                if (quizObj != null) {
                    quizQuestion = quizObj.optString("question", "")
                    quizAnswerIndex = quizObj.optInt("answer_index", 0)
                    quizExplanation = quizObj.optString("explanation", "")
                    val optArr = quizObj.optJSONArray("options")
                    if (optArr != null) {
                        quizOptions = List(optArr.length()) { optArr.getString(it) }
                    }
                }
            } catch (_: Exception) {
                // If JSON parsing fails, fall back to regex below
            }
        }
    }

    // 3. Extract module title from bullet line: • **Module ...**: Title
    val titleRegex = Regex("(?i)•\\s*\\*\\*[^*]*(?:module|title|शीर्षक|मॉड्यूल|தொகுதி|మాడ్యూల్|মডিউল)[^*]*\\*\\*\\s*[:：]\\s*([^*\\n]+)")
    val titleMatch = titleRegex.find(raw)
    if (titleMatch != null) {
        moduleTitle = titleMatch.groupValues[1].trim().removeSurrounding("**")
    }
    if (moduleTitle.isEmpty()) {
        moduleTitle = skillId.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    // 4. Fallback line-by-line regex if JSON was not parsed
    if (!parsedFromJson) {
        val domainRegex = Regex("(?i).*?(?:domain|क्षेत्र|विषय|डोमेन).*?[:：]\\s*[*_]*([^*_\\n(]+)")
        val skillRegex = Regex("\\(([^)]+)\\)")
        val masteryRegex = Regex("(?i).*?(?:mastery|निपुणता|स्तर|प्रवीणता|P\\(L_t\\)).*?[:：]\\s*[*_]*(\\d+)")
        val analogyRegex = Regex("(?i).*?(?:analogy|उपमा|उदाहरण|समानता|सादृश्य).*?[:：]\\s*[*_]*([^*_\\n]+)")
        val quizRegex = Regex("(?i).*?(?:quiz|प्रश्नोत्तरी|सवाल|प्रश्न).*?[:：]\\s*[*_]*(.+?)(?=\\s*\\(A\\.|$)")
        val optionsRegex = Regex("\\((A\\..+?)\\)")

        for (line in raw.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            when {
                domainRegex.containsMatchIn(trimmed) -> {
                    domain = domainRegex.find(trimmed)?.groupValues?.get(1)?.trim()?.removeSurrounding("**") ?: domain
                    skillId = skillRegex.find(trimmed)?.groupValues?.get(1)?.replace("`", "") ?: skillId
                }
                masteryRegex.containsMatchIn(trimmed) -> {
                    masteryPct = masteryRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: masteryPct
                }
                analogyRegex.containsMatchIn(trimmed) -> {
                    analogy = analogyRegex.find(trimmed)?.groupValues?.get(1)?.trim()?.removeSurrounding("**") ?: analogy
                }
                quizRegex.containsMatchIn(trimmed) -> {
                    quizQuestion = quizRegex.find(trimmed)?.groupValues?.get(1)?.trim()?.removeSurrounding("**") ?: quizQuestion
                    val optMatch = optionsRegex.find(trimmed)
                    if (optMatch != null) {
                        quizOptions = optMatch.groupValues[1].split("|").map { it.trim() }
                    }
                }
            }
        }
    }

    // 5. Extract study plan markdown (everything after the json block and divider)
    val afterBlock = if (fenceMatch != null) {
        raw.substring(fenceMatch.range.last + 1)
    } else if (raw.contains("---")) {
        raw.substringAfterLast("---")
    } else {
        raw.substringAfter("📊")
    }

    var cleanedStudyPlan = afterBlock.trim()
    while (cleanedStudyPlan.startsWith("---") || cleanedStudyPlan.startsWith("–") || cleanedStudyPlan.startsWith("-")) {
        cleanedStudyPlan = cleanedStudyPlan.replaceFirst(Regex("^[-–—]+\\s*"), "").trim()
    }

    val tutorHeaderRegex = Regex("(?i)^\\s*(?:\\*\\*)?[^*]+(?:tutor|study plan|guidance|योजना|திட்டம்|ప్రణాళిక|পরিকল্পনা|शिक्षक)[^*]*(?:\\*\\*)?\\s*[:：]?\\s*")
    cleanedStudyPlan = cleanedStudyPlan.replace(tutorHeaderRegex, "").trim()
    while (cleanedStudyPlan.startsWith("---") || cleanedStudyPlan.startsWith("–") || cleanedStudyPlan.startsWith("-")) {
        cleanedStudyPlan = cleanedStudyPlan.replaceFirst(Regex("^[-–—]+\\s*"), "").trim()
    }

    val telemetryMarker = "⚡ **Arm SoC Telemetry**:"
    val telemetryFooter = if (cleanedStudyPlan.contains(telemetryMarker)) {
        telemetryMarker + cleanedStudyPlan.substringAfter(telemetryMarker)
    } else ""
    cleanedStudyPlan = cleanedStudyPlan.substringBefore(telemetryMarker).trim()

    return ParsedTutorMessage(
        quizBanner = quizBanner,
        domain = domain,
        skillId = skillId,
        langCode = langCode,
        moduleTitle = moduleTitle,
        masteryPct = masteryPct,
        gapStatus = gapStatus,
        allocatedHours = allocatedHours,
        culturalAnalogy = analogy,
        milestones = milestones,
        quizQuestion = quizQuestion,
        quizOptions = quizOptions,
        quizAnswerIndex = quizAnswerIndex,
        quizExplanation = quizExplanation,
        roadmapJson = roadmapJson,
        studyPlanMarkdown = cleanedStudyPlan,
        telemetryFooter = telemetryFooter
    )
}

@Composable
fun MessageItem(
    message: Message,
    onQuickAction: (String) -> Unit = {},
    onBktUpdated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.85f

    when (message.messageType) {
        MessageType.SYSTEM -> SystemMessage(message, maxBubbleWidth, modifier)
        MessageType.IMAGE -> ImageMessage(message, maxBubbleWidth, modifier)
        MessageType.TEXT -> {
            if (message.isSent) {
                UserSentBubble(message, maxBubbleWidth, modifier)
            } else if (isWelcomeMessage(message.text)) {
                WelcomeOnboardingCard(message, onQuickAction, modifier)
            } else {
                val parsed = parseTutorResponse(message.text)
                if (parsed != null) {
                    StructuredTutorCard(parsed, message, onQuickAction, onBktUpdated, modifier)
                } else {
                    StandardAssistantCard(message, modifier)
                }
            }
        }
    }
}

@Composable
private fun UserSentBubble(
    message: Message,
    maxWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

private fun isWelcomeMessage(text: String): Boolean {
    return text.contains("AI-SoC", ignoreCase = true) ||
           text.contains("Bharat", ignoreCase = true) ||
           text.contains("பாரத்") ||
           text.contains("భారத்") ||
           text.contains("ভারত") ||
           text.contains("भारत")
}

@Composable
private fun WelcomeOnboardingCard(
    message: Message,
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val cardBg = if (appColors.isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val borderCol = if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPrimary = if (appColors.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (appColors.isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val lines = message.text.lines()
    val rawTitle = lines.firstOrNull { isWelcomeMessage(it) }?.replace("*", "")?.trim() ?: "🇮🇳 Bharat AI-SoC PS4 Companion"
    val ramMatch = Regex("Process RAM.*?:\\s*\\*\\*(\\d+\\s*MB)").find(message.text)
    val ramDisplay = ramMatch?.groupValues?.getOrNull(1) ?: "1580 MB"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4F46E5).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = rawTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "ExecuTorch • SpinQuant INT4 • BKT",
                        fontSize = 9.sp,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RAM $ramDisplay",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "100% offline personalized AI tutor across 6 Indian languages and domains. Tap a quick-start workflow below:",
            fontSize = 10.sp,
            color = textSecondary,
            lineHeight = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        val starterPrompts = listOf(
            "🛡️ Hindi Cybersecurity" to "I want to learn cybersecurity in Hindi",
            "⚡ Tamil Edge AI Roadmap" to "Build my 4-week Edge AI quantization roadmap in Tamil",
            "🔥 Telugu PyTorch Autograd" to "Explain PyTorch tensors and autograd in Telugu",
            "💻 Marathi GATE OS Prep" to "Teach me GATE Operating Systems CPU scheduling in Marathi"
        )

        starterPrompts.forEach { (label, prompt) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (appColors.isDark) Color(0xFF0F172A) else Color(0xFFEEF2FF))
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { onQuickAction(prompt) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (appColors.isDark) Color(0xFFE0E7FF) else Color(0xFF3730A3)
                )
                Text(
                    text = "Launch →",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
            }
        }
    }
}

@Composable
private fun StructuredTutorCard(
    parsed: ParsedTutorMessage,
    message: Message,
    onQuickAction: (String) -> Unit,
    onBktUpdated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val isDark = appColors.isDark
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var showJsonInspector by remember { mutableStateOf(false) }

    val masteryColor = when {
        parsed.masteryPct < 45 -> Color(0xFFEF4444)
        parsed.masteryPct < 80 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    val statusReadable = when {
        parsed.gapStatus.contains("CRITICAL") -> "Critical Gap • Remedial Active"
        parsed.gapStatus.contains("IN_PROGRESS") -> "In Progress • Guided Practice"
        else -> "Mastered • Review Ready"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        if (parsed.quizBanner.isNotEmpty()) {
            val isCorrect = parsed.quizBanner.contains("CORRECT ✅")
            val bannerBg = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
            val bannerBorder = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bannerBg)
                    .border(1.dp, bannerBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = bannerBorder,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = parsed.quizBanner,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = parsed.domain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = parsed.skillId,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0EA5E9).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🌐 ${parsed.langCode}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0EA5E9)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = parsed.moduleTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mastery: ${parsed.masteryPct}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "${parsed.allocatedHours} • $statusReadable",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = masteryColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (parsed.masteryPct / 100f).coerceIn(0.05f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(masteryColor)
                )
            }
        }

        // 3-Week Personalized Curriculum Milestones Box
        if (parsed.milestones.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗺️ 3-Week Personalized Curriculum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                    Text(
                        text = "Language: ${parsed.langCode}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                parsed.milestones.forEach { m ->
                    val statusColor = when {
                        m.status.contains("Active", ignoreCase = true) -> Color(0xFF10B981)
                        m.status.contains("Remedial", ignoreCase = true) -> Color(0xFFEF4444)
                        m.status.contains("Upcoming", ignoreCase = true) -> Color(0xFF38BDF8)
                        else -> Color(0xFFF59E0B)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color.White)
                            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "W${m.week}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m.focus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${m.hours}h • ${m.status}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }

        if (parsed.culturalAnalogy.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Vernacular Cultural Analogy",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parsed.culturalAnalogy,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = textPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Diagnostic Quiz block with Instant (<10ms) inline Kotlin BKT grading
        if (parsed.quizQuestion.isNotEmpty() && parsed.quizOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            val context = LocalContext.current
            val ragEngine = remember { CurriculumRagEngine(context) }
            val quizStrings = remember(parsed.langCode) { getCardQuizStrings(parsed.langCode) }
            var selectedOptionIdx by remember(message.id) { mutableIntStateOf(-1) }
            var inlineFeedback by remember(message.id) { mutableStateOf<InlineQuizFeedback?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFEEF2FF))
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = quizStrings.sectionHeader,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parsed.quizQuestion,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                parsed.quizOptions.forEachIndexed { idx, optText ->
                    val optionLetter = ('A' + idx)
                    val cleanOpt = optText.replace(Regex("^[A-D][.)]\\s*"), "")
                    val isChosen = (selectedOptionIdx == idx)
                    val isAnswered = (selectedOptionIdx >= 0)
                    val isCorrectOption = (idx == parsed.quizAnswerIndex)

                    val optBg = when {
                        !isAnswered -> if (isDark) Color(0xFF1E293B) else Color.White
                        isCorrectOption -> Color(0xFF10B981).copy(alpha = 0.15f)
                        isChosen -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        else -> if (isDark) Color(0xFF1E293B) else Color.White
                    }

                    val optBorder = when {
                        !isAnswered -> borderCol
                        isCorrectOption -> Color(0xFF10B981)
                        isChosen -> Color(0xFFEF4444)
                        else -> borderCol.copy(alpha = 0.4f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(optBg)
                            .border(1.dp, optBorder, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isAnswered) {
                                selectedOptionIdx = idx
                                val isCorrect = (idx == parsed.quizAnswerIndex)
                                val prevPL = ragEngine.getMasteryProbability(parsed.skillId)
                                val prevPct = (prevPL * 100).roundToInt()
                                val nextPL = ragEngine.updateBktMastery(parsed.skillId, isCorrect)
                                val nextPct = (nextPL * 100).roundToInt()
                                onBktUpdated()

                                inlineFeedback = InlineQuizFeedback(
                                    isCorrect = isCorrect,
                                    selectedLetter = optionLetter,
                                    correctLetter = ('A' + parsed.quizAnswerIndex),
                                    prevPct = prevPct,
                                    nextPct = nextPct,
                                    explanation = parsed.quizExplanation
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$optionLetter. $cleanOpt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isAnswered) {
                                if (isCorrectOption) quizStrings.correctBadge else if (isChosen) quizStrings.chosenBadge else ""
                            } else quizStrings.submit,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnswered) {
                                if (isCorrectOption) Color(0xFF10B981) else if (isChosen) Color(0xFFEF4444) else Color.Transparent
                            } else Color(0xFF6366F1)
                        )
                    }
                }

                // Instant inline result banner (<10ms Kotlin BKT evaluation)
                inlineFeedback?.let { fb ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val fbBg = if (fb.isCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                    val fbBorder = if (fb.isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(fbBg)
                            .border(1.dp, fbBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = fbBorder,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (fb.isCorrect) {
                                    quizStrings.correctTitle(fb.correctLetter)
                                } else {
                                    quizStrings.incorrectTitle(fb.selectedLetter, fb.correctLetter)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fb.isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${quizStrings.masteryUpdated(fb.prevPct, fb.nextPct)}${if (!fb.isCorrect) quizStrings.remedialSuffix else ""}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        if (fb.explanation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${quizStrings.explanationPrefix} ${fb.explanation}",
                                fontSize = 10.sp,
                                color = textPrimary.copy(alpha = 0.9f)
                            )
                        }

                        // Optional deep Socratic explanation button
                        val langDisplayName = when (parsed.langCode.lowercase()) {
                            "hi" -> "हिंदी"
                            "ta" -> "தமிழ்"
                            "te" -> "తెలుగు"
                            "bn" -> "বাংলা"
                            "mr" -> "मराठी"
                            else -> "English"
                        }
                        val langPromptSuffix = when (parsed.langCode.lowercase()) {
                            "hi" -> "in Hindi"
                            "ta" -> "in Tamil"
                            "te" -> "in Telugu"
                            "bn" -> "in Bengali"
                            "mr" -> "in Marathi"
                            else -> "in English"
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4F46E5))
                                .clickable {
                                    onQuickAction("Explain why option ${fb.correctLetter} is correct for ${parsed.skillId} diagnostic quiz: ${parsed.quizQuestion} $langPromptSuffix")
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = quizStrings.askAiExplain(langDisplayName),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Streamed AI Tutor Socratic Guidance without duplicate headers
        if (parsed.studyPlanMarkdown.isNotEmpty()) {
            val tutorStrings = remember(parsed.langCode) { getCardQuizStrings(parsed.langCode) }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tutorStrings.tutorGuidanceHeader,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            RichText(
                style = RichTextStyle(
                    codeBlockStyle = CodeBlockStyle(
                        textStyle = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = textPrimary
                        )
                    )
                )
            ) {
                Markdown(content = parsed.studyPlanMarkdown)
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚡ Decoding Socratic coaching...",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (message.tokensPerSecond > 0) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f tok/s", message.tokensPerSecond),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = message.getFormattedTimestamp(),
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                    .clickable { showJsonInspector = !showJsonInspector }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showJsonInspector) "Hide JSON" else "Roadmap JSON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Icon(
                    imageVector = if (showJsonInspector) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showJsonInspector,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(10.dp)
            ) {
                Text(
                    text = "// Deterministic Crash-Proof Roadmap JSON Schema",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parsed.roadmapJson,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
private fun StandardAssistantCard(
    message: Message,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val textColor = appColors.textOnBubble

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(appColors.messageBubbleReceived)
            .padding(12.dp)
    ) {
        if (message.text.isNotEmpty()) {
            RichText(
                style = RichTextStyle(
                    paragraphSpacing = 8.sp
                )
            ) {
                Markdown(content = message.text)
            }
        }
        if (message.tokensPerSecond > 0) {
            Text(
                text = String.format("⚡ %.1f t/s", message.tokensPerSecond),
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SystemMessage(
    message: Message,
    maxWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .background(
                    color = appColors.messageBubbleSystem,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
                color = if (appColors.isDark) Color.LightGray else Color.Gray
            )
        }
    }
}

@Composable
private fun ImageMessage(
    message: Message,
    maxWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val arrangement = if (message.isSent) Arrangement.End else Arrangement.Start
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = arrangement
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .background(appColors.messageBubbleReceived, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            AsyncImage(
                model = Uri.parse(message.imagePath),
                contentDescription = "Attached image",
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}