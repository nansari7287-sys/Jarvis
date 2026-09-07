package com.example.jarvis.accessibility
import android.view.accessibility.AccessibilityNodeInfo
object AccessibilityHelper{fun findText(r:AccessibilityNodeInfo?,t:String)=r?.findAccessibilityNodeInfosByText(t)?.firstOrNull();fun click(n:AccessibilityNodeInfo?)=n?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true}
