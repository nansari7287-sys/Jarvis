package com.example.jarvis.network
import okhttp3.Request
class ApiClient{fun buildGet(url:String)=Request.Builder().url(url).get().build()}
