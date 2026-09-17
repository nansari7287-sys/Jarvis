package com.example.jarvis

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiAgentEngine(private val apiKey: String) {

    data class AgentDecision(
        val action: String,       // "INSTAGRAM_SEARCH", "WHATSAPP_SEND", "OPEN_APP", "CHAT", "SYSTEM_CONTROL"
        val target: String = "",   // Username, Contact Name, or App Name
        val payload: String = "",  // Message Text or Search Query
        val speech: String         // Hindi voice feedback
    )

    private val systemPrompt = """
        You are J.A.R.V.I.S., an autonomous Android Operating System Agent.
        Analyze user commands and output strictly raw JSON with NO markdown formatting, no backticks, no extra text.
        
        Valid JSON Schema:
        {
          "action": "INSTAGRAM_SEARCH" | "WHATSAPP_SEND" | "OPEN_APP" | "SYSTEM_CONTROL" | "CHAT",
          "target": "name or query or app_name or empty",
          "payload": "message text or parameter or empty",
          "speech": "short hindi confirmation response for the user"
        }

        Examples:
        - "Instagram kholo aur Rahul search karo" -> {"action":"INSTAGRAM_SEARCH","target":"Rahul","payload":"","speech":"इंस्टाग्राम पर राहुल को सर्च किया जा रहा है।"}
        - "WhatsApp pe Faizan ko bolo kal meeting hai" -> {"action":"WHATSAPP_SEND","target":"Faizan","payload":"कल मीटिंग है","speech":"फैजान को व्हाट्सएप संदेश भेजा जा रहा है।"}
        - "Torch on karo" -> {"action":"SYSTEM_CONTROL","target":"TORCH_ON","payload":"","speech":"टॉर्च चालू कर दी गई है।"}
        - "Tum kaun ho" -> {"action":"CHAT","target":"","payload":"","speech":"मैं जार्विस हूँ, आपका पर्सनल एआई असिस्टेंट।"}
    """.trimIndent()

    suspend fun executeUserIntent(userInput: String): AgentDecision = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AgentDecision(
                action = "CHAT",
                speech = "सर, जेमिनी एपीआई की (API Key) सेटिंग्स में मौजूद नहीं है।"
            )
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val requestBody = JSONObject().apply {
                val contents = JSONObject().apply {
                    val parts = JSONObject().apply {
                        put("text", "$systemPrompt\n\nUser: $userInput")
                    }
                    put("parts", org.json.JSONArray().put(parts))
                }
                put("contents", org.json.JSONArray().put(contents))
            }

            OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val rawResponse = reader.readText()
                reader.close()

                val jsonResponse = JSONObject(rawResponse)
                val candidates = jsonResponse.optJSONArray("candidates")
                val contentObj = candidates?.getJSONObject(0)?.getJSONObject("content")
                val rawText = contentObj?.getJSONArray("parts")?.getJSONObject(0)?.getString("text") ?: "{}"

                // Clean markdown code fence if present
                val cleanedJson = rawText.replace("```json", "").replace("```", "").trim()
                val parsed = JSONObject(cleanedJson)

                return@withContext AgentDecision(
                    action = parsed.optString("action", "CHAT"),
                    target = parsed.optString("target", ""),
                    payload = parsed.optString("payload", ""),
                    speech = parsed.optString("speech", "कमांड प्रोसेस हो गई है।")
                )
            } else {
                Log.e("GEMINI_CORE", "Server returned HTTP $responseCode")
                return@withContext AgentDecision("CHAT", speech = "सर्वर से कनेक्ट करने में त्रुटि आई।")
            }
        } catch (e: Exception) {
            Log.e("GEMINI_CORE", "API Failed", e)
            return@withContext AgentDecision("CHAT", speech = "सिस्टम में कुछ तकनीकी गड़बड़ हुई है सर।")
        }
    }
}
