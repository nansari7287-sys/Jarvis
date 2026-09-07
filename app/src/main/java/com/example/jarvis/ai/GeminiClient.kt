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
                You are JARVIS, a fast Android personal assistant.

                Understand the user's natural language command.

                If the user is asking JARVIS to perform an Android action,
                return ONLY valid JSON. Do not use markdown or ```.

                JSON format:

                {
                  "action": "ACTION_NAME",
                  "target": "TARGET",
                  "value": "VALUE",
                  "steps": [],
                  "requiresConfirmation": false
                }

                For multi-step commands use:

                {
                  "action": "AUTOMATION",
                  "target": "TARGET",
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

                Supported actions include:
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

                Do not invent unsupported actions.

                For actions that can send, publish, post, delete,
                make purchases, or otherwise cause an important external
                effect, set requiresConfirmation to true.

                If the user is asking a normal question rather than asking
                JARVIS to control the device, respond normally in plain text.
            """.trimIndent()

            val prompt = systemInstruction +
                "\n\nUSER REQUEST:\n" +
                d.prompt

            val body = JSONObject()
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    prompt
                                )
                            )
                        )
                    )
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put(
                            "temperature",
                            JarvisConfig.TEMPERATURE
                        )
                        .put(
                            "maxOutputTokens",
                            JarvisConfig.MAX_OUTPUT_TOKENS
                        )
                        .put(
                            "responseMimeType",
                            "application/json"
                        )
                )
                .toString()
                .toRequestBody(
                    "application/json".toMediaType()
                )

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader(
                    "Content-Type",
                    "application/json"
                )
                .build()

            client.newCall(request).execute().use { response ->

                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error(
                        "Gemini request failed (${response.code})."
                    )
                }

                val text = JSONObject(raw)
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

                ApiResult.Success(
                    AIResponse(
                        text = text,
                        raw = raw
                    )
                )
            }

        } catch (e: Exception) {

            ApiResult.Error(
                "Network error: ${e.message}",
                e
            )
        }
    }
}