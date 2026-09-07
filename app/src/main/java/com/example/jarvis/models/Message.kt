package com.example.jarvis.models
data class Message(val id: Long=System.currentTimeMillis(), val text:String, val isUser:Boolean, val timestamp:Long=System.currentTimeMillis())
