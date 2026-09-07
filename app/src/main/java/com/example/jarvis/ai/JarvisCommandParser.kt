
package com.example.jarvis.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini ke response ko JarvisCommand me convert karta hai.
 *
 * Gemini kabhi-kabhi JSON ko ```json ... ``` ke andar bhej sakta hai,
 * isliye parser pehle code fences remove karta hai.
 */
object JarvisCommandParser {

    fun parse(response: String): JarvisCommand? {
        return try {
            val jsonText = cleanJson(response)
            val json = JSONObject(jsonText)

            val action = json.optString("action", "").trim()

            if (action.isBlank()) {
                return null
            }

            JarvisCommand(
                action = action.uppercase(),
                target = json.optNullableString("target"),
                value = json.optNullableString("value"),
                steps = parseSteps(json.optJSONArray("steps")),
                requiresConfirmation =
                    json.optBoolean("requiresConfirmation", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSteps(array: JSONArray?): List<JarvisStep> {
        if (array == null) return emptyList()

        val result = mutableListOf<JarvisStep>()

        for (i in 0 until array.length()) {
            try {
                val item = array.optJSONObject(i) ?: continue

                val action = item.optString("action", "").trim()

                if (action.isBlank()) continue

                result.add(
                    JarvisStep(
                        action = action.uppercase(),
                        target = item.optNullableString("target"),
                        value = item.optNullableString("value"),
                        requiresConfirmation =
                            item.optBoolean("requiresConfirmation", false)
                    )
                )
            } catch (_: Exception) {
                // Invalid step ko ignore karo
            }
        }

        return result
    }

    /**
     * Gemini ke response se actual JSON nikalta hai.
     *
     * Supported:
     * 1. Normal JSON
     * 2. ```json ... ```
     * 3. ``` ... ```
     */
    private fun cleanJson(response: String): String {
        var text = response.trim()

        // Markdown code fence remove
        text = text
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // Agar response me extra text hai to first { se last } tak lo
        val firstObject = text.indexOf('{')
        val lastObject = text.lastIndexOf('}')

        if (firstObject >= 0 && lastObject > firstObject) {
            text = text.substring(firstObject, lastObject + 1)
        }

        return text.trim()
    }

    private fun JSONObject.optNullableString(
        key: String
    ): String? {
        if (!has(key) || isNull(key)) return null

        val value = optString(key, "").trim()

        return if (value.isBlank()) null else value
    }
}