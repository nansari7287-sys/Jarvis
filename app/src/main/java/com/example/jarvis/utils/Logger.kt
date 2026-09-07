package com.example.jarvis.utils
import android.util.Log
object Logger { private const val TAG="Jarvis"; fun d(m:String)=Log.d(TAG,m); fun e(m:String,t:Throwable?=null)=Log.e(TAG,m,t) }
