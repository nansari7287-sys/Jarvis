package com.example.jarvis.actions
import android.content.Context
import android.content.Intent
class AppActionHandler(private val c:Context){fun openPackage(p:String):Boolean{val i=c.packageManager.getLaunchIntentForPackage(p)?:return false;i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(i);return true}}
