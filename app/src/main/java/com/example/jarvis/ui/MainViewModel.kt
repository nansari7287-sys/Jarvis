package com.example.jarvis.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.ai.AIRequest
import com.example.jarvis.ai.ConversationManager
import com.example.jarvis.ai.GeminiClient
import com.example.jarvis.ai.JarvisCommand
import com.example.jarvis.ai.JarvisCommandParser
import com.example.jarvis.automation.CommandExecutor
import com.example.jarvis.models.Message
import com.example.jarvis.network.ApiResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // =========================================================
    // CORE DEPENDENCIES & ENGINES
    // =========================================================
    private val geminiClient = GeminiClient()
    private val conversationManager = ConversationManager()
    
    // ERROR FIX 1: Parser ka object banana zaroori tha
    private val commandParser = JarvisCommandParser() 

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var commandExecutor: CommandExecutor? = null
    private var responseListener: ((String) -> Unit)? = null

    // Safe Coroutine Handler to prevent app crashes on network/AI failure
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("JARVIS_VM", "Critical Error: \${exception.message}", exception)
        setThinking(false)
        respond("System overloaded. Ek error aayi hai: \${exception.localizedMessage}")
    }

    companion object {
        private const val TAG = "JarvisMainViewModel"
        private const val SYSTEM_PROMPT = """
            You are J.A.R.V.I.S., an advanced, highly intelligent, and autonomous Android AI assistant.
            You must act as a seamless, professional, and slightly witty assistant (like Tony Stark's JARVIS).

            CONVERSATION RULES:
            1. Understand Hindi, Hinglish, and English naturally.
            2. Remember context. If the user says "usko band karo", know what "usko" refers to.
            3. Keep conversational responses short, sharp, and natural.
            4. DO NOT use JSON for casual conversation.

            ANDROID AUTOMATION RULES:
            1. If the user asks you to control the device (open app, search, click, scroll, etc.), 
               you MUST return ONLY a strictly formatted JSON object. No extra text.
            2. Use ONLY the supported actions.
            3. For multi-step tasks, use the "steps" array.

            SUPPORTED ACTIONS:
            OPEN_APP, OPEN_URL, WEB_SEARCH, YOUTUBE, INSTAGRAM, WHATSAPP, BACK, HOME, RECENTS, SCROLL_UP, SCROLL_DOWN, CLICK, TYPE, WAIT, AUTOMATION, NO_ACTION

            JSON FORMAT EXPECTED:
            {
              "action": "ACTION_NAME",
              "target": "TARGET_NAME",
              "value": "OPTIONAL_VALUE",
              "requiresConfirmation": false,
              "steps": [
                 { "action": "CLICK", "target": "Search Bar" }
              ]
            }
        """
    }

    // =========================================================
    // EXECUTOR INITIALIZATION
    // =========================================================
    fun initializeExecutor(context: Context) {
        if (commandExecutor == null) {
            commandExecutor = CommandExecutor(context.applicationContext)
            Log.d(TAG, "Command Executor Initialized and Ready.")
        }
    }

    fun setResponseListener(listener: ((String) -> Unit)?) {
        responseListener = listener
    }

    // =========================================================
    // MAIN MESSAGE PROCESSING (AI BRAIN)
    // =========================================================
    fun send(text: String, apiKey: String) {
        val message = text.trim()
        if (message.isBlank()) return

        // 1. Update UI and Context
        addUserMessage(message)
        conversationManager.addUserMessage(message)
        setThinking(true)

        // 2. Check for Lightning-Fast Local Offline Commands first
        val localCommand: JarvisCommand? = detectLocalCommand(message)
        if (localCommand != null) {
            Log.d(TAG, "Local command detected: \${localCommand.action}")
            executeCommand(localCommand)
            return
        }

        // 3. Fallback to Gemini AI Cloud Processing
        if (apiKey.isBlank()) {
            setThinking(false)
            respond("System Alert: Gemini API key missing. Please configure it in settings.")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            val previousContext = conversationManager.buildContext()
            val prompt = buildPrompt(previousContext, message)

            val request = AIRequest(prompt = prompt, apiKey = apiKey)
            val result = geminiClient.generate(request)

            when (result) {
                is ApiResult.Success -> {
                    handleGeminiResponse(result.data.text ?: "")
                }
                is ApiResult.Error -> {
                    setThinking(false)
                    respond("Network anomaly detected: \${result.message}")
                }
            }
        }
    }

    // =========================================================
    // ADVANCED PROMPT BUILDER
    // =========================================================
    private fun buildPrompt(previousContext: String, currentMessage: String): String {
        return buildString {
            append(SYSTEM_PROMPT.trimIndent())
            append("\n\n=== CONVERSATION HISTORY ===\n")
            if (previousContext.isNotBlank()) {
                append(previousContext)
                append("\n")
            } else {
                append("[No recent history]\n")
            }
            append("\n=== CURRENT COMMAND ===\n")
            append("User: \$currentMessage\nJARVIS:")
        }
    }

    // =========================================================
    // AI RESPONSE PARSER & SANITIZER
    // =========================================================
    private fun handleGeminiResponse(response: String) {
        val rawText = response.trim()
        if (rawText.isBlank()) {
            setThinking(false)
            respond("I couldn't process that. Please try again.")
            return
        }

        // Clean up markdown syntax if Gemini wraps JSON in ```json ... ```
        val sanitizedText = cleanJsonResponse(rawText)
        val looksLikeJson = sanitizedText.startsWith("{") && sanitizedText.endsWith("}")

        if (looksLikeJson) {
            try {
                // ERROR FIX 2: Strict typing and safe parsing
                val command: JarvisCommand? = commandParser.parse(sanitizedText)
                if (command != null && command.action != "CONVERSATION") {
                    executeCommand(command)
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "JSON Parsing Failed: \${e.message}")
                // Fallback to normal conversation if JSON parsing completely fails
            }
        }

        // If not a system command, treat as normal conversational reply
        setThinking(false)
        respond(rawText)
    }

    // Utility to strip markdown code blocks from AI response
    private fun cleanJsonResponse(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json", ignoreCase = true)) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }

    // =========================================================
    // COMMAND EXECUTION ENGINE
    // =========================================================
    private fun executeCommand(command: JarvisCommand) {
        val executor = commandExecutor
        if (executor == null) {
            setThinking(false)
            respond("Automation engine offline. Accessibility service might be disabled.")
            return
        }

        if (command.requiresConfirmation) {
            setThinking(false)
            respond("Boss, ye action perform karne ke liye mujhe aapki confirmation chahiye.")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            val executed = executor.execute(command)
            setThinking(false)

            if (executed) {
                val successMessage = generateDynamicSuccessResponse(command)
                respond(successMessage)
            } else {
                respond("Action complete nahi ho saka. Shayad screen par UI elements match nahi kiye.")
            }
        }
    }

    // =========================================================
    // DYNAMIC RESPONSE GENERATOR
    // =========================================================
    private fun generateDynamicSuccessResponse(command: JarvisCommand): String {
        return when (command.action.uppercase()) {
            "OPEN_APP" -> {
                val appName = command.target?.replaceFirstChar { it.uppercase() } ?: "Application"
                "Right away, sir. Opening \$appName."
            }
            "OPEN_URL" -> "Accessing the requested secure link now."
            "WEB_SEARCH" -> "Running a web search for '\${command.value ?: "your query"}'. Here are the results."
            "YOUTUBE" -> if (!command.value.isNullOrBlank()) "Searching YouTube for '\${command.value}'." else "Launching YouTube interface."
            "INSTAGRAM" -> "Connecting to Instagram servers."
            "WHATSAPP" -> "Opening encrypted WhatsApp channel."
            "BACK" -> "Going back."
            "HOME" -> "Returning to main interface."
            "RECENTS" -> "Displaying active tasks."
            "SCROLL_UP", "SCROLL_DOWN" -> "Scrolling..."
            "CLICK" -> "Interacting with \${command.target ?: "the element"}."
            "TYPE" -> "Data entry complete."
            "AUTOMATION" -> "Complex automation protocol finished successfully."
            else -> "Task executed, boss."
        }
    }

    // =========================================================
    // HIGH-SPEED LOCAL COMMAND DETECTION (NO API CALL REQUIRED)
    // =========================================================
    private fun detectLocalCommand(text: String): JarvisCommand? {
        val command = text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "") // Remove all punctuations dynamically
            .trim()

        val openKeywords = listOf("open", "khol", "kholo", "launch", "chala", "start")
        val appList = listOf("instagram", "youtube", "whatsapp", "chrome", "settings", "camera", "gallery", "spotify", "maps")

        // Dynamic mapping check for fast offline execution
        for (app in appList) {
            for (keyword in openKeywords) {
                if (command.contains(keyword) && command.contains(app)) {
                    return JarvisCommand(action = "OPEN_APP", target = app)
                }
            }
        }

        // Other basic system commands
        if (command == "go back" || command == "peeche jao") {
            return JarvisCommand(action = "BACK")
        }
        if (command == "go home" || command == "home screen") {
            return JarvisCommand(action = "HOME")
        }

        return null // If no local match, return null to let Gemini handle it
    }

    // =========================================================
    // UI & STATE UPDATES
    // =========================================================
    private fun respond(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        addAssistantMessage(cleanText)
        conversationManager.addAssistantMessage(cleanText)
        
        // Trigger voice and UI via activity listener
        responseListener?.invoke(cleanText)
    }

    private fun addUserMessage(text: String) {
        val messages = _ui.value.messages.toMutableList()
        messages.add(Message(text = text, isUser = true))
        _ui.value = _ui.value.copy(messages = messages, error = null)
    }

    private fun addAssistantMessage(text: String) {
        val messages = _ui.value.messages.toMutableList()
        messages.add(Message(text = text, isUser = false))
        _ui.value = _ui.value.copy(messages = messages)
    }

    private fun setThinking(thinking: Boolean) {
        _ui.value = _ui.value.copy(isThinking = thinking)
    }

    fun clearConversation() {
        conversationManager.clear()
        _ui.value = _ui.value.copy(messages = emptyList(), error = null, isThinking = false)
        Log.d(TAG, "Conversation memory wiped.")
    }

    override fun onCleared() {
        responseListener = null
        conversationManager.clear()
        super.onCleared()
    }
}
