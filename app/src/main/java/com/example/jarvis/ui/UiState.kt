package com.example.jarvis.ui

import com.example.jarvis.models.Message

data class UiState(
    val messages: List<Message> = emptyList(),
    val isThinking: Boolean = false,
    val error: String? = null
)