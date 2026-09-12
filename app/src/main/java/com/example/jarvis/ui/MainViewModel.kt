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

    private val geminiClient = GeminiClient()

    private val conversationManager =
        ConversationManager()

    // ERROR FIX: Parser ko as an object instantiate kiya hai
    // Taaki "Unresolved reference 'parse'" ka error na aaye
    private val commandParser = 
        JarvisCommandParser()

    private val _ui =
        MutableStateFlow(UiState())

    val ui: StateFlow<UiState> =
        _ui.asStateFlow()

    private var commandExecutor: CommandExecutor? = null

    private var responseListener: ((String) -> Unit)? = null

    // Safe Coroutine Handler to prevent app crashes on network/AI failure
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("JARVIS_VM", "Critical Error: \${exception.message}", exception)
        setThinking(false)
        respond("System overloaded. Ek error aayi hai: \${exception.localizedMessage}")
    }

    // =========================================================
    // EXECUTOR
    // =========================================================

    fun initializeExecutor(
        context: Context
    ) {

        if (commandExecutor == null) {

            commandExecutor =
                CommandExecutor(
                    context.applicationContext
                )
        }
    }

    // =========================================================
    // RESPONSE LISTENER
    // =========================================================

    fun setResponseListener(
        listener: ((String) -> Unit)?
    ) {

        responseListener = listener
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    fun send(
        text: String,
        apiKey: String
    ) {

        val message =
            text.trim()

        if (message.isBlank()) {
            return
        }

        addUserMessage(message)

        conversationManager
            .addUserMessage(message)

        setThinking(true)

        // -----------------------------------------------------
        // Local commands
        // -----------------------------------------------------

        val localCommand =
            detectLocalCommand(message)

        if (localCommand != null) {

            executeCommand(
                localCommand
            )

            return
        }

        // -----------------------------------------------------
        // Gemini
        // -----------------------------------------------------

        viewModelScope.launch(exceptionHandler) {

            if (apiKey.isBlank()) {

                respond(
                    "Gemini API key set nahi hai. Settings me API key add karo."
                )

                setThinking(false)

                return@launch
            }

            try {

                val previousContext =
                    conversationManager
                        .buildContext()

                val prompt =
                    buildPrompt(
                        previousContext,
                        message
                    )

                val result =
                    geminiClient.generate(
                        AIRequest(
                            prompt = prompt,
                            apiKey = apiKey
                        )
                    )

                when (result) {

                    is ApiResult.Success -> {

                        handleGeminiResponse(
                            result.data.text ?: ""
                        )
                    }

                    is ApiResult.Error -> {

                        respond(
                            result.message
                        )

                        setThinking(false)
                    }
                }

            } catch (e: Exception) {

                respond(
                    "JARVIS error: ${
                        e.message ?: "Unknown error"
                    }"
                )

                setThinking(false)
            }
        }
    }

    // =========================================================
    // BUILD GEMINI PROMPT
    // =========================================================

    private fun buildPrompt(
        previousContext: String,
        currentMessage: String
    ): String {

        return buildString {

            append(
                """
                You are JARVIS, the user's personal Android AI assistant.

                Conversation behavior:
                - Understand Hindi, Hinglish and English naturally.
                - Remember the recent conversation context.
                - Understand follow-up references like "haan", "woh", "usko",
                  "pehle wala", "continue karo", etc.
                - For normal questions and casual conversation, reply naturally.
                - Do not return JSON for normal conversation.

                Android control behavior:
                - If the user asks you to control the Android device,
                  return one valid JSON command.
                - Use only supported actions.
                - For multiple actions use AUTOMATION with steps.
                - Important external actions must require confirmation.

                Supported actions:
                OPEN_APP
                OPEN_URL
                WEB_SEARCH
                YOUTUBE
                INSTAGRAM
                WHATSAPP
                BACK
                HOME
                RECENTS
                SCROLL_UP
                SCROLL_DOWN
                CLICK
                TYPE
                WAIT
                AUTOMATION
                NO_ACTION

                JSON format:
                {
                  "action": "ACTION_NAME",
                  "target": "TARGET",
                  "value": "VALUE",
                  "steps": [],
                  "requiresConfirmation": false
                }

                Never invent unsupported actions.

                """.trimIndent()
            )

            append("\n\n")

            if (previousContext.isNotBlank()) {

                append(previousContext)
                append("\n")
            }

            append(
                "CURRENT USER REQUEST:\n"
            )

            append(currentMessage)
        }
    }

    // =========================================================
    // GEMINI RESPONSE & SANITIZER
    // =========================================================

    private fun handleGeminiResponse(
        response: String
    ) {

        val rawText =
            response.trim()

        if (rawText.isBlank()) {

            respond(
                "Mujhe koi response nahi mila."
            )

            setThinking(false)

            return
        }
        
        // Clean markdown syntax securely
        var cleanText = rawText
        if (cleanText.startsWith("```json", ignoreCase = true)) {
            cleanText = cleanText.substring(7)
        } else if (cleanText.startsWith("```")) {
            cleanText = cleanText.substring(3)
        }
        if (cleanText.endsWith("```")) {
            cleanText = cleanText.substring(0, cleanText.length - 3)
        }
        cleanText = cleanText.trim()

        val looksLikeJson =
            cleanText.startsWith("{") && cleanText.endsWith("}")

        if (looksLikeJson) {
            
            try {

                val command =
                    commandParser.parse(
                        cleanText
                    )

                if (command != null && command.action != "CONVERSATION") {

                    executeCommand(
                        command
                    )

                    return
                }
                
            } catch (e: Exception) {
                Log.e("JARVIS_VM", "JSON Parse error: \${e.message}")
            }
        }

        // Normal conversation fallback
        respond(rawText)

        setThinking(false)
    }

    // =========================================================
    // EXECUTE COMMAND
    // =========================================================

    private fun executeCommand(
        command: JarvisCommand
    ) {

        val executor =
            commandExecutor

        if (executor == null) {

            respond(
                "JARVIS executor ready nahi hai."
            )

            setThinking(false)

            return
        }

        // -----------------------------------------------------
        // Confirmation
        // -----------------------------------------------------

        if (command.requiresConfirmation) {

            respond(
                "Ye action karne se pehle tumhari confirmation chahiye."
            )

            setThinking(false)

            return
        }

        viewModelScope.launch(exceptionHandler) {

            try {

                val executed =
                    executor.execute(
                        command
                    )

                if (executed) {

                    respond(
                        commandResponse(
                            command
                        )
                    )

                } else {

                    respond(
                        "Ye command execute nahi ho saki."
                    )
                }

            } catch (e: Exception) {

                respond(
                    "Command error: ${
                        e.message ?: "Unknown error"
                    }"
                )
            }

            setThinking(false)
        }
    }

    // =========================================================
    // COMMAND RESPONSE
    // =========================================================

    private fun commandResponse(
        command: JarvisCommand
    ): String {

        // Return type strictly defined to avoid Any vs String mismatch
        return when (
            command.action.uppercase()
        ) {

            "OPEN_APP" -> {

                val app =
                    command.target
                        ?.replaceFirstChar {
                            it.uppercase()
                        }
                        ?: "app"

                "$app open kar diya."
            }

            "OPEN_URL" -> {
                "Website open kar di."
            }

            "WEB_SEARCH" -> {
                
                if (
                    !command.value.isNullOrBlank()
                ) {
                    
                    "Web par search kar diya."
                    
                } else {
                    
                    "Search kar diya."
                }
            }

            "YOUTUBE" -> {

                if (
                    !command.value.isNullOrBlank()
                ) {

                    "YouTube par search kar diya."

                } else {

                    "YouTube open kar diya."
                }
            }

            "INSTAGRAM" -> {
                "Instagram open kar diya."
            }

            "WHATSAPP" -> {
                "WhatsApp open kar diya."
            }

            "BACK" -> {
                "Back kar diya."
            }

            "HOME" -> {
                "Home screen par aa gaya."
            }

            "RECENTS",
            "RECENT_APPS" -> {
                "Recent apps open kar diye."
            }

            "SCROLL_UP" -> {
                "Upar scroll kar diya."
            }

            "SCROLL_DOWN" -> {
                "Neeche scroll kar diya."
            }

            "CLICK" -> {
                "Click kar diya."
            }

            "TYPE" -> {
                "Text enter kar diya."
            }

            "WAIT" -> {
                "Theek hai."
            }

            "AUTOMATION" -> {
                "Task complete kar diya."
            }

            "NO_ACTION" -> {
                "Theek hai."
            }

            else -> {
                "Done."
            }
        }
    }

    // =========================================================
    // LOCAL COMMAND DETECTION
    // =========================================================

    private fun detectLocalCommand(
        text: String
    ): JarvisCommand? {

        val command =
            text
                .lowercase()
                .replace(",", " ")
                .replace(".", " ")
                .replace("!", " ")
                .replace("?", " ")
                .trim()

        return when {

            isOpenCommand(
                command,
                "instagram"
            ) -> {

                JarvisCommand(
                    action = "OPEN_APP",
                    target = "instagram"
                )
            }

            isOpenCommand(
                command,
                "youtube"
            ) -> {

                JarvisCommand(
                    action = "OPEN_APP",
                    target = "youtube"
                )
            }

            isOpenCommand(
                command,
                "whatsapp"
            ) -> {

                JarvisCommand(
                    action = "OPEN_APP",
                    target = "whatsapp"
                )
            }

            isOpenCommand(
                command,
                "chrome"
            ) -> {

                JarvisCommand(
                    action = "OPEN_APP",
                    target = "chrome"
                )
            }

            isOpenCommand(
                command,
                "settings"
            ) -> {

                JarvisCommand(
                    action = "OPEN_APP",
                    target = "settings"
                )
            }

            else -> null
        }
    }

    // =========================================================
    // OPEN COMMAND CHECK
    // =========================================================

    private fun isOpenCommand(
        command: String,
        appName: String
    ): Boolean {

        if (!command.contains(appName)) {
            return false
        }

        return command.contains("open") ||
            command.contains("khol") ||
            command.contains("kholo") ||
            command.contains("launch") ||
            command.contains("chala")
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private fun respond(
        text: String
    ) {

        val cleanText =
            text.trim()

        if (cleanText.isBlank()) {
            return
        }

        addAssistantMessage(
            cleanText
        )

        conversationManager
            .addAssistantMessage(
                cleanText
            )

        responseListener?.invoke(
            cleanText
        )
    }

    // =========================================================
    // USER MESSAGE UI
    // =========================================================

    private fun addUserMessage(
        text: String
    ) {

        val messages =
            _ui.value.messages.toMutableList()

        messages.add(
            Message(
                text = text,
                isUser = true
            )
        )

        _ui.value =
            _ui.value.copy(
                messages = messages,
                error = null
            )
    }

    // =========================================================
    // ASSISTANT MESSAGE UI
    // =========================================================

    private fun addAssistantMessage(
        text: String
    ) {

        val messages =
            _ui.value.messages.toMutableList()

        messages.add(
            Message(
                text = text,
                isUser = false
            )
        )

        _ui.value =
            _ui.value.copy(
                messages = messages
            )
    }

    // =========================================================
    // THINKING STATE
    // =========================================================

    private fun setThinking(
        thinking: Boolean
    ) {

        _ui.value =
            _ui.value.copy(
                isThinking = thinking
            )
    }

    // =========================================================
    // CLEAR CONVERSATION
    // =========================================================

    fun clearConversation() {

        conversationManager.clear()

        _ui.value =
            _ui.value.copy(
                messages = emptyList(),
                error = null,
                isThinking = false
            )
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onCleared() {

        responseListener = null

        conversationManager.clear()

        super.onCleared()
    }
}
