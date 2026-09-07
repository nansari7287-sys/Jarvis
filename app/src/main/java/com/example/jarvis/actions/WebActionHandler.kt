package com.example.jarvis.actions
import android.content.Context
import android.content.Intent
import android.net.Uri
class WebActionHandler(private val c:Context){fun search(q:String){c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
