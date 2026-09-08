package com.example.jarvis.ai

class ConversationManager {

    private val conversation = mutableListOf<Pair<String, String>>()

    companion object {
        private const val MAX_MESSAGES = 20
    }

    fun addUserMessage(message: String) {
        addMessage("user", message)
    }

    fun addAssistantMessage(message: String) {
        addMessage("assistant", message)
    }

    private fun addMessage(role: String, message: String) {
        val cleanMessage = message.trim()

        if (cleanMessage.isBlank()) {
            return
        }

        conversation.add(role to cleanMessage)

        // Keep only the latest messages so the prompt does not grow endlessly.
        while (conversation.size > MAX_MESSAGES) {
            conversation.removeAt(0)
        }
    }

    fun clear() {
        conversation.clear()
    }

    fun getHistory(): List<Pair<String, String>> {
        return conversation.toList()
    }

    fun buildContext(): String {
        if (conversation.isEmpty()) {
            return ""
        }

        return buildString {
            append("Previous conversation:\n")

            conversation.forEach { (role, message) ->
                when (role) {
                    "user" -> append("User: ")
                    "assistant" -> append("JARVIS: ")
                }

                append(message)
                append("\n")
            }
        }
    }

    fun isEmpty(): Boolean {
        return conversation.isEmpty()
    }

    fun size(): Int {
        return conversation.size
    }
}