package com.example.jarvis.accessibility
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
class JarvisAccessibilityService:AccessibilityService(){companion object{@Volatile var instance:JarvisAccessibilityService?=null private set};override fun onServiceConnected(){super.onServiceConnected();instance=this};override fun onAccessibilityEvent(e:AccessibilityEvent?){};override fun onInterrupt(){};override fun onDestroy(){instance=null;super.onDestroy()};fun clickText(t:String)=AccessibilityHelper.click(AccessibilityHelper.findText(rootInActiveWindow,t))}
