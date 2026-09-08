package com.example.jarvis.ai

class ConversationManager {

    companion object {
        private const val MAX_MESSAGES = 20
    }

    private val conversation = mutableListOf<Pair<String, String>>()

    @Synchronized
    fun addUserMessage(message: String) {
        addMessage("user", message)
    }

    @Synchronized
    fun addAssistantMessage(message: String) {
        addMessage("assistant", message)
    }

    @Synchronized
    private fun addMessage(role: String, message: String) {
        val cleanMessage = message.trim()

        if (cleanMessage.isBlank()) {
            return
        }

        conversation.add(role to cleanMessage)

        // Sirf recent conversation rakho.
        // Isse prompt bahut bada nahi hoga.
        while (conversation.size > MAX_MESSAGES) {
            conversation.removeAt(0)
        }
    }

    @Synchronized
    fun clear() {
        conversation.clear()
    }

    @Synchronized
    fun getHistory(): List<Pair<String, String>> {
        return conversation.toList()
    }

    @Synchronized
    fun buildContext(): String {
        if (conversation.isEmpty()) {
            return ""
        }

        return buildString {

            append("CONVERSATION HISTORY:\n")

            conversation.forEach { (role, message) ->

                when (role) {
                    "user" -> {
                        append("User: ")
                    }

                    "assistant" -> {
                        append("JARVIS: ")
                    }
                }

                append(message)
                append("\n")
            }

            append("\nContinue the conversation naturally using this context.")
        }
    }
}