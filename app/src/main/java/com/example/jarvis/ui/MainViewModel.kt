package com.example.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.ai.AIRequest
import com.example.jarvis.ai.GeminiClient
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

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var commandExecutor: CommandExecutor? = null

    fun initializeExecutor(context: android.content.Context) {
        if (commandExecutor == null) {
            commandExecutor = CommandExecutor(
                context.applicationContext
            )
        }
    }

    fun send(
        text: String,
        apiKey: String
    ) {

        val message = text.trim()

        if (message.isBlank()) {
            return
        }

        val currentMessages =
            _ui.value.messages.toMutableList()

        currentMessages.add(
            Message(
                text = message,
                isUser = true
            )
        )

        _ui.value = _ui.value.copy(
            messages = currentMessages,
            isThinking = true,
            error = null
        )

        /*
         * Simple commands ko Gemini ke paas bhejne ki zarurat nahi.
         * Isse common commands fast execute honge.
         */
        val localCommand = detectLocalCommand(message)

        if (localCommand != null) {

            val executor = commandExecutor

            if (executor != null) {

                val executed = executor.execute(
                    localCommand
                )

                val reply =
                    if (executed) {
                        "Done."
                    } else {
                        "Command execute nahi ho saka."
                    }

                addAssistantMessage(reply)

            } else {

                addAssistantMessage(
                    "JARVIS executor ready nahi hai."
                )
            }

            _ui.value = _ui.value.copy(
                isThinking = false
            )

            return
        }

        /*
         * Complex commands Gemini ko jayengi.
         */
        viewModelScope.launch {

            if (apiKey.isBlank()) {

                addAssistantMessage(
                    "Gemini API key set nahi hai. Settings me API key add karo."
                )

                _ui.value = _ui.value.copy(
                    isThinking = false
                )

                return@launch
            }

            val result = geminiClient.generate(
                AIRequest(
                    prompt = buildJarvisPrompt(message),
                    apiKey = apiKey
                )
            )

            when (result) {

                is ApiResult.Success -> {

                    val command =
                        JarvisCommandParser.parse(
                            result.data.text
                        )

                    if (command != null) {

                        val executor =
                            commandExecutor

                        if (executor != null) {

                            val executed =
                                executor.execute(command)

                            if (executed) {
                                addAssistantMessage(
                                    "Done."
                                )
                            } else {
                                addAssistantMessage(
                                    "Command execute nahi ho saka."
                                )
                            }

                        } else {

                            addAssistantMessage(
                                "JARVIS executor ready nahi hai."
                            )
                        }

                    } else {

                        /*
                         * Agar Gemini normal conversational
                         * response deta hai to use chat me dikhao.
                         */
                        addAssistantMessage(
                            result.data.text
                        )
                    }
                }

                is ApiResult.Error -> {

                    addAssistantMessage(
                        result.message
                    )
                }
            }

            _ui.value = _ui.value.copy(
                isThinking = false
            )
        }
    }

    /**
     * Common commands ke liye fast local detection.
     */
    private fun detectLocalCommand(
        text: String
    ): com.example.jarvis.ai.JarvisCommand? {

        val command =
            text
                .lowercase()
                .replace(",", " ")
                .replace(".", " ")
                .replace("!", " ")
                .trim()

        return when {

            command.contains("instagram") &&
                (
                    command.contains("open") ||
                    command.contains("khol") ||
                    command.contains("kholo")
                ) -> {

                com.example.jarvis.ai.JarvisCommand(
                    action = "OPEN_APP",
                    target = "instagram"
                )
            }

            command.contains("youtube") &&
                (
                    command.contains("open") ||
                    command.contains("khol") ||
                    command.contains("kholo")
                ) -> {

                com.example.jarvis.ai.JarvisCommand(
                    action = "OPEN_APP",
                    target = "youtube"
                )
            }

            command.contains("whatsapp") &&
                (
                    command.contains("open") ||
                    command.contains("khol") ||
                    command.contains("kholo")
                ) -> {

                com.example.jarvis.ai.JarvisCommand(
                    action = "OPEN_APP",
                    target = "whatsapp"
                )
            }

            command.contains("chrome") &&
                (
                    command.contains("open") ||
                    command.contains("khol") ||
                    command.contains("kholo")
                ) -> {

                com.example.jarvis.ai.JarvisCommand(
                    action = "OPEN_APP",
                    target = "chrome"
                )
            }

            command.contains("settings") &&
                (
                    command.contains("open") ||
                    command.contains("khol") ||
                    command.contains("kholo")
                ) -> {

                com.example.jarvis.ai.JarvisCommand(
                    action = "OPEN_APP",
                    target = "settings"
                )
            }

            else -> null
        }
    }

    /**
     * Gemini ko strict structured response ke liye prompt.
     */
    private fun buildJarvisPrompt(
        userMessage: String
    ): String {

        return """
            You are JARVIS, a personal Android AI assistant.

            Understand the user's natural language command.

            Return ONLY valid JSON.
            Do not use markdown.
            Do not use ```.

            JSON format:

            {
              "action": "ACTION_NAME",
              "target": "optional_target",
              "value": "optional_value",
              "steps": [],
              "requiresConfirmation": false
            }

            For multiple actions use:

            {
              "action": "AUTOMATION",
              "target": "optional_target",
              "value": null,
              "steps": [
                {
                  "action": "ACTION_NAME",
                  "target": "optional_target",
                  "value": "optional_value",
                  "requiresConfirmation": false
                }
              ],
              "requiresConfirmation": false
            }

            Allowed actions:

            OPEN_APP
            OPEN_URL
            WEB_SEARCH
            YOUTUBE
            INSTAGRAM
            WHATSAPP
            BACK
            HOME
            RECENT_APPS
            SCROLL_UP
            SCROLL_DOWN
            CLICK
            TYPE
            WAIT
            NO_ACTION

            Sensitive or external actions must set:
            "requiresConfirmation": true

            User command:
            $userMessage
        """.trimIndent()
    }

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

        _ui.value = _ui.value.copy(
            messages = messages
        )
    }
}