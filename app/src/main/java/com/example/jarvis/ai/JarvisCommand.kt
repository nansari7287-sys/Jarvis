package com.example.jarvis.ai

/**
 * Gemini ke dwara samjhi gayi command.
 *
 * Gemini natural language ko structured command me convert karega.
 *
 * Example:
 * "Hey Jarvis, Instagram open karo"
 *
 * =>
 * action = "OPEN_APP"
 * target = "instagram"
 */
data class JarvisCommand(
    val action: String,
    val target: String? = null,
    val value: String? = null,
    val steps: List<JarvisStep> = emptyList(),
    val requiresConfirmation: Boolean = false
)

/**
 * Multi-step automation ka ek step.
 *
 * Example:
 * Instagram kholo
 * -> Search karo
 * -> First result kholo
 * -> Scroll karo
 */
data class JarvisStep(
    val action: String,
    val target: String? = null,
    val value: String? = null,
    val requiresConfirmation: Boolean = false
)
