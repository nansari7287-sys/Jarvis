package com.example.jarvis.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini response ko JarvisCommand me convert karta hai.
 *
 * Supported:
 * - Normal JSON
 * - ```json ... ```
 * - ``` ... ```
 * - JSON ke around extra text
 * - Multi-step AUTOMATION
 */
object JarvisCommandParser {

    private val supportedActions = setOf(
        "OPEN_APP",
        "OPEN_URL",
        "WEB_SEARCH",
        "YOUTUBE",
        "INSTAGRAM",
        "WHATSAPP",
        "BACK",
        "HOME",
        "RECENTS",
        "RECENT_APPS",
        "SCROLL_UP",
        "SCROLL_DOWN",
        "CLICK",
        "TYPE",
        "WAIT",
        "AUTOMATION",
        "NO_ACTION"
    )

    fun parse(
        response: String
    ): JarvisCommand? {

        if (response.isBlank()) {
            return null
        }

        return try {

            val jsonText =
                cleanJson(response)

            if (jsonText.isBlank()) {
                return null
            }

            val json =
                JSONObject(jsonText)

            val action =
                json.optString(
                    "action",
                    ""
                )
                    .trim()
                    .uppercase()

            if (action.isBlank()) {
                return null
            }

            if (action !in supportedActions) {
                return null
            }

            val steps =
                parseSteps(
                    json.optJSONArray("steps")
                )

            // AUTOMATION ko steps ke bina execute
            // nahi karna chahiye.
            if (
                action == "AUTOMATION" &&
                steps.isEmpty()
            ) {
                return null
            }

            JarvisCommand(
                action = normalizeAction(action),
                target = json.optNullableString(
                    "target"
                ),
                value = json.optNullableString(
                    "value"
                ),
                steps = steps,
                requiresConfirmation =
                    json.optBoolean(
                        "requiresConfirmation",
                        false
                    )
            )

        } catch (_: Exception) {

            null
        }
    }

    // =========================================================
    // PARSE AUTOMATION STEPS
    // =========================================================

    private fun parseSteps(
        array: JSONArray?
    ): List<JarvisStep> {

        if (array == null) {
            return emptyList()
        }

        val result =
            mutableListOf<JarvisStep>()

        for (i in 0 until array.length()) {

            try {

                val item =
                    array.optJSONObject(i)
                        ?: continue

                val rawAction =
                    item.optString(
                        "action",
                        ""
                    )
                        .trim()
                        .uppercase()

                if (rawAction.isBlank()) {
                    continue
                }

                if (
                    rawAction !in supportedActions
                ) {
                    continue
                }

                // Nested AUTOMATION avoid karo.
                if (
                    rawAction == "AUTOMATION"
                ) {
                    continue
                }

                val action =
                    normalizeAction(
                        rawAction
                    )

                result.add(
                    JarvisStep(
                        action = action,
                        target =
                            item.optNullableString(
                                "target"
                            ),
                        value =
                            item.optNullableString(
                                "value"
                            ),
                        requiresConfirmation =
                            item.optBoolean(
                                "requiresConfirmation",
                                false
                            )
                    )
                )

            } catch (_: Exception) {
                // Invalid step safely ignore.
            }
        }

        return result
    }

    // =========================================================
    // NORMALIZE ACTION
    // =========================================================

    private fun normalizeAction(
        action: String
    ): String {

        return when (
            action.trim().uppercase()
        ) {

            "RECENT_APPS" ->
                "RECENTS"

            else ->
                action.trim().uppercase()
        }
    }

    // =========================================================
    // CLEAN JSON
    // =========================================================

    private fun cleanJson(
        response: String
    ): String {

        var text =
            response.trim()

        // -----------------------------------------------------
        // Markdown JSON fence
        // -----------------------------------------------------

        text = text.replaceFirst(
            Regex(
                "^```json\\s*",
                RegexOption.IGNORE_CASE
            ),
            ""
        )

        text = text.replaceFirst(
            Regex(
                "^```\\s*"
            ),
            ""
        )

        text = text.replaceFirst(
            Regex(
                "\\s*```$"
            ),
            ""
        )

        text =
            text.trim()

        // -----------------------------------------------------
        // Direct JSON
        // -----------------------------------------------------

        if (
            text.startsWith("{") &&
            text.endsWith("}")
        ) {
            return text
        }

        // -----------------------------------------------------
        // JSON surrounded by text
        // -----------------------------------------------------

        val firstObject =
            text.indexOf('{')

        val lastObject =
            text.lastIndexOf('}')

        if (
            firstObject >= 0 &&
            lastObject > firstObject
        ) {

            text =
                text.substring(
                    firstObject,
                    lastObject + 1
                )
        }

        return text.trim()
    }

    // =========================================================
    // NULLABLE JSON STRING
    // =========================================================

    private fun JSONObject.optNullableString(
        key: String
    ): String? {

        if (
            !has(key) ||
            isNull(key)
        ) {
            return null
        }

        val value =
            optString(
                key,
                ""
            )
                .trim()

        return if (
            value.isBlank()
        ) {
            null
        } else {
            value
        }
    }
}