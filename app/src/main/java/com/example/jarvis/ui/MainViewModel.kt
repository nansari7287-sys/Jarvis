package com.example.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.ai.AIRequest
import com.example.jarvis.ai.GeminiClient
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

    fun send(text: String, apiKey: String) {
        val message = text.trim()

        if (message.isBlank()) {
            return
        }

        val currentMessages = _ui.value.messages.toMutableList()

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

        viewModelScope.launch {

            val result = geminiClient.generate(
                AIRequest(
                    prompt = message,
                    apiKey = apiKey
                )
            )

            val updatedMessages = _ui.value.messages.toMutableList()

            when (result) {

                is ApiResult.Success -> {
                    updatedMessages.add(
                        Message(
                            text = result.data.text,
                            isUser = false
                        )
                    )
                }

                is ApiResult.Error -> {
                    updatedMessages.add(
                        Message(
                            text = result.message,
                            isUser = false
                        )
                    )
                }
            }

            _ui.value = _ui.value.copy(
                messages = updatedMessages,
                isThinking = false
            )
        }
    }
}