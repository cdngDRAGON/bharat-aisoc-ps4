/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.executorchllamademo.ui.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.executorchllamademo.BackendType
import com.example.executorchllamademo.AppSettings
import com.example.executorchllamademo.DemoSharedPreferences
import com.example.executorchllamademo.ETImage
import com.example.executorchllamademo.ETLogging
import com.example.executorchllamademo.Message
import com.example.executorchllamademo.MessageType
import com.example.executorchllamademo.ModelConfiguration
import com.example.executorchllamademo.ModelType
import com.example.executorchllamademo.ModelUtils
import com.example.executorchllamademo.PromptFormat
import com.example.executorchllamademo.ModuleSettings
import com.example.executorchllamademo.CurriculumRagEngine
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.reflect.TypeToken
import org.json.JSONException
import org.json.JSONObject
import org.pytorch.executorch.ExecutorchRuntimeException
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ChatViewModel(application: Application) : AndroidViewModel(application), LlmCallback {

    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> = _messages

    var activeLangCode by mutableStateOf("en")
    var activeSkillId by mutableStateOf("edge_ai_01_tensors")
    var bktRefreshTick by mutableIntStateOf(0)
    var currentLanguageCode by mutableStateOf("en")
    var inputText by mutableStateOf("")
    var isModelReady by mutableStateOf(false)
    var isGenerating by mutableStateOf(false)
    var thinkMode by mutableStateOf(false)
    var showMediaSelector by mutableStateOf(false)
    var ramUsage by mutableStateOf("0 MB")
    var showMediaButtons by mutableStateOf(false)
    var supportsImageInput by mutableStateOf(false)
    var supportsAudioInput by mutableStateOf(false)

    // Thinking mode state: tracks whether we're inside a <think>...</think> block
    private var isInThinkingBlock = false

    private val _selectedImages = mutableStateListOf<Uri>()
    val selectedImages: List<Uri> = _selectedImages

    // Dialog states
    var showModelLoadErrorDialog by mutableStateOf(false)
    var modelLoadError by mutableStateOf("")

    // LoRA mode state
    var isLoraMode by mutableStateOf(false)
        private set
    var availableModels by mutableStateOf<List<ModelConfiguration>>(emptyList())
        private set
    var activeModelId by mutableStateOf("")
        private set

    // Map of loaded LlmModules by model ID for LoRA mode
    private val loadedModules = mutableMapOf<String, LlmModule>()

    // Delegate to process-wide singleton fields to guarantee strictly 1 native C++ module exists per process
    private var module: LlmModule?
        get() = sharedNativeModule
        set(value) {
            sharedNativeModule = value
        }
    private var resultMessage: Message? = null
    private val demoSharedPreferences = DemoSharedPreferences(application)
    private val curriculumEngine = CurriculumRagEngine(application)
    private var currentSettingsFields = ModuleSettings()
    private var appSettings = AppSettings()
    private var promptID = 0
    private var sawStartHeaderId = false
    private var audioFileToPrefill: String? = null
    private var shouldAddSystemPrompt = true

    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val contentResolver = application.contentResolver
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    val curriculumRagEngine: CurriculumRagEngine get() = curriculumEngine

    fun selectLanguage(langCode: String) {
        activeLangCode = langCode
        currentLanguageCode = langCode
        curriculumEngine.setActiveLanguageCode(langCode)
        bktRefreshTick++
        updateWelcomeSummary(langCode)
    }

    fun selectSkill(skillId: String) {
        activeSkillId = skillId
        curriculumEngine.setActiveSkillId(skillId)
        bktRefreshTick++
    }

    fun updateWelcomeSummary(langCode: String = activeLangCode) {
        val welcomeText = curriculumEngine.buildWelcomeSummary(langCode)
        val existingIndex = _messages.indexOfFirst { msg ->
            !msg.isSent && isWelcomeMessage(msg.text)
        }
        if (existingIndex >= 0) {
            _messages[existingIndex] = Message(welcomeText, false, MessageType.TEXT, 0)
        } else if (_messages.isEmpty() || !_messages.any { it.isSent }) {
            _messages.add(0, Message(welcomeText, false, MessageType.TEXT, 0))
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

    init {
        activeLangCode = curriculumEngine.getActiveLanguageCode()
        activeSkillId = curriculumEngine.getActiveSkillId()
        currentLanguageCode = activeLangCode

        // Check for clear chat history flag BEFORE loading saved messages
        val moduleSettings = demoSharedPreferences.getModuleSettings()
        appSettings = demoSharedPreferences.getAppSettings()
        if (moduleSettings.isClearChatHistory) {
            // Clear the flag and don't load messages
            // Keep isLoadModel flag so model still loads in checkAndLoadSettings()
            demoSharedPreferences.removeExistingMessages()
            demoSharedPreferences.saveModuleSettings(moduleSettings.copy(isClearChatHistory = false))
            // Don't update currentSettingsFields here - let checkAndLoadSettings() handle it
            // so it detects the change and loads the model if needed
        } else {
            loadSavedMessages()
        }
        updateWelcomeSummary(activeLangCode)
    }

    private fun loadSavedMessages() {
        val appSettings = demoSharedPreferences.getAppSettings()
        // Only load saved messages if saveChatHistory is enabled
        if (!appSettings.saveChatHistory) {
            // Clear any existing saved messages since saving is disabled
            demoSharedPreferences.removeExistingMessages()
            return
        }

        val existingMsgJSON = demoSharedPreferences.getSavedMessages()
        if (existingMsgJSON.isNotEmpty()) {
            // Use InstanceCreator so that Gson calls the Message constructor (which
            // assigns default values like id = UUID). Without this, Gson uses
            // Unsafe.allocateInstance() and fields missing from old JSON become null.
            val gson = GsonBuilder()
                .registerTypeAdapter(Message::class.java, InstanceCreator<Message> {
                    Message("", false, MessageType.TEXT, 0)
                })
                .create()
            val type = object : TypeToken<ArrayList<Message>>() {}.type
            val savedMessages: ArrayList<Message>? = gson.fromJson(existingMsgJSON, type)
            savedMessages?.let {
                _messages.addAll(it)
                promptID = _messages.maxOfOrNull { msg -> msg.promptID }?.plus(1) ?: 0
            }
        }
    }

    fun saveMessages() {
        val appSettings = demoSharedPreferences.getAppSettings()
        // Only save messages if saveChatHistory is enabled
        if (appSettings.saveChatHistory) {
            demoSharedPreferences.addMessages(_messages.toList())
        } else {
            // Make sure no messages are persisted
            demoSharedPreferences.removeExistingMessages()
        }
    }

    private val systemPromptMessage = "To get started, select your desired model and tokenizer from the top right corner"

    fun checkAndLoadSettings() {
        if (globalModelLoadLock.get()) {
            Log.d("ChatViewModel", "Model loading already in progress process-wide, skipping checkAndLoadSettings")
            return
        }
        val updatedSettingsFields = demoSharedPreferences.getModuleSettings()
        appSettings = demoSharedPreferences.getAppSettings()
        val isUpdated = currentSettingsFields != updatedSettingsFields
        val isLoadModel = updatedSettingsFields.isLoadModel

        // Update LoRA mode state
        isLoraMode = updatedSettingsFields.isLoraMode
        availableModels = updatedSettingsFields.models
        activeModelId = updatedSettingsFields.activeModelId

        if (isUpdated) {
            val settingsAfterClear = checkForClearChatHistory(updatedSettingsFields)

            if (isLoadModel) {
                // Update local copy BEFORE checking media capabilities
                val settingsWithLoadFlagCleared = settingsAfterClear.copy(isLoadModel = false)
                currentSettingsFields = settingsWithLoadFlagCleared
                demoSharedPreferences.saveModuleSettings(settingsWithLoadFlagCleared)

                // Update media capabilities after settings are updated
                setBackendMode(settingsAfterClear.backendType)

                if (isLoraMode && settingsAfterClear.hasModels()) {
                    // LoRA mode: Load all configured models
                    loadLoraModels(settingsAfterClear)
                } else {
                    // Legacy single-model mode
                    val resolved = resolveReadableModelPaths(getApplication())
                    val effectiveModelPath = if (File(settingsAfterClear.modelFilePath).let { it.exists() && it.canRead() && it.length() > 500_000_000L }) {
                        settingsAfterClear.modelFilePath
                    } else {
                        resolved?.first ?: settingsAfterClear.modelFilePath
                    }
                    val effectiveTokPath = if (File(settingsAfterClear.tokenizerFilePath).let { it.exists() && it.canRead() }) {
                        settingsAfterClear.tokenizerFilePath
                    } else {
                        resolved?.second ?: settingsAfterClear.tokenizerFilePath
                    }

                    loadLocalModelAndParameters(
                        effectiveModelPath,
                        effectiveTokPath,
                        settingsAfterClear.dataPath,
                        settingsAfterClear.temperature.toFloat()
                    )
                }
            } else {
                currentSettingsFields = settingsAfterClear.copy()
                setBackendMode(settingsAfterClear.backendType)
                autoLoadDefaultModelIfNeeded(settingsAfterClear)
            }
        } else {
            setBackendMode(updatedSettingsFields.backendType)
            autoLoadDefaultModelIfNeeded(updatedSettingsFields)
        }
    }

    /**
     * Resolves readable model and tokenizer paths across SELinux-safe app storage and local tmp.
     */
    fun resolveReadableModelPaths(context: Context): Pair<String, String>? {
        val candidateDirs = listOfNotNull(
            context.getExternalFilesDir(null)?.let { File(it, "llama") },
            File(context.filesDir, "llama"),
            File("/data/local/tmp/llama")
        )
        for (dir in candidateDirs) {
            val pte = File(dir, "Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte")
            val tok = File(dir, "tokenizer.model")
            if (pte.exists() && pte.canRead() && pte.length() > 500_000_000L &&
                tok.exists() && tok.canRead() && tok.length() > 100_000L) {
                Log.i("ChatViewModel", "Resolved readable model files at: ${dir.absolutePath} (size=${pte.length()} bytes)")
                return Pair(pte.absolutePath, tok.absolutePath)
            }
        }
        Log.w("ChatViewModel", "No readable model files found in: ${candidateDirs.map { it.absolutePath }}")
        return null
    }

    private fun autoLoadDefaultModelIfNeeded(settings: ModuleSettings) {
        val resolved = resolveReadableModelPaths(getApplication())
        if (resolved != null) {
            val (modelPath, tokenizerPath) = resolved
            // If process-wide singleton is already loaded with the same model, reuse it immediately!
            if (sharedNativeModule != null && sharedLoadedModelPath == modelPath) {
                Log.i("ChatViewModel", "autoLoadDefaultModelIfNeeded: Reusing process-wide singleton LlmModule for $modelPath")
                try {
                    sharedNativeModule?.resetContext()
                } catch (t: Throwable) {
                    Log.w("ChatViewModel", "resetContext on reused module: ${t.message}")
                }
                isModelReady = true
                updateMemoryUsage()
                updateWelcomeSummary(activeLangCode)
                Log.i("BHARAT_PS4_READY", "MODEL_AND_BKT_READY (SINGLETON_REUSED) RAM=" + ramUsage)
                return
            }
            if (globalModelLoadLock.get() || isModelReady || module != null || loadedModules.isNotEmpty()) return
            val autoSettings = settings.copy(
                modelFilePath = modelPath,
                tokenizerFilePath = tokenizerPath,
                modelType = ModelType.LLAMA_3,
                isLoraMode = false,
                isLoadModel = false
            )
            currentSettingsFields = autoSettings
            demoSharedPreferences.saveModuleSettings(autoSettings)
            loadLocalModelAndParameters(modelPath, tokenizerPath, autoSettings.dataPath, autoSettings.temperature.toFloat())
        } else {
            isModelReady = false
            val missingMsg = "Model files not accessible in app storage (/sdcard/Android/data/.../files/llama/ or /data/local/tmp/llama/). Please ensure Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte and tokenizer.model are present."
            Log.w("ChatViewModel", missingMsg)
            addSystemMessage(missingMsg)
        }
    }

    private fun loadLoraModels(settings: ModuleSettings) {
        Thread {
            val sharedDataPath = settings.getEffectiveDataPath()
            
            // Build detailed loading message with args for each model
            val loadingDetails = StringBuilder("Loading ${settings.models.size} LoRA model(s):\n")
            settings.models.forEachIndexed { index, modelConfig ->
                if (modelConfig.isValid()) {
                    val dataFiles = mutableListOf<String>()
                    if (sharedDataPath.isNotEmpty()) {
                        dataFiles.add(sharedDataPath)
                    }
                    dataFiles.addAll(modelConfig.adapterFilePaths)
                    
                    loadingDetails.append("\n${index + 1}. ${modelConfig.displayName}\n")
                    loadingDetails.append("   PTE: ${modelConfig.modelFilePath.substringAfterLast('/')}\n")
                    loadingDetails.append("   Tokenizer: ${modelConfig.tokenizerFilePath.substringAfterLast('/')}\n")
                    loadingDetails.append("   Data files: ${if (dataFiles.isEmpty()) "none" else dataFiles.joinToString(", ") { it.substringAfterLast('/') }}\n")
                }
            }
            
            val modelLoadingMessage = Message(loadingDetails.toString(), false, MessageType.SYSTEM, 0)
            _messages.add(modelLoadingMessage)
            isModelReady = false

            var loadedCount = 0
            var firstLoadedModelId: String? = null

            for (modelConfig in settings.models) {
                if (!modelConfig.isValid()) continue

                try {
                    // Build dataFiles list: foundation PTD + adapter PTDs
                    val dataFiles = mutableListOf<String>()
                    if (sharedDataPath.isNotEmpty()) {
                        dataFiles.add(sharedDataPath)
                    }
                    dataFiles.addAll(modelConfig.adapterFilePaths)

                    val dataFilesLog = if (dataFiles.isEmpty()) "no data files" else dataFiles.joinToString(", ")
                    ETLogging.getInstance().log(
                        "LoRA: Loading model ${modelConfig.displayName} with tokenizer ${modelConfig.tokenizerFilePath}, data files: $dataFilesLog"
                    )

                    val runStartTime = System.currentTimeMillis()
                    val llmModule = LlmModule(
                        ModelUtils.getModelCategory(modelConfig.modelType, modelConfig.backendType),
                        modelConfig.modelFilePath,
                        modelConfig.tokenizerFilePath,
                        modelConfig.temperature.toFloat(),
                        dataFiles
                    )

                    llmModule.load()
                    val loadDuration = System.currentTimeMillis() - runStartTime

                    // Store in map
                    loadedModules[modelConfig.id] = llmModule
                    loadedCount++

                    if (firstLoadedModelId == null) {
                        firstLoadedModelId = modelConfig.id
                    }

                    ETLogging.getInstance().log(
                        "LoRA: Loaded ${modelConfig.displayName} in ${loadDuration.toFloat() / 1000} sec"
                    )
                } catch (e: ExecutorchRuntimeException) {
                    ETLogging.getInstance().log("LoRA: Failed to load ${modelConfig.displayName}: ${e.message}")
                    _messages.add(Message("Failed to load ${modelConfig.displayName}: ${e.message}", false, MessageType.SYSTEM, 0))
                }
            }

            _messages.remove(modelLoadingMessage)

            if (loadedCount > 0) {
                // Set the active module
                val activeId = if (settings.activeModelId.isNotEmpty() && loadedModules.containsKey(settings.activeModelId)) {
                    settings.activeModelId
                } else {
                    firstLoadedModelId ?: ""
                }

                activeModelId = activeId
                module = loadedModules[activeId]

                val activeModelName = settings.getModelById(activeId)?.displayName ?: "Unknown"
                _messages.add(Message(
                    "Successfully loaded $loadedCount model(s). Active: $activeModelName. Use the switch button to change models.",
                    false, MessageType.SYSTEM, 0
                ))
                isModelReady = true
            } else {
                _messages.add(Message("No models loaded. Please check your configuration.", false, MessageType.SYSTEM, 0))
                isModelReady = false
            }
        }.start()
    }

    /**
     * Switches to a different model in LoRA mode.
     * Creates a new LlmModule with the selected PTE and the same PTD.
     */
    fun switchToModel(modelId: String) {
        if (!isLoraMode) return
        if (isGenerating) {
            addSystemMessage("Cannot switch models while generating. Please wait or stop generation.")
            return
        }

        val modelConfig = currentSettingsFields.getModelById(modelId)
        if (modelConfig == null) {
            addSystemMessage("Model not found.")
            return
        }

        // Check if model is already loaded
        if (loadedModules.containsKey(modelId)) {
            // Just switch to the already loaded module
            module = loadedModules[modelId]
            activeModelId = modelId

            // Update settings with new active model
            currentSettingsFields = currentSettingsFields.setActiveModel(modelId)
            demoSharedPreferences.saveModuleSettings(currentSettingsFields)

            addSystemMessage("Switched to ${modelConfig.displayName}")
            ETLogging.getInstance().log("LoRA: Switched to already loaded model ${modelConfig.displayName}")
        } else {
            // Need to load the model first
            Thread {
                val sharedDataPath = currentSettingsFields.getEffectiveDataPath()
                addSystemMessage("Loading ${modelConfig.displayName}...")
                isModelReady = false

                try {
                    // Build dataFiles list: foundation PTD + adapter PTDs
                    val dataFiles = mutableListOf<String>()
                    if (sharedDataPath.isNotEmpty()) {
                        dataFiles.add(sharedDataPath)
                    }
                    dataFiles.addAll(modelConfig.adapterFilePaths)

                    val runStartTime = System.currentTimeMillis()
                    val llmModule = LlmModule(
                        ModelUtils.getModelCategory(modelConfig.modelType, modelConfig.backendType),
                        modelConfig.modelFilePath,
                        modelConfig.tokenizerFilePath,
                        modelConfig.temperature.toFloat(),
                        dataFiles
                    )

                    llmModule.load()
                    val loadDuration = System.currentTimeMillis() - runStartTime

                    // Store and switch
                    loadedModules[modelId] = llmModule
                    module = llmModule
                    activeModelId = modelId

                    // Update settings
                    currentSettingsFields = currentSettingsFields.setActiveModel(modelId)
                    demoSharedPreferences.saveModuleSettings(currentSettingsFields)

                    addSystemMessage("Switched to ${modelConfig.displayName} (loaded in ${loadDuration.toFloat() / 1000} sec)")
                    ETLogging.getInstance().log("LoRA: Loaded and switched to ${modelConfig.displayName} in ${loadDuration.toFloat() / 1000} sec")
                    isModelReady = true
                } catch (e: ExecutorchRuntimeException) {
                    addSystemMessage("Failed to load ${modelConfig.displayName}: ${e.message}")
                    ETLogging.getInstance().log("LoRA: Failed to load ${modelConfig.displayName}: ${e.message}")
                    isModelReady = loadedModules.isNotEmpty()
                }
            }.start()
        }
    }

    private fun setBackendMode(backendType: BackendType) {
        // Media buttons visibility depends on backend (MediaTek doesn't support media)
        val backendSupportsMedia = when (backendType) {
            BackendType.XNNPACK, BackendType.QUALCOMM, BackendType.VULKAN -> true
            BackendType.MEDIATEK -> false
        }
        updateMediaCapabilities(backendSupportsMedia)
    }

    private fun updateMediaCapabilities(backendSupportsMedia: Boolean) {
        // In LoRA mode, use foundation model type; otherwise use regular model type
        val modelType = if (currentSettingsFields.isLoraMode) {
            currentSettingsFields.foundationModelType
        } else {
            currentSettingsFields.modelType
        }
        
        supportsImageInput = backendSupportsMedia && modelType.supportsImage()
        supportsAudioInput = backendSupportsMedia && modelType.supportsAudio()
        showMediaButtons = supportsImageInput || supportsAudioInput
        ETLogging.getInstance().log("updateMediaCapabilities: modelType=$modelType, supportsImage=${modelType.supportsImage()}, supportsAudio=${modelType.supportsAudio()}, showMediaButtons=$showMediaButtons")
    }

    private fun getCapabilityDescription(modelType: ModelType): String {
        return when {
            modelType.supportsImage() && modelType.supportsAudio() ->
                "You can send text, images, or audio for inference."
            modelType.supportsImage() ->
                "You can send text or images for inference."
            modelType.supportsAudio() ->
                "You can send text or audio for inference."
            else ->
                "You can send text for inference."
        }
    }

    private fun checkForClearChatHistory(updatedSettingsFields: ModuleSettings): ModuleSettings {
        if (updatedSettingsFields.isClearChatHistory) {
            _messages.clear()
            demoSharedPreferences.removeExistingMessages()
            val clearedSettings = updatedSettingsFields.copy(isClearChatHistory = false)
            demoSharedPreferences.saveModuleSettings(clearedSettings)
            module?.resetContext()
            shouldAddSystemPrompt = true
            promptID = 0
            return clearedSettings
        }
        return updatedSettingsFields
    }

    private fun loadLocalModelAndParameters(
        modelFilePath: String,
        tokenizerFilePath: String,
        dataPath: String,
        temperature: Float
    ) {
        // Fast-path: Reuse existing process-wide singleton if path matches
        if (sharedNativeModule != null && sharedLoadedModelPath == modelFilePath) {
            Log.i("ChatViewModel", "loadLocalModelAndParameters: Reusing process-wide singleton LlmModule for $modelFilePath")
            try {
                sharedNativeModule?.resetContext()
            } catch (t: Throwable) {
                Log.w("ChatViewModel", "resetContext on reused module: ${t.message}")
            }
            isModelReady = true
            updateMemoryUsage()
            updateWelcomeSummary(activeLangCode)
            Log.i("BHARAT_PS4_READY", "MODEL_AND_BKT_READY (SINGLETON_REUSED) RAM=" + ramUsage)
            return
        }

        if (!globalModelLoadLock.compareAndSet(false, true)) {
            Log.w("ChatViewModel", "loadLocalModelAndParameters skipped: model load already in progress process-wide")
            return
        }
        Thread {
            try {
                setLocalModel(modelFilePath, tokenizerFilePath, dataPath, temperature)
            } finally {
                globalModelLoadLock.set(false)
            }
        }.start()
    }

    private fun setLocalModel(
        modelPath: String,
        tokenizerPath: String,
        dataPath: String,
        temperature: Float
    ) {
        // Release previous process-wide native instance if present to prevent memory bloat
        sharedNativeModule?.let { oldModule ->
            try {
                Log.i("ChatViewModel", "Releasing previous process-wide native module before loading new model...")
                oldModule.resetNative()
            } catch (t: Throwable) {
                Log.w("ChatViewModel", "resetNative on previous module failed: ${t.message}")
            }
        }
        sharedNativeModule = null
        sharedLoadedModelPath = ""
        System.gc()

        val modelLoadingMessage = Message("Loading model...", false, MessageType.SYSTEM, 0)
        ETLogging.getInstance().log(
            "Loading model $modelPath with tokenizer $tokenizerPath data path $dataPath"
        )

        isModelReady = false
        _messages.add(modelLoadingMessage)

        val runStartTime = System.currentTimeMillis()
        val newModule = if (dataPath.isEmpty()) {
            LlmModule(
                ModelUtils.getModelCategory(
                    currentSettingsFields.modelType,
                    currentSettingsFields.backendType
                ),
                modelPath,
                tokenizerPath,
                temperature
            )
        } else {
            LlmModule(
                ModelUtils.getModelCategory(
                    currentSettingsFields.modelType,
                    currentSettingsFields.backendType
                ),
                modelPath,
                tokenizerPath,
                temperature,
                dataPath
            )
        }

        var loadDuration = 0L
        var modelInfo: String

        var loadSuccess = false
        try {
            val loadResult = newModule.load()
            loadDuration = System.currentTimeMillis() - runStartTime
            if (loadResult == 0) {
                // Execute a short warmup pass to fault-in model weights into RSS and verify execution
                try {
                    newModule.generate("<|begin_of_text|>", 16, object : LlmCallback {
                        override fun onResult(result: String) {}
                        override fun onStats(stats: String) {}
                    }, false)
                    newModule.resetContext()
                } catch (wt: Throwable) {
                    Log.w("ChatViewModel", "Warmup execution notice: ${wt.message}")
                }
            }

            val procRss = curriculumEngine.getTrueProcessRssMb()
            val pteName = modelPath.substringAfterLast('/')
            val tokenizerName = tokenizerPath.substringAfterLast('/')
            val capabilityText = getCapabilityDescription(currentSettingsFields.modelType)

            if (loadResult == 0 && procRss > 600) {
                sharedNativeModule = newModule
                sharedLoadedModelPath = modelPath
                modelInfo = "Successfully loaded model $pteName and tokenizer $tokenizerName in ${loadDuration.toFloat() / 1000} sec. Process RSS: ${procRss} MB. $capabilityText"
                if (currentSettingsFields.modelType == ModelType.LLAVA_1_5) {
                    val llavaPresetPrompt = PromptFormat.getLlavaPresetPrompt()
                    ETLogging.getInstance().log("Llava start prefill prompt: $llavaPresetPrompt")
                    newModule.prefillPrompt(llavaPresetPrompt)
                    ETLogging.getInstance().log("Llava completes prefill prompt")
                }
                loadSuccess = true
            } else {
                modelInfo = "Model load failed: loadResult=$loadResult, Process RSS=${procRss} MB (must be > 600 MB for 1.1GB model)"
                Log.e("ChatViewModel", modelInfo)
                loadSuccess = false
                try {
                    newModule.resetNative()
                } catch (_: Throwable) {}
                sharedNativeModule = null
                sharedLoadedModelPath = ""
            }
        } catch (t: Throwable) {
            modelInfo = "Model load failure: ${t.message}"
            loadDuration = 0
            modelLoadError = modelInfo
            showModelLoadErrorDialog = true
            loadSuccess = false
            try {
                newModule.resetNative()
            } catch (_: Throwable) {}
            sharedNativeModule = null
            sharedLoadedModelPath = ""
            Log.e("ChatViewModel", "Model load exception", t)
        }

        val modelLoadedMessage = Message(modelInfo, false, MessageType.SYSTEM, 0)
        val modelLoggingInfo = "Model path: $modelPath\n" +
                "Tokenizer path: $tokenizerPath\n" +
                "Backend: ${currentSettingsFields.backendType}\n" +
                "ModelType: ${ModelUtils.getModelCategory(currentSettingsFields.modelType, currentSettingsFields.backendType)}\n" +
                "Temperature: $temperature\n" +
                "Model loaded time: $loadDuration ms"
        ETLogging.getInstance().log("Load complete. $modelLoggingInfo")

        isModelReady = loadSuccess
        _messages.remove(modelLoadingMessage)
        _messages.add(modelLoadedMessage)
        if (loadSuccess) {
            updateMemoryUsage()
            updateWelcomeSummary(activeLangCode)
            Log.i("BHARAT_PS4_READY", "MODEL_AND_BKT_READY RAM=" + ramUsage)
            // startAutoPromptWatcher() // Disabled for interactive user mode
        }
    }

    fun updateMemoryUsage() {
        val rssMb = curriculumEngine.getTrueProcessRssMb()
        if (rssMb > 0) {
            ramUsage = "${rssMb} MB"
            if (rssMb > 1780) {
                Log.w("BHARAT_PS4_MEM", "RAM ${rssMb}MB approaching 1800MB threshold, running System.gc()")
                System.gc()
            }
            return
        }
    }

    fun toggleThinkMode() {
        thinkMode = !thinkMode
        val thinkingModeText = if (thinkMode) "on" else "off"
        _messages.add(Message("Thinking mode is $thinkingModeText", false, MessageType.SYSTEM, 0))
    }

    fun toggleMediaSelector() {
        showMediaSelector = !showMediaSelector
    }

    fun addImage(uri: Uri) {
        if (_selectedImages.size < MAX_NUM_OF_IMAGES) {
            _selectedImages.add(uri)
            prefillImageIfNeeded()
        }
    }

    fun removeImage(uri: Uri) {
        _selectedImages.remove(uri)
    }

    fun clearImages() {
        _selectedImages.clear()
    }

    fun setAudioFile(path: String) {
        audioFileToPrefill = path
        _messages.add(Message("Selected audio: $path", false, MessageType.SYSTEM, 0))
    }

    private fun prefillImageIfNeeded() {
        if (currentSettingsFields.modelType == ModelType.LLAVA_1_5 ||
            currentSettingsFields.modelType == ModelType.GEMMA_3
        ) {
            val processedImageList = getProcessedImagesForModel(_selectedImages)
            if (processedImageList.isNotEmpty()) {
                _messages.add(
                    Message("Starting image prefill.", false, MessageType.SYSTEM, 0)
                )
                executor.execute {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE)
                    ETLogging.getInstance().log("Starting runnable prefill image")
                    val img = processedImageList[0]
                    ETLogging.getInstance().log("Starting prefill image")
                    if (currentSettingsFields.modelType == ModelType.LLAVA_1_5) {
                        module?.prefillImages(
                            img.getInts(),
                            img.width,
                            img.height,
                            ModelUtils.VISION_MODEL_IMAGE_CHANNELS
                        )
                    } else if (currentSettingsFields.modelType == ModelType.GEMMA_3) {
                        val gemmaPreImagePrompt = PromptFormat.getGemmaPreImagePrompt()
                        ETLogging.getInstance().log("Gemma prefill pre-image prompt: $gemmaPreImagePrompt")
                        module?.prefillPrompt(gemmaPreImagePrompt)
                        module?.prefillImages(
                            img.getFloats(),
                            img.width,
                            img.height,
                            ModelUtils.VISION_MODEL_IMAGE_CHANNELS
                        )
                    }
                }
            }
        }
    }

    private fun getInputImageSideSize(): Int {
        return when (currentSettingsFields.modelType) {
            ModelType.LLAVA_1_5 -> 336
            ModelType.GEMMA_3 -> 896
            else -> throw IllegalArgumentException("Unsupported model type: ${currentSettingsFields.modelType}")
        }
    }

    private fun getProcessedImagesForModel(uris: List<Uri>): List<ETImage> {
        val imageList = mutableListOf<ETImage>()
        uris.forEach { uri ->
            imageList.add(ETImage(contentResolver, uri, getInputImageSideSize()))
        }
        return imageList
    }

    fun sendMessage() {
        if (inputText.trim().isEmpty()) return
        if (!isModelReady || isGenerating) return

        // Add selected images to chat
        for (imageURI in _selectedImages) {
            _messages.add(Message(imageURI.toString(), true, MessageType.IMAGE, 0))
        }

        val rawPrompt = inputText
        
        val ragTurn = curriculumEngine.processLearnerInput(rawPrompt, activeLangCode)
        activeLangCode = ragTurn.detectedLangCode
        currentLanguageCode = ragTurn.detectedLangCode
        activeSkillId = ragTurn.matchedSkillId
        bktRefreshTick++
        val finalPrompt: String

        if (currentSettingsFields.modelType == ModelType.LLAVA_1_5 && _selectedImages.isNotEmpty()) {
            finalPrompt = PromptFormat.getLlavaMultimodalUserPrompt()
                .replace(PromptFormat.USER_PLACEHOLDER, rawPrompt)
        } else if (currentSettingsFields.modelType == ModelType.GEMMA_3 && _selectedImages.isNotEmpty()) {
            finalPrompt = PromptFormat.getGemmaMultimodalUserPrompt()
                .replace(PromptFormat.USER_PLACEHOLDER, rawPrompt)
        } else {
            // Fresh stateless grounded prompt for every tutoring turn
            val sysFormatted = PromptFormat.getSystemPromptTemplate(currentSettingsFields.getEffectiveModelType())
                .replace(PromptFormat.SYSTEM_PLACEHOLDER, ragTurn.groundedSystemPrompt)
            val userFormatted = currentSettingsFields.getFormattedUserPrompt(ragTurn.groundedUserPrompt, thinkMode)
            finalPrompt = sysFormatted + userFormatted
        }
        shouldAddSystemPrompt = false

        // Add user message
        _messages.add(Message(rawPrompt, true, MessageType.TEXT, promptID))
        inputText = ""
        updateMemoryUsage()

        // Create result message placeholder pre-populated with BKT + FTS5 Micro-RAG Header Card
        resultMessage = Message(ragTurn.uiHeaderCard, false, MessageType.TEXT, promptID)
        sawStartHeaderId = false
        isInThinkingBlock = false
        _messages.add(resultMessage!!)

        // Clear selected images after adding to chat
        _selectedImages.clear()
        showMediaSelector = false
        promptID++

        // Run generation
        executor.execute {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE)
            ETLogging.getInstance().log("starting runnable generate()")
            isGenerating = true

            // Reset KV cache context so each turn starts clean at position 0
            try {
                module?.resetContext()
            } catch (t: Throwable) {
                Log.w("ChatViewModel", "resetContext failed: ${t.message}")
            }

            val targetSeqLen = 768
            val generateStartTime = System.currentTimeMillis()
            if (ModelUtils.getModelCategory(
                    currentSettingsFields.modelType,
                    currentSettingsFields.backendType
                ) == ModelUtils.VISION_MODEL
            ) {
                if (currentSettingsFields.modelType == ModelType.VOXTRAL && audioFileToPrefill != null) {
                    prefillVoxtralAudio(audioFileToPrefill!!, finalPrompt)
                    audioFileToPrefill = null
                    ETLogging.getInstance().log("Running vision model inference.. prompt=(empty after audio prefill)")
                    module?.generate("", targetSeqLen, this, false)
                } else {
                    ETLogging.getInstance().log("Running vision model inference.. prompt=$finalPrompt")
                    module?.generate(finalPrompt, targetSeqLen, this, false)
                }
            } else if (currentSettingsFields.modelType == ModelType.LLAMA_GUARD_3) {
                val llamaGuardPromptForClassification =
                    PromptFormat.getFormattedLlamaGuardPrompt(rawPrompt)
                ETLogging.getInstance().log("Running inference.. prompt=$llamaGuardPromptForClassification")
                module?.generate(
                    llamaGuardPromptForClassification,
                    minOf(llamaGuardPromptForClassification.length + 64, targetSeqLen),
                    this,
                    false
                )
            } else {
                ETLogging.getInstance().log("Running inference.. prompt=$finalPrompt, maxSeqLen=$targetSeqLen")
                module?.generate(finalPrompt, targetSeqLen, this, false)
            }

            val generateDuration = System.currentTimeMillis() - generateStartTime
            mainHandler.post {
                resultMessage?.let { msg ->
                    msg.totalGenerationTime = generateDuration
                    val index = _messages.indexOfLast { it === msg }
                    if (index >= 0) {
                        val updated = msg.copy()
                        _messages[index] = updated
                        resultMessage = updated
                    }
                }
                isGenerating = false
                updateMemoryUsage()
            }
            ETLogging.getInstance().log("Inference completed")
            resultMessage?.let { msg ->
                for (line in msg.text.lines()) {
                    Log.i("BHARAT_PS4_OUTPUT", line)
                }
            }
            Log.i("BHARAT_PS4_TURN_DONE", "TURN_FINISHED")
            System.gc()
        }
    }

    fun stopGeneration() {
        Log.i("ChatViewModel", "stopGeneration called")
        module?.stop()
    }

    private fun prefillVoxtralAudio(audioFeaturePath: String, textPrompt: String) {
        try {
            val byteData = Files.readAllBytes(Paths.get(audioFeaturePath))
            val buffer = ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN)
            val floatCount = byteData.size / java.lang.Float.BYTES
            val floats = FloatArray(floatCount)

            for (i in 0 until floatCount) {
                floats[i] = buffer.float
            }
            val bins = 128
            val frames = 3000
            val batchSize = floatCount / (bins * frames)
            val preAudioPrompt = "<s>[INST][BEGIN_AUDIO]"
            val postAudioPrompt = "$textPrompt[/INST]"
            ETLogging.getInstance().log("Voxtral prefill pre-audio prompt: $preAudioPrompt")
            module?.prefillPrompt(preAudioPrompt)
            module?.prefillAudio(floats, batchSize, bins, frames)
            ETLogging.getInstance().log("Voxtral prefill post-audio prompt: $postAudioPrompt")
            module?.prefillPrompt(postAudioPrompt)
        } catch (e: IOException) {
            Log.e("AudioPrefill", "Audio file error")
        }
    }

    fun dismissModelLoadErrorDialog() {
        showModelLoadErrorDialog = false
    }

    fun addSystemMessage(text: String) {
        if (!isDuplicateSystemMessage(text)) {
            _messages.add(Message(text, false, MessageType.SYSTEM, 0))
        }
    }

    private fun isDuplicateSystemMessage(text: String): Boolean {
        if (_messages.isEmpty()) return false
        val lastMessage = _messages.last()
        return lastMessage.messageType == MessageType.SYSTEM && text == lastMessage.text
    }

    // LlmCallback implementation
    override fun onResult(result: String) {
        var processedResult = result

        // Filter out raw EOT strings (<|eot_id|>, <|end_of_text|>)
        if (processedResult == "<|eot_id|>" || processedResult == "<|end_of_text|>" ||
            processedResult.contains("<|eot_id|>") || processedResult.contains("<|end_of_text|>")) {
            module?.stop()
            return
        }

        if (processedResult == PromptFormat.getStopToken(currentSettingsFields.modelType)) {
            module?.stop()
            return
        }

        // Thinking mode state machine: intercept <think> / </think> tags
        if (processedResult == "<think>") {
            isInThinkingBlock = true
            return
        }
        if (processedResult == "</think>") {
            isInThinkingBlock = false
            return
        }

        processedResult = PromptFormat.replaceSpecialToken(currentSettingsFields.modelType, processedResult)

        if (currentSettingsFields.modelType == ModelType.LLAMA_3) {
            if (processedResult.contains("<|start_header_id|>") || processedResult.contains("<|end_header_id|>")) {
                processedResult = processedResult
                    .replace("<|start_header_id|>", "")
                    .replace("<|end_header_id|>", "")
                    .replace("assistant", "")
                    .trimStart('\n', ' ')
                sawStartHeaderId = false
                if (processedResult.isEmpty()) return
            } else if (sawStartHeaderId) {
                if (processedResult.contains("assistant")) {
                    return
                }
                sawStartHeaderId = false
            }
        }

        val inThinking = isInThinkingBlock
        mainHandler.post {
            if (inThinking) {
                // Skip leading newlines in thinking content
                val keepThinking = !(processedResult == "\n" || processedResult == "\n\n") ||
                        resultMessage?.thinkingContent?.isNotEmpty() == true
                if (keepThinking) {
                    resultMessage?.appendThinkingText(processedResult)
                }
            } else {
                val keepResult = !(processedResult == "\n" || processedResult == "\n\n") ||
                        resultMessage?.text?.isNotEmpty() == true
                if (keepResult) {
                    resultMessage?.appendText(processedResult)
                }
            }

            // Create a new Message reference to trigger recomposition under Compose strong
            // skipping mode, which compares unstable parameters by reference equality (===).
            val index = _messages.indexOfLast { it === resultMessage }
            if (index >= 0) {
                val updated = resultMessage!!.copy()
                _messages[index] = updated
                resultMessage = updated
            }
        }
    }

    override fun onStats(stats: String) {
        var tps = 0f
        try {
            val jsonObject = JSONObject(stats)
            val numGeneratedTokens = jsonObject.optInt("generated_tokens", 0)
            val inferenceEndMs = jsonObject.optInt("inference_end_ms", 0)
            val promptEvalEndMs = jsonObject.optInt("prompt_eval_end_ms", 0)
            val diff = inferenceEndMs - promptEvalEndMs
            if (diff > 0 && numGeneratedTokens > 0) {
                tps = numGeneratedTokens.toFloat() / diff * 1000
            }
        } catch (e: JSONException) {
            Log.e("LLM", "Error parsing JSON: ${e.message}")
        }

        mainHandler.post {
            resultMessage?.let { msg ->
                if (tps > 0f) {
                    msg.tokensPerSecond = tps
                }

                // Sentence completion guard:
                // Inspect msg.text: if generated text after the header card ends mid-sentence,
                // trim back to the last complete sentence delimiter if > 120 chars were generated.
                val headerDelimiter = "---\n"
                val fullText = msg.text
                if (fullText.contains(headerDelimiter)) {
                    val prefix = fullText.substringBefore(headerDelimiter) + headerDelimiter
                    val generatedBody = fullText.substringAfter(headerDelimiter)
                    val trimmedBody = generatedBody.trimEnd()
                    if (trimmedBody.length > 120) {
                        val validEndChars = setOf('.', '।', '!', '?', ')', '\n')
                        if (trimmedBody.lastOrNull() !in validEndChars) {
                            val lastDelimIdx = trimmedBody.indexOfLast { it == '.' || it == '।' || it == '!' || it == '\n' }
                            if (lastDelimIdx > 60) {
                                val cleanedBody = trimmedBody.substring(0, lastDelimIdx + 1)
                                msg.setText(prefix + cleanedBody)
                                Log.i("ChatViewModel", "Sentence completion guard trimmed dangling mid-sentence text: dropped ${trimmedBody.length - cleanedBody.length} chars")
                            }
                        }
                    }
                }

                updateMemoryUsage()
                // Create a new reference to trigger recomposition under strong skipping mode
                val index = _messages.indexOfLast { it === msg }
                if (index >= 0) {
                    val updated = msg.copy()
                    _messages[index] = updated
                    resultMessage = updated
                }
            }
        }
        if (tps > 0f) {
            Log.i("BHARAT_PS4_METRICS", "TPS=" + tps + " RAM=" + ramUsage + " OFFLINE=" + curriculumEngine.isDeviceOffline())
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do NOT destroy sharedNativeModule on ViewModel clearance.
        // It is a process-wide singleton so MainActivity relaunch or ViewModel recreate reuses it immediately.
    }


    private var autoWatcherStarted = false

    private fun startAutoPromptWatcher() {
        if (true) return // Disabled for interactive user mode
        if (autoWatcherStarted) return
        autoWatcherStarted = true
        Thread {
            val demoTurns = listOf(
                "I want to learn cybersecurity in Hindi",
                "Answer: A",
                "Build my 4-week Edge AI quantization roadmap in Tamil"
            )
            for ((idx, promptText) in demoTurns.withIndex()) {
                // Wait until model is ready and not generating
                while (!isModelReady || isGenerating) {
                    Thread.sleep(300)
                }
                Thread.sleep(600)
                Log.i("BHARAT_PS4_TURN_START", "STARTING_TURN_" + (idx + 1) + ": " + promptText)
                val latch = java.util.concurrent.CountDownLatch(1)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    inputText = promptText
                    sendMessage()
                    latch.countDown()
                }
                latch.await()
                Thread.sleep(500)
                while (isGenerating) {
                    Thread.sleep(300)
                }
            }
            Log.i("BHARAT_PS4_ALL_DONE", "ALL_3_TURNS_COMPLETED_SUCCESSFULLY")
        }.start()
    }

    companion object {
        private const val MAX_NUM_OF_IMAGES = 5
        @Volatile private var sharedNativeModule: LlmModule? = null
        @Volatile private var sharedLoadedModelPath: String = ""
        private val globalModelLoadLock = java.util.concurrent.atomic.AtomicBoolean(false)
    }
}
