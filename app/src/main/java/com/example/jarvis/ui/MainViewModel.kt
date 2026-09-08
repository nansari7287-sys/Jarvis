package com.example.jarvis.ui

import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val geminiClient = GeminiClient()

    private val conversationManager =
        ConversationManager(maxMessages = 20)

    private val _ui =
        MutableStateFlow(UiState())

    val ui: StateFlow<UiState> =
        _ui.asStateFlow()

    private var commandExecutor: CommandExecutor? = null

    /**
     * MainActivity / voice mode ke liye
     * assistant response listener.
     */
    private var responseListener: ((String) -> Unit)? = null

    /**
     * Command executor ko ek baar initialize karta hai.
     */
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

    /**
     * Voice mode / TTS ke liye listener.
     */
    fun setResponseListener(
        listener: ((String) -> Unit)?
    ) {
        responseListener = listener
    }

    /**
     * User ka message process karta hai.
     */
    fun send(
        text: String,
        apiKey: String
    ) {
        val message = text.trim()

        if (message.isBlank()) {
            return
        }

        addUserMessage(message)

        conversationManager.addUserMessage(message)

        setThinking(true)

        /*
         * Common commands ko directly execute karo.
         * Isse unnecessary Gemini request nahi jayegi.
         */
        val localCommand =
            detectLocalCommand(message)

        if (localCommand != null) {
            executeCommand(localCommand)
            return
        }

        /*
         * Baaki request Gemini ko bhejo.
         */
        viewModelScope.launch {

            if (apiKey.isBlank()) {

                respond(
                    "Gemini API key set nahi hai. Settings me API key add karo."
                )

                setThinking(false)

                return@launch
            }

            try {

                /*
                 * Previous conversation ko current
                 * request ke saath Gemini ko bhejo.
                 */
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
                            result.data.text
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

    /**
     * Gemini ke liye conversation context
     * + current user request banata hai.
     */
    private fun buildPrompt(
        previousContext: String,
        currentMessage: String
    ): String {

        return buildString {

            if (previousContext.isNotBlank()) {

                append(previousContext)
                append("\n")
            }

            append("Current user request:\n")
            append(currentMessage)
        }
    }

    /**
     * Gemini response ko command ya normal
     * conversation ke roop me handle karta hai.
     */
    private fun handleGeminiResponse(
        response: String
    ) {

        val text =
            response.trim()

        if (text.isBlank()) {

            respond(
                "Mujhe koi response nahi mila."
            )

            setThinking(false)

            return
        }

        /*
         * JSON response command ho sakta hai.
         */
        val looksLikeJson =
            text.startsWith("{") &&
                    text.endsWith("}")

        if (looksLikeJson) {

            val command =
                JarvisCommandParser.parse(text)

            if (command != null) {

                executeCommand(command)

                return
            }
        }

        /*
         * Normal conversation.
         */
        respond(text)

        setThinking(false)
    }

    /**
     * Command execute karta hai.
     */
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

        /*
         * Important action ke liye confirmation
         * future UI/voice layer handle kar sakti hai.
         *
         * Abhi command executor ko direct pass
         * nahi kar rahe agar confirmation required hai.
         */
        if (command.requiresConfirmation) {

            respond(
                "Ye action karne se pehle tumhari confirmation chahiye."
            )

            setThinking(false)

            return
        }

        viewModelScope.launch {

            try {

                val executed =
                    executor.execute(command)

                if (executed) {

                    val response =
                        commandResponse(command)

                    respond(response)

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

    /**
     * Command complete hone ke baad natural response.
     */
    private fun commandResponse(
        command: JarvisCommand
    ): String {

        return when (command.action.uppercase()) {

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
                "Search kar diya."
            }

            "YOUTUBE" -> {

                if (!command.value.isNullOrBlank()) {

                    "YouTube par ${command.value} search kar diya."

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

            "RECENTS" -> {
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

    /**
     * Common apps ke liye local command detection.
     */
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

    /**
     * Open app commands detect karta hai.
     */
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

    /**
     * Assistant ka response:
     *
     * 1. UI me add
     * 2. Conversation memory me save
     * 3. Voice/TTS listener ko send
     */
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

    /**
     * User message UI me add karta hai.
     */
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

    /**
     * Assistant message UI me add karta hai.
     */
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

    private fun setThinking(
        thinking: Boolean
    ) {

        _ui.value =
            _ui.value.copy(
                isThinking = thinking
            )
    }

    /**
     * Conversation memory clear karta hai.
     */
    fun clearConversation() {

        conversationManager.clear()

        _ui.value =
            _ui.value.copy(
                messages = emptyList(),
                error = null
            )
    }

    override fun onCleared() {

        responseListener = null

        conversationManager.clear()

        super.onCleared()
    }
}