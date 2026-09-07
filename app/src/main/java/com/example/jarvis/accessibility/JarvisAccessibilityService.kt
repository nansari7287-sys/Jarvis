package com.example.jarvis.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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

    /**
     * JARVIS ke screen-level actions yahan handle honge.
     */
    fun performJarvisAction(
        action: String,
        target: String? = null,
        value: String? = null
    ): Boolean {

        return when (action.uppercase()) {

            "BACK" -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            "HOME" -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            "RECENTS" -> {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }

            "SCROLL_UP" -> {
                scrollWindow(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            }

            "SCROLL_DOWN" -> {
                scrollWindow(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }

            "CLICK" -> {
                clickTarget(target)
            }

            "TYPE" -> {
                typeText(target, value)
            }

            else -> {
                false
            }
        }
    }

    /**
     * Kisi visible text ko click karta hai.
     */
    fun clickText(text: String): Boolean {

        if (text.isBlank()) return false

        val node = AccessibilityHelper.findText(
            rootInActiveWindow,
            text
        )

        return AccessibilityHelper.click(node)
    }

    /**
     * Target text/content-description ko find karke click karta hai.
     */
    private fun clickTarget(target: String?): Boolean {

        if (target.isNullOrBlank()) return false

        val root = rootInActiveWindow ?: return false

        // Pehle exact visible text try karo.
        val textNodes =
            root.findAccessibilityNodeInfosByText(target)

        for (node in textNodes) {
            if (node.isClickable && node.isVisibleToUser) {
                return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            var parent = node.parent

            while (parent != null) {
                if (parent.isClickable && parent.isVisibleToUser) {
                    return parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                }

                parent = parent.parent
            }
        }

        // Content description bhi try karo.
        return clickByContentDescription(root, target)
    }

    private fun clickByContentDescription(
        root: AccessibilityNodeInfo,
        target: String
    ): Boolean {

        val nodes = root.findAccessibilityNodeInfosByText(target)

        for (node in nodes) {
            val description =
                node.contentDescription?.toString()

            if (
                description != null &&
                description.equals(target, ignoreCase = true) &&
                node.isVisibleToUser
            ) {
                return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }
        }

        return false
    }

    /**
     * Currently focused text field me text enter karta hai.
     *
     * Agar target diya gaya hai to pehle target field
     * find karne ki koshish karta hai.
     */
    private fun typeText(
        target: String?,
        value: String?
    ): Boolean {

        if (value.isNullOrBlank()) return false

        val root = rootInActiveWindow ?: return false

        val node = if (!target.isNullOrBlank()) {
            findEditableNode(root, target)
                ?: findFocusedEditableNode(root)
        } else {
            findFocusedEditableNode(root)
        }

        ?: return false

        val arguments = Bundle()

        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            value
        )

        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            arguments
        )
    }

    /**
     * Focused editable field find karta hai.
     */
    private fun findFocusedEditableNode(
        root: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (
            root.isEditable &&
            root.isFocused &&
            root.isVisibleToUser
        ) {
            return root
        }

        for (i in 0 until root.childCount) {

            val child = root.getChild(i) ?: continue

            val result = findFocusedEditableNode(child)

            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Target ke naam/text se editable field find karta hai.
     */
    private fun findEditableNode(
        root: AccessibilityNodeInfo,
        target: String
    ): AccessibilityNodeInfo? {

        if (
            root.isEditable &&
            root.isVisibleToUser
        ) {
            val text =
                root.text?.toString()

            val description =
                root.contentDescription?.toString()

            if (
                text?.contains(
                    target,
                    ignoreCase = true
                ) == true ||
                description?.contains(
                    target,
                    ignoreCase = true
                ) == true
            ) {
                return root
            }
        }

        for (i in 0 until root.childCount) {

            val child = root.getChild(i) ?: continue

            val result =
                findEditableNode(child, target)

            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Active window me scrollable node ko scroll karta hai.
     */
    private fun scrollWindow(
        action: Int
    ): Boolean {

        val root = rootInActiveWindow ?: return false

        val scrollable =
            findScrollableNode(root)

        return scrollable?.performAction(action) == true
    }

    /**
     * Pehla visible scrollable node find karta hai.
     */
    private fun findScrollableNode(
        root: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (
            root.isScrollable &&
            root.isVisibleToUser
        ) {
            return root
        }

        for (i in 0 until root.childCount) {

            val child = root.getChild(i) ?: continue

            val result =
                findScrollableNode(child)

            if (result != null) {
                return result
            }
        }

        return null
    }
}