package com.example.jarvis.network
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
object NetworkModule{val client=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS).build()}
