package com.example.jarvis.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events can be handled here when needed.
    }

    override fun onInterrupt() {
        // Required override.
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun clickText(text: String): Boolean {
        val node = AccessibilityHelper.findText(
            rootInActiveWindow,
            text
        )

        return AccessibilityHelper.click(node)
    }
}