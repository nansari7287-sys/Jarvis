package com.example.jarvis.ai

class ConversationManager {

    private val conversation = mutableListOf<Pair<String, String>>()

    fun addUserMessage(message: String) {
        conversation.add("user" to message)
    }

    fun addAssistantMessage(message: String) {
        conversation.add("assistant" to message)
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
        }
    }
}
