package com.example.jarvis.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.ai.AIRequest
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

    private val _ui = MutableStateFlow(
        UiState()
    )

    val ui: StateFlow<UiState> =
        _ui.asStateFlow()

    private var commandExecutor: CommandExecutor? = null

    /**
     * MainActivity se ek baar call karo.
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
     * User ka command process karta hai.
     */
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

        setThinking(true)

        /*
         * Simple commands ko Gemini ke paas bhejne ki
         * zarurat nahi hai.
         */
        val localCommand =
            detectLocalCommand(message)

        if (localCommand != null) {

            executeCommand(
                localCommand
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

                setThinking(false)

                return@launch
            }

            try {

                val result =
                    geminiClient.generate(
                        AIRequest(
                            prompt = message,
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

                            executeCommand(
                                command
                            )

                        } else {

                            addAssistantMessage(
                                result.data.text
                            )

                            setThinking(false)
                        }
                    }

                    is ApiResult.Error -> {

                        addAssistantMessage(
                            result.message
                        )

                        setThinking(false)
                    }
                }

            } catch (e: Exception) {

                addAssistantMessage(
                    "JARVIS error: ${
                        e.message ?: "Unknown error"
                    }"
                )

                setThinking(false)
            }
        }
    }

    /**
     * Command ko CommandExecutor tak bhejta hai.
     */
    private fun executeCommand(
        command: JarvisCommand
    ) {

        val executor =
            commandExecutor

        if (executor == null) {

            addAssistantMessage(
                "JARVIS executor ready nahi hai."
            )

            setThinking(false)

            return
        }

        viewModelScope.launch {

            try {

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

            } catch (e: Exception) {

                addAssistantMessage(
                    "Command error: ${
                        e.message ?: "Unknown error"
                    }"
                )
            }

            setThinking(false)
        }
    }

    /**
     * Common commands ke liye local fast detection.
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
     * "Instagram open karo",
     * "Instagram kholo",
     * "open Instagram"
     * jaise commands detect karta hai.
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
     * JARVIS response UI me add karta hai.
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
}