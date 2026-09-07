package com.example.jarvis.assistant
import com.example.jarvis.ai.*
import com.example.jarvis.network.ApiResult
class JarvisAssistant(private val client:GeminiClient=GeminiClient()){suspend fun respond(p:String,k:String)=when(val r=client.generate(AIRequest(p,k))){is ApiResult.Success->AssistantResponse(r.data.text);is ApiResult.Error->AssistantResponse("Sorry, ${r.message}",false)}}
