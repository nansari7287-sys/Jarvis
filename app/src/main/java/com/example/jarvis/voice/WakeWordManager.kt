package com.example.jarvis.voice

class WakeWordManager {

    private var enabled = false

    companion object {
        private const val WAKE_PHRASE = "hey jarvis"
        private const val JARVIS_PHRASE = "jarvis"
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    fun containsWakeWord(text: String): Boolean {
        if (!enabled) return false

        val normalized = normalize(text)

        return normalized.contains(WAKE_PHRASE) ||
                normalized == JARVIS_PHRASE ||
                normalized.startsWith("$JARVIS_PHRASE ")
    }

    fun removeWakeWord(text: String): String {
        val normalized = normalize(text)

        return when {
            normalized.startsWith("$WAKE_PHRASE ") ->
                normalized.removePrefix(WAKE_PHRASE).trim()

            normalized == WAKE_PHRASE ->
                ""

            normalized.startsWith("$JARVIS_PHRASE ") ->
                normalized.removePrefix(JARVIS_PHRASE).trim()

            normalized == JARVIS_PHRASE ->
                ""

            else ->
                text.trim()
        }
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[,!?;:]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}