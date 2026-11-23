package com.example.jarvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * OpenAI Responses API transport for the Sign in with ChatGPT open-source flow.
 *
 * The bearer token must come from a completed OAuth grant that includes
 * chatgpt.tokens.use.direct. Never pass an API key or a browser cookie here.
 *
 * SIWC preview requirements: store=false, stream=true, and the caller must
 * consume the stream through response.completed before treating it as success.
 */
class OpenAiPlanResponsesClient(
    private val http: OkHttpClient = OkHttpClient()
) {
    suspend fun respond(
        accessToken: String,
        model: String,
        instructions: String,
        history: List<Pair<String, String>>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        require(accessToken.isNotBlank()) { "ChatGPT plan connection is not signed in." }
        require(model.isNotBlank()) { "Select a model available to this ChatGPT account." }

        val input = JSONArray()
        history.forEach { (user, assistant) ->
            input.put(message("user", user))
            input.put(message("assistant", assistant))
        }
        input.put(message("user", userMessage))

        val payload = JSONObject().apply {
            put("model", model)
            put("instructions", instructions)
            put("input", input)
            put("store", false)
            put("stream", true)
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body?.string().orEmpty().take(1200)
                throw IOException("OpenAI Responses request failed (${response.code}): $detail")
            }

            val source = response.body?.source()
                ?: throw IOException("OpenAI returned an empty response stream.")
            val output = StringBuilder()
            var completed = false

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty() || data == "[DONE]") continue

                val event = try {
                    JSONObject(data)
                } catch (_: Exception) {
                    continue
                }

                when (event.optString("type")) {
                    "response.output_text.delta" -> output.append(event.optString("delta"))
                    "response.failed" -> {
                        val error = event.optJSONObject("response")
                            ?.optJSONObject("error")
                        throw IOException(
                            "OpenAI response failed: ${error?.optString("code") ?: "unknown_error"}"
                        )
                    }
                    "response.incomplete" -> throw IOException("OpenAI response was incomplete.")
                    "response.completed" -> completed = true
                }
            }

            if (!completed) throw IOException("OpenAI stream ended before response.completed.")
            output.toString().trim().ifEmpty {
                throw IOException("OpenAI completed without output text.")
            }
        }
    }


    suspend fun listModels(accessToken: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/models")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Could not load ChatGPT models (${response.code}): ${body.take(500)}")
            val models = JSONObject(body).optJSONArray("models")
                ?: throw IOException("ChatGPT model catalog did not contain a models list.")
            buildList {
                for (index in 0 until models.length()) {
                    val item = models.optJSONObject(index) ?: continue
                    val slug = item.optString("slug")
                    if (slug.isNotBlank() && item.optString("visibility") == "list") {
                        add(slug to item.optString("display_name", slug))
                    }
                }
            }
        }
    }

    private fun message(role: String, text: String) = JSONObject().apply {
        put("role", role)
        put("content", text)
    }
}
