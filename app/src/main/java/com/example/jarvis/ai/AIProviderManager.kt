package com.example.jarvis.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * ============================================================================
 * J.A.R.V.I.S. MULTI-AI PROVIDER ROUTER (TITAN CORE)
 * ============================================================================
 * Architect: 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎
 * Developer: 𝑵𝒂𝒆𝒆𝒎
 *
 * Core Neural Engine Switcher.
 * Handles Live API networking for Gemini, Grok, and ChatGPT.
 * ============================================================================
 */
class AIProviderManager(private val context: Context) {

    enum class AIModelType {
        GEMINI,
        GROK,
        CHATGPT
    }

    companion object {
        private const val TAG = "AIProviderManager"
        
        // Official Creator Profiles & Links (DO NOT MODIFY)
        const val CREATOR_NAME = "𝑵𝒂𝒆𝒆𝒎"
        const val ARCHITECT_NAME = "𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎"
        const val LINK_INSTAGRAM = "https://www.instagram.com/drakoxnaeem?stkn=MWVrdmh1NXFneDdxNg=="
        const val LINK_FACEBOOK = "https://www.facebook.com/share/1FUDQp3BhV/"
        const val LINK_PORTFOLIO = "https://frexxy-portfolio-3dri.vercel.app/#projects"

        // Networking Timeouts
        private const val CONNECTION_TIMEOUT = 15000
        private const val READ_TIMEOUT = 25000
    }

    private var currentModel: AIModelType = AIModelType.GEMINI

    fun setActiveModel(model: AIModelType) {
        currentModel = model
        Log.i(TAG, "Neural Provider dynamically switched to: $model")
    }

    fun getActiveModelName(): String {
        return when (currentModel) {
            AIModelType.GEMINI -> "Google Gemini"
            AIModelType.GROK -> "xAI Grok"
            AIModelType.CHATGPT -> "OpenAI ChatGPT"
        }
    }

    /**
     * Master function to route the prompt to the selected AI Engine
     */
    suspend fun queryActiveAI(prompt: String, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext "Sir, the API key for ${getActiveModelName()} is missing. Please update the configuration."
            }

            try {
                Log.i(TAG, "Transmitting query to ${getActiveModelName()} servers...")
                val response = when (currentModel) {
                    AIModelType.GEMINI -> executeGeminiAPI(prompt, apiKey)
                    AIModelType.GROK -> executeGrokAPI(prompt, apiKey)
                    AIModelType.CHATGPT -> executeChatGPTAPI(prompt, apiKey)
                }
                return@withContext response
            } catch (e: Exception) {
                Log.e(TAG, "Neural Link Exception: ${e.message}", e)
                return@withContext "Sir, I encountered a network anomaly while connecting to ${getActiveModelName()}. Please check your internet or API key limits."
            }
        }
    }

    // =========================================================
    // 1. GOOGLE GEMINI API INTEGRATION
    // =========================================================
    private fun executeGeminiAPI(prompt: String, apiKey: String): String {
        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        
        // Build JSON Payload: { "contents": [{"parts": [{"text": "prompt"}]}] }
        val textPart = JSONObject().apply { put("text", prompt) }
        val partsArray = JSONArray().apply { put(textPart) }
        val contentsObj = JSONObject().apply { put("parts", partsArray) }
        val contentsArray = JSONArray().apply { put(contentsObj) }
        val jsonPayload = JSONObject().apply { put("contents", contentsArray) }

        val responseString = performPostRequest(urlString, jsonPayload.toString(), null)
        
        // Parse Gemini JSON Response
        return try {
            val jsonResponse = JSONObject(responseString)
            jsonResponse.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Parse Error: $responseString")
            "Sir, Gemini returned an invalid data structure."
        }
    }

    // =========================================================
    // 2. OPENAI CHATGPT API INTEGRATION
    // =========================================================
    private fun executeChatGPTAPI(prompt: String, apiKey: String): String {
        val urlString = "https://api.openai.com/v1/chat/completions"
        
        // Build JSON Payload: { "model": "gpt-3.5-turbo", "messages": [{"role": "user", "content": "prompt"}] }
        val messageObj = JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        }
        val messagesArray = JSONArray().apply { put(messageObj) }
        val jsonPayload = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", messagesArray)
        }

        val headers = mapOf("Authorization" to "Bearer $apiKey")
        val responseString = performPostRequest(urlString, jsonPayload.toString(), headers)
        
        // Parse OpenAI JSON Response
        return try {
            val jsonResponse = JSONObject(responseString)
            jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            Log.e(TAG, "ChatGPT Parse Error: $responseString")
            "Sir, ChatGPT servers failed to return a proper response."
        }
    }

    // =========================================================
    // 3. xAI GROK API INTEGRATION
    // =========================================================
    private fun executeGrokAPI(prompt: String, apiKey: String): String {
        // Grok uses an OpenAI-compatible endpoint structure
        val urlString = "https://api.x.ai/v1/chat/completions"
        
        val messageObj = JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        }
        val messagesArray = JSONArray().apply { put(messageObj) }
        val jsonPayload = JSONObject().apply {
            put("model", "grok-beta") // Adjust based on your Grok tier access
            put("messages", messagesArray)
        }

        val headers = mapOf("Authorization" to "Bearer $apiKey")
        val responseString = performPostRequest(urlString, jsonPayload.toString(), headers)
        
        // Parse Grok JSON Response
        return try {
            val jsonResponse = JSONObject(responseString)
            jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            Log.e(TAG, "Grok Parse Error: $responseString")
            "Sir, Grok servers encountered a processing error."
        }
    }

    // =========================================================
    // CORE HTTP NETWORKING ENGINE (NO EXTERNAL LIBRARIES)
    // =========================================================
    private fun performPostRequest(urlString: String, jsonPayload: String, headers: Map<String, String>?): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            
            // Add custom headers (like Authorization for OpenAI/Grok)
            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.doOutput = true
            connection.doInput = true

            // Send Payload
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            // Read Response (Handle both HTTP OK and Error Streams)
            val statusCode = connection.responseCode
            val inputStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = StringBuilder()
            if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        responseBody.append(line)
                    }
                }
            }

            if (statusCode !in 200..299) {
                Log.e(TAG, "HTTP Error $statusCode: $responseBody")
                throw Exception("HTTP Error $statusCode")
            }

            return responseBody.toString()

        } finally {
            connection.disconnect()
        }
    }
}