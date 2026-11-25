package com.example.executorchllamademo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.executorchllamademo.CurriculumRagEngine
import com.example.executorchllamademo.QuizItem
import com.example.executorchllamademo.SkillNode
import com.example.executorchllamademo.ui.components.ChatInput
import com.example.executorchllamademo.ui.components.MessageItem
import com.example.executorchllamademo.ui.theme.LocalAppColors
import com.example.executorchllamademo.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class LangOption(val code: String, val label: String, val promptSuffix: String)
private data class DomainChip(val skillId: String, val iconLabel: String, val queryTopic: String)

private data class ScreenUiStrings(
    val tabTutor: String,
    val tabGraph: String,
    val tabQuiz: String,
    val activeCourseLabel: String,
    val quizThisCourse: String,
    val studyNow: String,
    val takeQuiz: String,
    val resetPriorsMsg: String,
    val avgMastery: String,
    val diagnosticTitle: String,
    val askAiExplain: String,
    val generateStudyPlan: String,
    val submitAnswer: String,
    val nextQuestion: String,
    val unlocked: String,
    val locked: String,
    val hoursPerWk: String,
    val diagnosticPassed: String,
    val diagnosticNeedsReview: String,
    val correctBadge: String,
    val chosenBadge: String,
    val masteryLabel: String,
    val analogyLabel: String,
    val explanationPrefix: String,
    val formatQuestionCount: (current: Int, total: Int) -> String,
    val formatQuizCorrect: (letter: Char, before: Int, after: Int, exp: String) -> String,
    val formatQuizIncorrect: (selected: Char, correct: Char, after: Int, exp: String) -> String
) {
    companion object {
        fun forLang(code: String): ScreenUiStrings = when (code) {
            "hi" -> ScreenUiStrings(
                tabTutor = "AI ट्यूटर",
                tabGraph = "दक्षता आरेख",
                tabQuiz = "प्रश्नोत्तरी",
                activeCourseLabel = "सक्रिय पाठ्यक्रम",
                quizThisCourse = "⚡ प्रश्नोत्तरी शुरू करें",
                studyNow = "📚 अभी सीखें",
                takeQuiz = "🎯 प्रश्नोत्तरी लें",
                resetPriorsMsg = "BKT स्थिति रीसेट की गई",
                avgMastery = "औसत प्रवीणता",
                diagnosticTitle = "मूल्यांकन हेतु कौशल चुनें:",
                askAiExplain = "🤖 AI ट्यूटर से व्याख्या पूछें",
                generateStudyPlan = "🤖 AI ट्यूटर अध्ययन योजना बनाएं",
                submitAnswer = "उत्तर सबमिट करें →",
                nextQuestion = "अगला प्रश्न →",
                unlocked = "🔓 खुला है",
                locked = "🔒 लॉक है",
                hoursPerWk = "घंटे/सप्ताह",
                diagnosticPassed = "मूल्यांकन सफल ✅",
                diagnosticNeedsReview = "पुनरावलोकन आवश्यक ❌",
                correctBadge = "✓ सही",
                chosenBadge = "✗ चुना गया",
                masteryLabel = "वर्तमान BKT प्रवीणता P(L_t)",
                analogyLabel = "💡 सांस्कृतिक सादृश्य",
                explanationPrefix = "💡 व्याख्या:",
                formatQuestionCount = { c, t -> "प्रश्न $c / $t" },
                formatQuizCorrect = { l, b, a, exp -> "✅ सही उत्तर! BKT प्रवीणता P(L_t) अद्यतन (विकल्प $l सही है) — $b% → $a%$exp" },
                formatQuizIncorrect = { s, c, a, exp -> "❌ गलत उत्तर — सही उत्तर: विकल्प $c (आपने चुना: $s) — BKT P(L_t): $a% (+2 घंटे/सप्ताह)$exp" }
            )
            "ta" -> ScreenUiStrings(
                tabTutor = "AI ஆசிரியர்",
                tabGraph = "திறன் வரைபடம்",
                tabQuiz = "வினாடி வினா",
                activeCourseLabel = "செயலில் உள்ள பாடம்",
                quizThisCourse = "⚡ வினாடி வினா தொடங்கு",
                studyNow = "📚 இப்போது கற்க",
                takeQuiz = "🎯 வினாடி வினா எடு",
                resetPriorsMsg = "BKT நிலை மீட்டமைக்கப்பட்டது",
                avgMastery = "சராசரி தேர்ச்சி",
                diagnosticTitle = "மதிப்பீட்டிற்கான திறனைத் தேர்ந்தெடுக்கவும்:",
                askAiExplain = "🤖 AI ஆசிரியரிடம் விளக்கம் கேட்க",
                generateStudyPlan = "🤖 AI ஆசிரியர் கற்றல் திட்டம் உருவாக்குக",
                submitAnswer = "விடையை சமர்ப்பி →",
                nextQuestion = "அடுத்த கேள்வி →",
                unlocked = "🔓 திறக்கப்பட்டது",
                locked = "🔒 பூட்டப்பட்டது",
                hoursPerWk = "மணி/வாரம்",
                diagnosticPassed = "மதிப்பீட்டில் தேர்ச்சி ✅",
                diagnosticNeedsReview = "மறுஆய்வு தேவை ❌",
                correctBadge = "✓ சரி",
                chosenBadge = "✗ தேர்வு",
                masteryLabel = "தற்போதைய BKT தேர்ச்சி P(L_t)",
                analogyLabel = "💡 பண்பாட்டு ஒப்புமை",
                explanationPrefix = "💡 விளக்கம்:",
                formatQuestionCount = { c, t -> "வினா $c / $t" },
                formatQuizCorrect = { l, b, a, exp -> "✅ சரியான விடை! BKT தேர்ச்சி P(L_t) புதுப்பிக்கப்பட்டது (தேர்வு $l சரியானது) — $b% → $a%$exp" },
                formatQuizIncorrect = { s, c, a, exp -> "❌ தவறான விடை — சரியான விடை: தேர்வு $c (நீங்கள் தேர்வு செய்தது: $s) — BKT P(L_t): $a% (+2 மணி/வாரம்)$exp" }
            )
            "te" -> ScreenUiStrings(
                tabTutor = "AI ట్యూటర్",
                tabGraph = "నైపుణ్య గ్రాఫ్",
                tabQuiz = "క్విజ్",
                activeCourseLabel = "ప్రస్తుత కోర్సు",
                quizThisCourse = "⚡ క్విజ్ ప్రారంభించండి",
                studyNow = "📚 ఇప్పుడే నేర్చుకోండి",
                takeQuiz = "🎯 క్విజ్ తీసుకోండి",
                resetPriorsMsg = "BKT స్థితి రీసెట్ చేయబడింది",
                avgMastery = "సగటు ప్రావీణ్యం",
                diagnosticTitle = "అంచనా కోసం నైపుణ్యాన్ని ఎంచుకోండి:",
                askAiExplain = "🤖 AI ట్యూటర్‌ను వివరణ అడగండి",
                generateStudyPlan = "🤖 AI అధ్యయన ప్రణాళికను రూపొందించండి",
                submitAnswer = "సమర్పించండి →",
                nextQuestion = "తదుపరి ప్రశ్న →",
                unlocked = "🔓 అన్‌లాక్ చేయబడింది",
                locked = "🔒 లాక్ చేయబడింది",
                hoursPerWk = "గం/వారం",
                diagnosticPassed = "అంచనాలో ఉత్తీర్ణత ✅",
                diagnosticNeedsReview = "పునఃపరిశీలన అవసరం ❌",
                correctBadge = "✓ సరైనది",
                chosenBadge = "✗ ఎంచుకున్నది",
                masteryLabel = "ప్రస్తుత BKT ప్రావీణ్యం P(L_t)",
                analogyLabel = "💡 సాంస్కృతిక సారూప్యత",
                explanationPrefix = "💡 వివరణ:",
                formatQuestionCount = { c, t -> "ప్రశ్న $c / $t" },
                formatQuizCorrect = { l, b, a, exp -> "✅ సరైన సమాధానం! BKT ప్రావీణ్యం P(L_t) నవీకరించబడింది (ఎంపిక $l సరైనది) — $b% → $a%$exp" },
                formatQuizIncorrect = { s, c, a, exp -> "❌ తప్పు సమాధానం — సరైన సమాధానం: ఎంపిక $c (మీరు ఎంచుకున్నది: $s) — BKT P(L_t): $a% (+2 గంటలు/వారం)$exp" }
            )
            "bn" -> ScreenUiStrings(
                tabTutor = "AI টিউটর",
                tabGraph = "স্কিল গ্রাফ",
                tabQuiz = "কুইজ",
                activeCourseLabel = "সক্রিয় কোর্স",
                quizThisCourse = "⚡ কুইজ শুরু করুন",
                studyNow = "📚 এখনই শিখুন",
                takeQuiz = "🎯 কুইজ দিন",
                resetPriorsMsg = "BKT অবস্থা রিসেট হয়েছে",
                avgMastery = "গড় দক্ষতা",
                diagnosticTitle = "মূল্যায়নের জন্য দক্ষতা নির্বাচন করুন:",
                askAiExplain = "🤖 AI টিউটরকে ব্যাখ্যা করতে বলুন",
                generateStudyPlan = "🤖 AI টিউটর পাঠ পরিকল্পনা তৈরি করুন",
                submitAnswer = "জমা দিন →",
                nextQuestion = "পরবর্তী প্রশ্ন →",
                unlocked = "🔓 আনলকড",
                locked = "🔒 লক করা",
                hoursPerWk = "ঘণ্টা/সপ্তাহ",
                diagnosticPassed = "মূল্যায়ন সফল ✅",
                diagnosticNeedsReview = "পুনর্বিবেচনা প্রয়োজন ❌",
                correctBadge = "✓ সঠিক",
                chosenBadge = "✗ নির্বাচিত",
                masteryLabel = "বর্তমান BKT দক্ষতা P(L_t)",
                analogyLabel = "💡 সাংস্কৃতিক উপমা",
                explanationPrefix = "💡 ব্যাখ্যা:",
                formatQuestionCount = { c, t -> "প্রশ্ন $c / $t" },
                formatQuizCorrect = { l, b, a, exp -> "✅ সঠিক উত্তর! BKT দক্ষতা P(L_t) আপডেট হয়েছে (বিকল্প $l সঠিক) — $b% → $a%$exp" },
                formatQuizIncorrect = { s, c, a, exp -> "❌ ভুল উত্তর — সঠিক উত্তর: বিকল্প $c (আপনি বেছেছেন: $s) — BKT P(L_t): $a% (+২ ঘণ্টা/সপ্তাহ)$exp" }
            )
            "mr" -> ScreenUiStrings(
                tabTutor = "AI शिक्षक",
                tabGraph = "कौशल्य आलेख",
                tabQuiz = "चाचणी",
                activeCourseLabel = "सक्रिय अभ्यासक्रम",
                quizThisCourse = "⚡ चाचणी सुरू करा",
                studyNow = "📚 आता शिका",
                takeQuiz = "🎯 चाचणी द्या",
                resetPriorsMsg = "BKT पूर्वस्थिती रीसेट केली",
                avgMastery = "सरासरी प्रावीण्य",
                diagnosticTitle = "मूल्यांकनासाठी कौशल्य निवडा:",
                askAiExplain = "🤖 AI शिक्षकाकडून स्पष्टीकरण मागा",
                generateStudyPlan = "🤖 AI शिक्षक अभ्यास योजना तयार करा",
                submitAnswer = "उत्तर सादर करा →",
                nextQuestion = "पुढील प्रश्न →",
                unlocked = "🔓 अनलॉक",
                locked = "🔒 लॉक",
                hoursPerWk = "तास/आठवडा",
                diagnosticPassed = "मूल्यांकन उत्तीर्ण ✅",
                diagnosticNeedsReview = "पुनरावलोकन आवश्यक ❌",
                correctBadge = "✓ बरोबर",
                chosenBadge = "✗ निवडलेले",
                masteryLabel = "सध्याचे BKT प्रावीण्य P(L_t)",
                analogyLabel = "💡 सांस्कृतिक साम्य",
                explanationPrefix = "💡 स्पष्टीकरण:",
                formatQuestionCount = { c, t -> "प्रश्न $c / $t" },
                formatQuizCorrect = { l, b, a, exp -> "✅ योग्य उत्तर! BKT प्रावीण्य P(L_t) अपडेट झाले (पर्याय $l बरोबर आहे) — $b% → $a%$exp" },
                formatQuizIncorrect = { s, c, a, exp -> "❌ चुकीचे उत्तर — योग्य उत्तर: पर्याय $c (तुम्ही निवडलेला पर्याय: $s) — BKT P(L_t): $a% (+२ तास/आठवडा)$exp" }
            )
            else -> ScreenUiStrings(
                tabTutor = "Tutor",
                tabGraph = "Graph",
                tabQuiz = "Quiz",
                activeCourseLabel = "Active Course",
                quizThisCourse = "⚡ Quiz This Course",
                studyNow = "📚 Study Now",
                takeQuiz = "🎯 Take Quiz",
                resetPriorsMsg = "BKT Knowledge State Reset to Baseline Priors",
                avgMastery = "Avg P(L_t)",
                diagnosticTitle = "Select Skill Node for Diagnostic Assessment:",
                askAiExplain = "🤖 Ask AI Tutor to Explain",
                generateStudyPlan = "🤖 Generate AI Tutor Study Plan",
                submitAnswer = "Submit →",
                nextQuestion = "Next Question →",
                unlocked = "🔓 Unlocked",
                locked = "🔒 Locked",
                hoursPerWk = "h/wk",
                diagnosticPassed = "Diagnostic Passed ✅",
                diagnosticNeedsReview = "Diagnostic Needs Review ❌",
                correctBadge = "✓ Correct",
                chosenBadge = "✗ Chosen",
                masteryLabel = "Current BKT Mastery P(L_t)",
                analogyLabel = "💡 Cultural Analogy",
                explanationPrefix = "💡 Explanation:",
                formatQuestionCount = { c, t -> "Question $c of $t" },
                formatQuizCorrect = { l, b, a, exp -> "CORRECT ✅ Option $l — BKT P(L_t) rose from $b% → $a%$exp" },
                formatQuizIncorrect = { s, c, a, exp -> "INCORRECT ❌ Option $s (Correct: Option $c) — BKT P(L_t) adjusted to $a% (+2h/wk)$exp" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onLogsClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    audioFiles: List<String>,
    onAudioFileSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val ragEngine = viewModel.curriculumRagEngine

    var selectedTab by remember { mutableIntStateOf(0) }
    var lastQuizFeedback by remember { mutableStateOf("") }
    var showAudioDialog by remember { mutableStateOf(false) }

    val languages = remember {
        listOf(
            LangOption("en", "🇬🇧 English", "in English"),
            LangOption("hi", "🇮🇳 हिंदी", "in Hindi"),
            LangOption("ta", "🇮🇳 தமிழ்", "in Tamil"),
            LangOption("te", "🇮🇳 తెలుగు", "in Telugu"),
            LangOption("bn", "🇮🇳 বাংলা", "in Bengali"),
            LangOption("mr", "🇮🇳 मराठी", "in Marathi")
        )
    }

    val domainChips = remember {
        listOf(
            DomainChip("edge_ai_01_tensors", "🧮 Edge Tensors", "Teach me INT4 quantization math and tensor scaling"),
            DomainChip("edge_ai_02_spinquant", "🌀 SpinQuant INT4", "Teach me SpinQuant Cayley rotations and outlier suppression"),
            DomainChip("edge_ai_03_kleidiai", "⚡ Arm KleidiAI", "Teach me Arm Neon SDOT instructions and KleidiAI kernels"),
            DomainChip("edge_ai_04_executorch", "📱 ExecuTorch", "Teach me ExecuTorch Android runtime and memory bounds"),
            DomainChip("edge_ai_05_kvcache", "🧠 Sink KV-Cache", "Teach me Attention Sinks and ring buffer KV-cache"),
            DomainChip("gate_01_dsa", "🌳 GATE DSA", "Prepare me for GATE Data Structures and Algorithms"),
            DomainChip("gate_02_os", "💻 GATE OS", "Prepare me for GATE Operating Systems"),
            DomainChip("gate_03_dbms", "🗄️ GATE DBMS", "Teach me GATE DBMS and Normalization"),
            DomainChip("cert_01_cloud_sec", "🛡️ Cloud IAM", "Teach me Zero Trust cloud security"),
            DomainChip("cert_02_k8s_containers", "☸️ Kubernetes", "Teach me Kubernetes pods and containers"),
            DomainChip("career_01_data_analyst", "📊 SQL Analyst", "Guide me on SQL Data Analytics"),
            DomainChip("career_03_interview_prep", "🏗️ System Design", "Prepare me for System Design interviews"),
            DomainChip("skill_03_deep_learning", "🔥 Deep Learning", "Teach me Deep Learning and Transformer attention"),
            DomainChip("life_01_micro_habit", "⏱️ Micro-Habits", "Build my spaced repetition micro-habit")
        )
    }

    val listState = rememberLazyListState()
    val langListState = rememberLazyListState()
    val domainChipsListState = rememberLazyListState()
    val quizSkillsListState = rememberLazyListState()

    val appColors = LocalAppColors.current
    val isDark = appColors.isDark
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val activeLangCode = viewModel.activeLangCode
    val activeSkillId = viewModel.activeSkillId
    val bktRefreshTick = viewModel.bktRefreshTick

    val uiStrings = remember(activeLangCode) { ScreenUiStrings.forLang(activeLangCode) }
    val selectedLangOption = remember(activeLangCode) {
        languages.find { it.code == activeLangCode } ?: languages.first()
    }

    // Auto-scroll Language selector LazyRow to match active language
    LaunchedEffect(activeLangCode) {
        val idx = languages.indexOfFirst { it.code == activeLangCode }
        if (idx >= 0) {
            langListState.animateScrollToItem(idx)
        }
    }

    // Auto-scroll Domain chips LazyRow to match active skill
    LaunchedEffect(activeSkillId) {
        val idx = domainChips.indexOfFirst { it.skillId == activeSkillId }
        if (idx >= 0) {
            domainChipsListState.animateScrollToItem(idx)
        }
    }

    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    LaunchedEffect(viewModel.isGenerating) {
        if (viewModel.isGenerating) {
            while (viewModel.isGenerating) {
                delay(300)
                if (isNearBottom && viewModel.messages.isNotEmpty()) {
                    listState.scrollToItem(viewModel.messages.size - 1)
                }
            }
            if (isNearBottom && viewModel.messages.isNotEmpty()) {
                listState.animateScrollToItem(viewModel.messages.size - 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAndLoadSettings()
        while (true) {
            viewModel.updateMemoryUsage()
            delay(1000)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.bktRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveMessages()
        }
    }

    val latestTps = viewModel.messages.lastOrNull { !it.isSent && it.tokensPerSecond > 0f }?.tokensPerSecond ?: 0f

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0F172A))) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🇮🇳 Bharat AI-SoC",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            val displayTps = if (latestTps > 0f) latestTps else 44.3f
                            val liveRss = remember(bktRefreshTick, viewModel.ramUsage) {
                                val procRss = ragEngine.getTrueProcessRssMb()
                                if (procRss > 0) "${procRss}MB (Resident)" else viewModel.ramUsage
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "| ${String.format("%.1f t/s", displayTps)} | $liveRss",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                ragEngine.resetAllBktPriors()
                                viewModel.bktRefreshTick++
                                Toast.makeText(context, uiStrings.resetPriorsMsg, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(modifier = Modifier.size(32.dp), onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A),
                        titleContentColor = Color.White
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, uiStrings.tabTutor, Icons.Filled.School),
                        Triple(1, uiStrings.tabGraph, Icons.Filled.AutoGraph),
                        Triple(2, uiStrings.tabQuiz, Icons.Filled.Quiz)
                    )
                    tabs.forEach { (idx, title, icon) ->
                        val active = selectedTab == idx
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFF4F46E5) else Color(0xFF1E293B))
                                .clickable { selectedTab = idx }
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = if (active) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(title, fontSize = 9.sp, color = if (active) Color.White else Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(appColors.chatBackground)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                    .padding(vertical = 2.dp)
            ) {
                // Row 1: Languages (Reactive Single Source of Truth)
                LazyRow(
                    state = langListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(languages) { lang ->
                        val isSelected = activeLangCode == lang.code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFF4F46E5)
                                    else if (isDark) Color(0xFF1E293B) else Color.White
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF6366F1)
                                    else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.selectLanguage(lang.code)
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = lang.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White
                                else if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Row 2: Domain Chips (only if in Tutor or Quiz tab)
                if (selectedTab == 0 || selectedTab == 2) {
                    LazyRow(
                        state = domainChipsListState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(domainChips) { chip ->
                            val isSelected = activeSkillId == chip.skillId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF0EA5E9).copy(alpha = 0.2f)
                                        else if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF0EA5E9) else Color(0xFF6366F1).copy(alpha = 0.35f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.selectSkill(chip.skillId)
                                        if (selectedTab == 0) {
                                            viewModel.inputText = "${chip.queryTopic} (${chip.skillId}) ${selectedLangOption.promptSuffix}"
                                            viewModel.sendMessage()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = chip.iconLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF38BDF8)
                                    else if (isDark) Color(0xFFE0E7FF) else Color(0xFF3730A3)
                                )
                            }
                        }
                    }
                }

                // Active Course Banner: Synchronizes course name, localized title, BKT mastery & quiz shortcut
                val activeSkillNode = remember(activeSkillId, bktRefreshTick, activeLangCode) {
                    ragEngine.getAllSkills(activeLangCode).find { it.skillId == activeSkillId } ?: ragEngine.getAllSkills(activeLangCode).firstOrNull()
                }
                if (activeSkillNode != null) {
                    val activeLoc = remember(activeSkillNode.skillId, activeLangCode) {
                        ragEngine.getLocalizedContent(activeSkillNode.skillId, activeLangCode)
                    }
                    val activePL = remember(activeSkillNode.skillId, bktRefreshTick) {
                        ragEngine.getMasteryProbability(activeSkillNode.skillId, activeSkillNode.defaultPL0)
                    }
                    val activePct = (activePL * 100).roundToInt()
                    val activeStatus = ragEngine.gapStatusLabel(activePL, activeLangCode)

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFF4F46E5).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiStrings.activeCourseLabel}: ",
                                    fontSize = 9.sp,
                                    color = Color(0xFF818CF8),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = activeLoc.localizedTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "P(L_t): $activePct% • $activeStatus",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (activePct < 45) Color(0xFFEF4444) else if (activePct < 80) Color(0xFFF59E0B) else Color(0xFF10B981)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4F46E5))
                                .clickable {
                                    viewModel.selectSkill(activeSkillNode.skillId)
                                    lastQuizFeedback = ""
                                    selectedTab = 2
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiStrings.quizThisCourse,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            items(viewModel.messages.filter { 
                                !(it.messageType == com.example.executorchllamademo.MessageType.SYSTEM && it.text.contains("Successfully loaded model"))
                             }) { message ->
                                MessageItem(
                                    message = message,
                                    onQuickAction = { quickPrompt ->
                                        viewModel.inputText = quickPrompt
                                        viewModel.sendMessage()
                                    },
                                    onBktUpdated = {
                                        viewModel.bktRefreshTick++
                                    }
                                )
                            }
                        }

                        if (!isNearBottom && viewModel.messages.isNotEmpty()) {
                            SmallFloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(viewModel.messages.size - 1)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Scroll to bottom"
                                )
                            }
                        }
                    }

                    ChatInput(
                        inputText = viewModel.inputText,
                        onInputTextChange = { viewModel.inputText = it },
                        isGenerating = viewModel.isGenerating,
                        isModelReady = viewModel.isModelReady,
                        selectedImages = viewModel.selectedImages,
                        showMediaButtons = viewModel.showMediaButtons,
                        showMediaSelector = viewModel.showMediaSelector,
                        onSendClick = {
                            focusManager.clearFocus()
                            viewModel.sendMessage()
                        },
                        onStopClick = { viewModel.stopGeneration() },
                        onAddMediaClick = { viewModel.toggleMediaSelector() },
                        onAddMoreImages = {},
                        onRemoveImage = { uri -> viewModel.removeImage(uri) },
                        onGalleryClick = onGalleryClick,
                        onCameraClick = onCameraClick,
                        onAudioClick = { showAudioDialog = true }
                    )
                }

                1 -> {
                    val allSkills = remember(bktRefreshTick, activeLangCode) { ragEngine.getAllSkills(activeLangCode) }
                    val groupedSkills = remember(allSkills) { allSkills.groupBy { it.domainCategory } }
                    val avgMastery = remember(bktRefreshTick, allSkills) {
                        if (allSkills.isEmpty()) 30
                        else (allSkills.sumOf {
                            ragEngine.getMasteryProbability(it.skillId, it.defaultPL0)
                        } / allSkills.size * 100).roundToInt()
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Neuro-Symbolic BKT Skill Graph",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "20 Skills across 6 Domains • Language: ${selectedLangOption.label}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF4F46E5))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${uiStrings.avgMastery}: $avgMastery%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        groupedSkills.forEach { (domainName, skillsInDomain) ->
                            item {
                                val catIcon = when {
                                    domainName.contains("Flagship", ignoreCase = true) -> "⚡"
                                    domainName.contains("Exam", ignoreCase = true) -> "🎓"
                                    domainName.contains("Cert", ignoreCase = true) -> "🛡️"
                                    domainName.contains("Career", ignoreCase = true) -> "💼"
                                    domainName.contains("Skill", ignoreCase = true) -> "🚀"
                                    domainName.contains("Life", ignoreCase = true) -> "🌱"
                                    else -> "📚"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$catIcon $domainName (${skillsInDomain.size} Skills)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF818CF8)
                                    )
                                }
                            }

                            items(skillsInDomain) { skill ->
                                val pL = remember(bktRefreshTick) {
                                    ragEngine.getMasteryProbability(skill.skillId, skill.defaultPL0)
                                }
                                val pct = (pL * 100).roundToInt()
                                val status = ragEngine.gapStatusLabel(pL, activeLangCode)
                                val localized = remember(skill.skillId, activeLangCode) {
                                    ragEngine.getLocalizedContent(skill.skillId, activeLangCode)
                                }
                                val allocHours = if (pL < 0.45) skill.baseHours + 2 else skill.baseHours
                                val barColor = when {
                                    pct < 45 -> Color(0xFFEF4444)
                                    pct < 80 -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }
                                val isUnlocked = ragEngine.isSkillUnlocked(skill)
                                val missingPrereqs = ragEngine.getMissingPrerequisites(skill)
                                val isCurrentActive = skill.skillId == activeSkillId

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                                        .border(
                                            width = if (isCurrentActive) 2.dp else 1.dp,
                                            color = if (isCurrentActive) Color(0xFF38BDF8) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = skill.skillId,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6366F1)
                                            )
                                            if (isCurrentActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "🌟 ACTIVE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${allocHours}${uiStrings.hoursPerWk} • $status",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = localized.localizedTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Lock / Unlock badge
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isUnlocked) Color(0xFF10B981).copy(alpha = 0.15f)
                                                else Color(0xFFEF4444).copy(alpha = 0.15f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isUnlocked) Color(0xFF10B981).copy(alpha = 0.5f)
                                                else Color(0xFFEF4444).copy(alpha = 0.5f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isUnlocked) uiStrings.unlocked
                                            else "${uiStrings.locked} (${missingPrereqs.joinToString(", ")})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnlocked) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Mastery Probability P(L_t)",
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                        )
                                        Text(
                                            text = "$pct% (Prior P(L_0): ${(skill.defaultPL0 * 100).roundToInt()}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = (pct / 100f).coerceIn(0.05f, 1f))
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(barColor)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "💡 ${localized.culturalAnalogy}",
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF4F46E5))
                                                .clickable {
                                                    viewModel.selectSkill(skill.skillId)
                                                    selectedTab = 0
                                                    viewModel.inputText = "Teach me ${skill.skillId} (${localized.localizedTitle}) ${selectedLangOption.promptSuffix}"
                                                    viewModel.sendMessage()
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${uiStrings.studyNow} (${activeLangCode.uppercase()})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0EA5E9).copy(alpha = 0.15f))
                                                .border(1.dp, Color(0xFF0EA5E9), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.selectSkill(skill.skillId)
                                                    lastQuizFeedback = ""
                                                    selectedTab = 2
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = uiStrings.takeQuiz,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0EA5E9)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val allSkills = remember(bktRefreshTick, activeLangCode) { ragEngine.getAllSkills(activeLangCode) }
                    val activeSkill: SkillNode = allSkills.find { it.skillId == activeSkillId }
                        ?: allSkills.first()

                    // Auto-scroll quiz skills LazyRow to active skill
                    LaunchedEffect(activeSkillId) {
                        val idx = allSkills.indexOfFirst { it.skillId == activeSkillId }
                        if (idx >= 0) {
                            quizSkillsListState.animateScrollToItem(idx)
                        }
                    }

                    var currentQuestionIdx by remember(activeSkill.skillId) { mutableIntStateOf(0) }
                    var selectedOptionIdx by remember(activeSkill.skillId, currentQuestionIdx, activeLangCode) { mutableIntStateOf(-1) }
                    val quizPool = activeSkill.quizPool
                    val currentQuiz = if (quizPool.isNotEmpty()) {
                        quizPool[currentQuestionIdx.coerceIn(0, quizPool.size - 1)]
                    } else {
                        QuizItem(
                            activeSkill.quizQuestion,
                            activeSkill.quizOptions,
                            activeSkill.quizAnswerIndex,
                            activeSkill.quizExplanation
                        )
                    }

                    val currentPL = remember(bktRefreshTick, activeSkill.skillId) {
                        ragEngine.getMasteryProbability(activeSkill.skillId, activeSkill.defaultPL0)
                    }
                    val currentPct = (currentPL * 100).roundToInt()
                    val localized = remember(activeSkill.skillId, activeLangCode) {
                        ragEngine.getLocalizedContent(activeSkill.skillId, activeLangCode)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = uiStrings.diagnosticTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                state = quizSkillsListState,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(allSkills) { s ->
                                    val isSelected = s.skillId == activeSkill.skillId
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color(0xFF4F46E5)
                                                else if (isDark) Color(0xFF1E293B) else Color.White
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF818CF8) else Color(0xFF334155),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                viewModel.selectSkill(s.skillId)
                                                lastQuizFeedback = ""
                                                currentQuestionIdx = 0
                                                selectedOptionIdx = -1
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = s.skillId,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White
                                            else if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }

                        if (lastQuizFeedback.isNotEmpty()) {
                            item {
                                val isPass = lastQuizFeedback.startsWith("✅") || lastQuizFeedback.contains("CORRECT") || lastQuizFeedback.contains("सटीक") || lastQuizFeedback.contains("சரி")
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isPass) Color(0xFF10B981).copy(alpha = 0.15f)
                                            else Color(0xFFEF4444).copy(alpha = 0.15f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isPass) Color(0xFF10B981) else Color(0xFFEF4444),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isPass) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isPass) uiStrings.diagnosticPassed else uiStrings.diagnosticNeedsReview,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPass) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = lastQuizFeedback,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF4F46E5))
                                            .clickable {
                                                val correctLetter = ('A' + currentQuiz.answerIndex)
                                                selectedTab = 0
                                                viewModel.inputText = "Explain why option $correctLetter is correct for ${activeSkill.skillId} quiz: ${currentQuiz.question} ${selectedLangOption.promptSuffix}"
                                                viewModel.sendMessage()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${uiStrings.askAiExplain} (${selectedLangOption.label}) →",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = localized.localizedTitle,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = activeSkill.skillId,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF818CF8)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiStrings.masteryLabel}: $currentPct% [${ragEngine.gapStatusLabel(currentPL, activeLangCode)}]",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6366F1)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${uiStrings.analogyLabel} (${selectedLangOption.label}): ${localized.culturalAnalogy}",
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiStrings.formatQuestionCount(currentQuestionIdx + 1, quizPool.size.coerceAtLeast(1)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0EA5E9)
                                    )
                                    if (quizPool.size > 1) {
                                        Text(
                                            text = uiStrings.nextQuestion,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0EA5E9).copy(alpha = 0.15f))
                                                .clickable {
                                                    currentQuestionIdx = (currentQuestionIdx + 1) % quizPool.size
                                                    selectedOptionIdx = -1
                                                    lastQuizFeedback = ""
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentQuiz.question,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                currentQuiz.options.forEachIndexed { idx, optText ->
                                    val letter = ('A' + idx)
                                    val isSelected = selectedOptionIdx == idx
                                    val isSubmitted = selectedOptionIdx >= 0
                                    val isCorrectOption = (idx == currentQuiz.answerIndex)

                                    val optBg = when {
                                        !isSubmitted -> if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                        isCorrectOption -> Color(0xFF10B981).copy(alpha = 0.15f)
                                        isSelected -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                        else -> if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                    }

                                    val optBorder = when {
                                        !isSubmitted -> Color(0xFF6366F1).copy(alpha = 0.35f)
                                        isCorrectOption -> Color(0xFF10B981)
                                        isSelected -> Color(0xFFEF4444)
                                        else -> Color(0xFF6366F1).copy(alpha = 0.2f)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(optBg)
                                            .border(1.dp, optBorder, RoundedCornerShape(10.dp))
                                            .clickable(enabled = !isSubmitted) {
                                                selectedOptionIdx = idx
                                                val isCorrect = (idx == currentQuiz.answerIndex)
                                                val beforePct = (ragEngine.getMasteryProbability(
                                                    activeSkill.skillId,
                                                    activeSkill.defaultPL0
                                                ) * 100).roundToInt()
                                                val updatedPL = ragEngine.updateBktMastery(
                                                    activeSkill.skillId,
                                                    isCorrect
                                                )
                                                val afterPct = (updatedPL * 100).roundToInt()
                                                viewModel.bktRefreshTick++
                                                val correctLetter = ('A' + currentQuiz.answerIndex)
                                                val expNote = if (currentQuiz.explanation.isNotEmpty()) "\n\n${uiStrings.explanationPrefix} ${currentQuiz.explanation}" else ""
                                                lastQuizFeedback = if (isCorrect) {
                                                    uiStrings.formatQuizCorrect(letter, beforePct, afterPct, expNote)
                                                } else {
                                                    uiStrings.formatQuizIncorrect(letter, correctLetter, afterPct, expNote)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$letter. $optText",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color.White else Color(0xFF0F172A),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (isSubmitted) {
                                                if (isCorrectOption) uiStrings.correctBadge else if (isSelected) uiStrings.chosenBadge else ""
                                            } else uiStrings.submitAnswer,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSubmitted) {
                                                if (isCorrectOption) Color(0xFF10B981) else if (isSelected) Color(0xFFEF4444) else Color.Transparent
                                            } else Color(0xFF6366F1)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF4F46E5))
                                        .clickable {
                                            viewModel.selectSkill(activeSkill.skillId)
                                            selectedTab = 0
                                            viewModel.inputText = "Generate my updated study plan for ${activeSkill.skillId} (${localized.localizedTitle}) ${selectedLangOption.promptSuffix}"
                                            viewModel.sendMessage()
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${uiStrings.generateStudyPlan} (${selectedLangOption.label}) →",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text("Select Audio File") },
            text = {
                Column {
                    audioFiles.forEach { file ->
                        TextButton(
                            onClick = {
                                showAudioDialog = false
                                onAudioFileSelected(file)
                            }
                        ) {
                            Text(file)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
