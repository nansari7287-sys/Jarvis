package com.example.jarvis.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini response ko JarvisCommand me convert karta hai.
 *
 * Supported response formats:
 * 1. Normal JSON
 * 2. ```json ... ```
 * 3. ``` ... ```
 * 4. JSON ke around extra text
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
        "SCROLL_UP",
        "SCROLL_DOWN",
        "CLICK",
        "TYPE",
        "WAIT",
        "AUTOMATION",
        "NO_ACTION"
    )

    fun parse(response: String): JarvisCommand? {
        if (response.isBlank()) {
            return null
        }

        return try {
            val jsonText = cleanJson(response)

            if (jsonText.isBlank()) {
                return null
            }

            val json = JSONObject(jsonText)

            val action = json
                .optString("action", "")
                .trim()
                .uppercase()

            if (action.isBlank()) {
                return null
            }

            // Unknown action ko execute mat karo.
            if (action !in supportedActions) {
                return null
            }

            JarvisCommand(
                action = action,
                target = json.optNullableString("target"),
                value = json.optNullableString("value"),
                steps = parseSteps(json.optJSONArray("steps")),
                requiresConfirmation = json.optBoolean(
                    "requiresConfirmation",
                    false
                )
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSteps(
        array: JSONArray?
    ): List<JarvisStep> {

        if (array == null) {
            return emptyList()
        }

        val result = mutableListOf<JarvisStep>()

        for (i in 0 until array.length()) {

            try {
                val item = array.optJSONObject(i)
                    ?: continue

                val action = item
                    .optString("action", "")
                    .trim()
                    .uppercase()

                if (action.isBlank()) {
                    continue
                }

                // Automation ke andar bhi unknown action allow nahi karna.
                if (action !in supportedActions) {
                    continue
                }

                result.add(
                    JarvisStep(
                        action = action,
                        target = item.optNullableString("target"),
                        value = item.optNullableString("value"),
                        requiresConfirmation = item.optBoolean(
                            "requiresConfirmation",
                            false
                        )
                    )
                )

            } catch (_: Exception) {
                // Invalid step ko safely ignore karo.
            }
        }

        return result
    }

    /**
     * Gemini response se actual JSON object nikalta hai.
     */
    private fun cleanJson(response: String): String {

        var text = response.trim()

        // Markdown code fences remove.
        text = text
            .replaceFirst(
                Regex("^```json\\s*", RegexOption.IGNORE_CASE),
                ""
            )
            .replaceFirst(
                Regex("^```\\s*"),
                ""
            )
            .replaceFirst(
                Regex("\\s*```$"),
                ""
            )
            .trim()

        // Agar response me JSON ke bahar extra text hai,
        // to first { se last } tak JSON extract karo.
        val firstObject = text.indexOf('{')
        val lastObject = text.lastIndexOf('}')

        if (
            firstObject >= 0 &&
            lastObject > firstObject
        ) {
            text = text.substring(
                firstObject,
                lastObject + 1
            )
        }

        return text.trim()
    }

    /**
     * JSON string ko nullable String me convert karta hai.
     */
    private fun JSONObject.optNullableString(
        key: String
    ): String? {

        if (!has(key) || isNull(key)) {
            return null
        }

        val value = optString(
            key,
            ""
        ).trim()

        return if (value.isBlank()) {
            null
        } else {
            value
        }
    }
}