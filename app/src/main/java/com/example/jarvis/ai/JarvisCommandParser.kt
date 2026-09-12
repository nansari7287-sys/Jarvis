package com.example.jarvis.ai

import org.json.JSONObject

class JarvisCommandParser {

    fun parse(rawResponse: String?): JarvisCommand {
        if (rawResponse.isNullOrEmpty()) {
            return JarvisCommand(action = "CONVERSATION", target = null)
        }

        try {
            // Agar AI ne JSON format me diya hai toh usko parse karenge
            val json = JSONObject(rawResponse.trim())
            val action = json.optString("action", "CONVERSATION")
            val target = json.optString("target", null)
            val value = json.optString("value", null)
            val requiresConfirmation = json.optBoolean("requiresConfirmation", false)

            val stepsArray = json.optJSONArray("steps")
            val stepsList = mutableListOf<JarvisStep>()
            
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(i)
                    stepsList.add(
                        JarvisStep(
                            action = stepObj.optString("action", "CLICK"),
                            target = stepObj.optString("target", null),
                            value = stepObj.optString("value", null),
                            requiresConfirmation = stepObj.optBoolean("requiresConfirmation", false)
                        )
                    )
                }
            }

            return JarvisCommand(
                action = action,
                target = target,
                value = value,
                steps = stepsList,
                requiresConfirmation = requiresConfirmation
            )
        } catch (e: Exception) {
            // Fallback agar plain text me command ho
            val upperText = rawResponse.uppercase()
            return when {
                upperText.contains("OPEN") -> JarvisCommand(action = "OPEN_APP", target = rawResponse)
                upperText.contains("SEARCH") -> JarvisCommand(action = "SEARCH", value = rawResponse)
                else -> JarvisCommand(action = "CONVERSATION", target = rawResponse)
            }
        }
    }
}