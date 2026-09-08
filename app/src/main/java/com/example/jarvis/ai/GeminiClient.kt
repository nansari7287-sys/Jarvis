package com.example.jarvis.ai

import com.example.jarvis.models.JarvisConfig
import com.example.jarvis.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiClient(
    private val client: OkHttpClient = OkHttpClient()
) {

    suspend fun generate(
        d: AIRequest
    ): ApiResult<AIResponse> = withContext(Dispatchers.IO) {

        if (d.apiKey.isBlank()) {
            return@withContext ApiResult.Error(
                "Gemini API key is not configured."
            )
        }

        try {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "${JarvisConfig.DEFAULT_MODEL}:generateContent?key=${d.apiKey}"

            val systemInstruction = """
                You are JARVIS, a fast, intelligent Android personal assistant.

                You communicate naturally with the user.

                IMPORTANT:
                The application can execute Android actions using structured
                commands.

                If the user's request requires controlling the Android device,
                return ONLY one valid JSON object using this format:

                {
                  "action": "ACTION_NAME",
                  "target": "TARGET",
                  "value": "VALUE",
                  "steps": [],
                  "requiresConfirmation": false
                }

                Supported actions:

                OPEN_APP
                OPEN_URL
                WEB_SEARCH
                YOUTUBE
                INSTAGRAM
                WHATSAPP
                BACK
                HOME
                RECENTS
                SCROLL_UP
                SCROLL_DOWN
                CLICK
                TYPE
                WAIT
                NO_ACTION

                For multiple actions, use:

                {
                  "action": "AUTOMATION",
                  "target": null,
                  "value": null,
                  "steps": [
                    {
                      "action": "ACTION_NAME",
                      "target": "TARGET",
                      "value": "VALUE",
                      "requiresConfirmation": false
                    }
                  ],
                  "requiresConfirmation": false
                }

                Examples:

                User: Instagram kholo

                {
                  "action": "OPEN_APP",
                  "target": "instagram",
                  "value": null,
                  "steps": [],
                  "requiresConfirmation": false
                }

                User: YouTube par cars search karo

                {
                  "action": "YOUTUBE",
                  "target": "youtube",
                  "value": "cars",
                  "steps": [],
                  "requiresConfirmation": false
                }

                User: Instagram kholo aur search me cars likho

                {
                  "action": "AUTOMATION",
                  "target": "instagram",
                  "value": null,
                  "steps": [
                    {
                      "action": "OPEN_APP",
                      "target": "instagram",
                      "value": null,
                      "requiresConfirmation": false
                    },
                    {
                      "action": "CLICK",
                      "target": "search",
                      "value": null,
                      "requiresConfirmation": false
                    },
                    {
                      "action": "TYPE",
                      "target": "search",
                      "value": "cars",
                      "requiresConfirmation": false
                    }
                  ],
                  "requiresConfirmation": false
                }

                IMPORTANT SAFETY RULE:

                Actions that send messages, publish/post content, delete
                content, make purchases, change important account settings,
                or otherwise create an important external effect must set:

                "requiresConfirmation": true

                Do not invent unsupported actions.

                If the user is simply asking a question, having a conversation,
                asking for an explanation, greeting you, or talking casually,
                DO NOT return JSON.

                For normal conversation, return a natural plain-text answer.

                Be concise, helpful and conversational.

                The user may speak Hindi, Hinglish or English.
                Understand all three naturally.
            """.trimIndent()

            val prompt =
                systemInstruction +
                    "\n\nUSER REQUEST:\n" +
                    d.prompt

            val contents =
                JSONArray().put(
                    JSONObject()
                        .put(
                            "role",
                            "user"
                        )
                        .put(
                            "parts",
                            JSONArray().put(
                                JSONObject()
                                    .put(
                                        "text",
                                        prompt
                                    )
                            )
                        )
                )

            val generationConfig =
                JSONObject()
                    .put(
                        "temperature",
                        JarvisConfig.TEMPERATURE
                    )
                    .put(
                        "maxOutputTokens",
                        JarvisConfig.MAX_OUTPUT_TOKENS
                    )

            val body =
                JSONObject()
                    .put(
                        "contents",
                        contents
                    )
                    .put(
                        "generationConfig",
                        generationConfig
                    )
                    .toString()
                    .toRequestBody(
                        "application/json".toMediaType()
                    )

            val request =
                Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .build()

            client.newCall(request).execute().use { response ->

                val raw =
                    response.body
                        ?.string()
                        .orEmpty()

                if (!response.isSuccessful) {

                    val errorMessage =
                        extractGeminiError(raw)

                    return@withContext ApiResult.Error(
                        "Gemini request failed (${response.code}): $errorMessage"
                    )
                }

                val text =
                    JSONObject(raw)
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()
                        .trim()

                if (text.isBlank()) {

                    return@withContext ApiResult.Error(
                        "Gemini returned an empty response."
                    )
                }

                return@withContext ApiResult.Success(
                    AIResponse(
                        text = text,
                        raw = raw
                    )
                )
            }

        } catch (e: Exception) {

            return@withContext ApiResult.Error(
                "Network error: ${
                    e.message ?: "Unknown network error"
                }",
                e
            )
        }
    }

    private fun extractGeminiError(
        raw: String
    ): String {

        if (raw.isBlank()) {
            return "No error details returned by Gemini."
        }

        return try {

            val json =
                JSONObject(raw)

            val error =
                json.optJSONObject("error")

            val message =
                error
                    ?.optString("message")
                    ?.trim()
                    .orEmpty()

            if (message.isNotBlank()) {
                message
            } else {
                raw.take(500)
            }

        } catch (_: Exception) {

            raw.take(500)
        }
    }
}