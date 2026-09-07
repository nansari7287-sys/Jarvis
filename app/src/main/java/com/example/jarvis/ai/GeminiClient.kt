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

            val body = JSONObject()
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", d.prompt)
                            )
                        )
                    )
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", JarvisConfig.TEMPERATURE)
                        .put("maxOutputTokens", JarvisConfig.MAX_OUTPUT_TOKENS)
                )
                .toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
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

                if (text.isBlank()) {
                    ApiResult.Error("Gemini returned an empty response.")
                } else {
                    ApiResult.Success(
                        AIResponse(text, raw)
                    )
                }
            }

        } catch (e: Exception) {
            ApiResult.Error(
                "Network error: ${e.message}",
                e
            )
        }
    }
}