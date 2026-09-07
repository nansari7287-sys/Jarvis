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
     * JARVIS screen-level actions.
     */
    fun performJarvisAction(
        action: String,
        target: String? = null,
        value: String? = null
    ): Boolean {

        return when (action.trim().uppercase()) {

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
                scrollWindow(
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                )
            }

            "SCROLL_DOWN" -> {
                scrollWindow(
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                )
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
     * Visible text ko click karta hai.
     */
    fun clickText(text: String): Boolean {

        if (text.isBlank()) {
            return false
        }

        val root = rootInActiveWindow
            ?: return false

        val node = AccessibilityHelper.findText(
            root,
            text
        )

        return AccessibilityHelper.click(node)
    }

    /**
     * Target text ko find karke click karta hai.
     */
    private fun clickTarget(target: String?): Boolean {

        if (target.isNullOrBlank()) {
            return false
        }

        val root = rootInActiveWindow
            ?: return false

        val nodes =
            root.findAccessibilityNodeInfosByText(target)

        for (node in nodes) {

            if (
                node.isVisibleToUser &&
                node.isClickable
            ) {
                return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            var parent = node.parent

            while (parent != null) {

                if (
                    parent.isVisibleToUser &&
                    parent.isClickable
                ) {
                    return parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                }

                parent = parent.parent
            }
        }

        return clickByContentDescription(
            root,
            target
        )
    }

    /**
     * Content description se button/action find karta hai.
     */
    private fun clickByContentDescription(
        root: AccessibilityNodeInfo,
        target: String
    ): Boolean {

        val targetLower = target.trim().lowercase()

        return findNodeByDescription(
            root,
            targetLower
        )
    }

    private fun findNodeByDescription(
        node: AccessibilityNodeInfo,
        target: String
    ): Boolean {

        val description =
            node.contentDescription
                ?.toString()
                ?.trim()
                ?.lowercase()

        if (
            !description.isNullOrBlank() &&
            description.contains(target) &&
            node.isVisibleToUser
        ) {

            if (node.isClickable) {
                return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            var parent = node.parent

            while (parent != null) {

                if (
                    parent.isVisibleToUser &&
                    parent.isClickable
                ) {
                    return parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                }

                parent = parent.parent
            }
        }

        for (i in 0 until node.childCount) {

            val child = node.getChild(i)
                ?: continue

            if (
                findNodeByDescription(
                    child,
                    target
                )
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Text field me value enter karta hai.
     */
    private fun typeText(
        target: String?,
        value: String?
    ): Boolean {

        if (value.isNullOrBlank()) {
            return false
        }

        val root = rootInActiveWindow
            ?: return false

        val node =
            if (!target.isNullOrBlank()) {
                findEditableNode(
                    root,
                    target
                ) ?: findFocusedEditableNode(root)
            } else {
                findFocusedEditableNode(root)
            }
            ?: return false

        val arguments = Bundle()

        arguments.putCharSequence(
            AccessibilityNodeInfo
                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            value
        )

        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            arguments
        )
    }

    /**
     * Currently focused editable field find karta hai.
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

            val child = root.getChild(i)
                ?: continue

            val result =
                findFocusedEditableNode(child)

            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Target ke text/content-description se
     * editable field find karta hai.
     */
    private fun findEditableNode(
        root: AccessibilityNodeInfo,
        target: String
    ): AccessibilityNodeInfo? {

        val targetLower =
            target.trim().lowercase()

        if (
            root.isEditable &&
            root.isVisibleToUser
        ) {

            val text =
                root.text
                    ?.toString()
                    ?.lowercase()

            val description =
                root.contentDescription
                    ?.toString()
                    ?.lowercase()

            if (
                text?.contains(targetLower) == true ||
                description?.contains(targetLower) == true
            ) {
                return root
            }
        }

        for (i in 0 until root.childCount) {

            val child = root.getChild(i)
                ?: continue

            val result =
                findEditableNode(
                    child,
                    target
                )

            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Active screen ka scrollable node find karke scroll karta hai.
     */
    private fun scrollWindow(
        action: Int
    ): Boolean {

        val root = rootInActiveWindow
            ?: return false

        val scrollable =
            findScrollableNode(root)
                ?: return false

        return scrollable.performAction(action)
    }

    /**
     * First visible scrollable node find karta hai.
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

            val child = root.getChild(i)
                ?: continue

            val result =
                findScrollableNode(child)

            if (result != null) {
                return result
            }
        }

        return null
    }
}