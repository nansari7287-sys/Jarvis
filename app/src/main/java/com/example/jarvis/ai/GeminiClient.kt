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
class GeminiClient(private val client:OkHttpClient=OkHttpClient()){suspend fun generate(d:AIRequest):ApiResult<AIResponse>=withContext(Dispatchers.IO){if(d.apiKey.isBlank())return@withContext ApiResult.Error("Gemini API key is not configured.");try{val url="https://generativelanguage.googleapis.com/v1beta/models/${JarvisConfig.DEFAULT_MODEL}:generateContent?key=${d.apiKey}";val body=JSONObject().put("contents",JSONArray().put(JSONObject().put("parts",JSONArray().put(JSONObject().put("text",d.prompt))))).put("generationConfig",JSONObject().put("temperature",JarvisConfig.TEMPERATURE).put("maxOutputTokens",JarvisConfig.MAX_OUTPUT_TOKENS)).toString().toRequestBody("application/json".toMediaType());client.newCall(Request.Builder().url(url).post(body).build()).execute().use{r->val raw=r.body?.string().orEmpty();if(!r.isSuccessful)return@withContext ApiResult.Error("Gemini request failed (${r.code}).");val t=JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty();if(t.isBlank())ApiResult.Error("Gemini returned an empty response.")else ApiResult.Success(AIResponse(t,raw))}}catch(e:Exception){ApiResult.Error("Network error: ${e.message}",e)}}}
