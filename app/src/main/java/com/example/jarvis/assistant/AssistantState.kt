package com.example.jarvis.assistant
sealed class AssistantState{data object Idle:AssistantState();data object Thinking:AssistantState();data class Speaking(val text:String):AssistantState();data class Error(val message:String):AssistantState()}
