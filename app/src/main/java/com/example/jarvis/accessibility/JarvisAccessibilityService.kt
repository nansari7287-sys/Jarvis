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

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        // Screen changes can be observed here when required.
    }

    override fun onInterrupt() {
        // Required by AccessibilityService.
    }

    override fun onDestroy() {

        if (instance === this) {
            instance = null
        }

        super.onDestroy()
    }

    // =========================================================
    // MAIN JARVIS ACTION ROUTER
    // =========================================================

    fun performJarvisAction(
        action: String,
        target: String? = null,
        value: String? = null
    ): Boolean {

        return when (action.trim().uppercase()) {

            "BACK" -> {
                performGlobalAction(
                    GLOBAL_ACTION_BACK
                )
            }

            "HOME" -> {
                performGlobalAction(
                    GLOBAL_ACTION_HOME
                )
            }

            "RECENTS",
            "RECENT_APPS" -> {
                performGlobalAction(
                    GLOBAL_ACTION_RECENTS
                )
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
                clickTarget(target, value)
            }

            "TYPE" -> {
                typeText(target, value)
            }

            else -> {
                false
            }
        }
    }

    // =========================================================
    // CLICK BY VISIBLE TEXT
    // =========================================================

    fun clickText(
        text: String
    ): Boolean {

        if (text.isBlank()) {
            return false
        }

        val root =
            rootInActiveWindow
                ?: return false

        val node =
            AccessibilityHelper.findText(
                root,
                text.trim()
            )

        return AccessibilityHelper.click(node)
    }

    // =========================================================
    // CLICK TARGET
    // =========================================================

    private fun clickTarget(
        target: String?,
        value: String?
    ): Boolean {

        val searchText =
            target?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: value?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: return false

        val root =
            rootInActiveWindow
                ?: return false

        // First try exact/normal text matching.
        if (clickByText(
                root,
                searchText
            )
        ) {
            return true
        }

        // Then try content description.
        if (clickByContentDescription(
                root,
                searchText
            )
        ) {
            return true
        }

        // Finally try resource-id matching.
        return clickByResourceId(
            root,
            searchText
        )
    }

    // =========================================================
    // CLICK BY TEXT
    // =========================================================

    private fun clickByText(
        root: AccessibilityNodeInfo,
        target: String
    ): Boolean {

        val cleanTarget =
            target.trim()

        if (cleanTarget.isBlank()) {
            return false
        }

        val nodes =
            root.findAccessibilityNodeInfosByText(
                cleanTarget
            )

        // Prefer an actually clickable matching node.
        for (node in nodes) {

            if (
                node.isVisibleToUser &&
                node.isClickable
            ) {

                return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }
        }

        // If text node itself is not clickable,
        // walk upward to its clickable parent.
        for (node in nodes) {

            if (!node.isVisibleToUser) {
                continue
            }

            var parent =
                node.parent

            while (parent != null) {

                if (
                    parent.isVisibleToUser &&
                    parent.isClickable
                ) {

                    return parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                }

                parent =
                    parent.parent
            }
        }

        return false
    }

    // =========================================================
    // CLICK BY CONTENT DESCRIPTION
    // =========================================================

    private fun clickByContentDescription(
        root: AccessibilityNodeInfo,
        target: String
    ): Boolean {

        val targetLower =
            target.trim().lowercase()

        if (targetLower.isBlank()) {
            return false
        }

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
            (
                description == target ||
                description.contains(target)
            ) &&
            node.isVisibleToUser
        ) {

            if (node.isClickable) {

                return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            var parent =
                node.parent

            while (parent != null) {

                if (
                    parent.isVisibleToUser &&
                    parent.isClickable
                ) {

                    return parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                }

                parent =
                    parent.parent
            }
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
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

    // =========================================================
    // CLICK BY RESOURCE ID
    // =========================================================

    private fun clickByResourceId(
        root: AccessibilityNodeInfo,
        target: String
    ): Boolean {

        val cleanTarget =
            target.trim()

        if (cleanTarget.isBlank()) {
            return false
        }

        val nodes =
            try {
                root.findAccessibilityNodeInfosByViewId(
                    cleanTarget
                )
            } catch (_: Exception) {
                emptyList()
            }

        for (node in nodes) {

            if (
                node.isVisibleToUser &&
                node.isClickable
            ) {

                return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            var parent =
                node.parent

            while (parent != null) {

                if (
                    parent.isVisibleToUser &&
                    parent.isClickable
                ) {

                    return parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                }

                parent =
                    parent.parent
            }
        }

        return false
    }

    // =========================================================
    // TYPE TEXT
    // =========================================================

    private fun typeText(
        target: String?,
        value: String?
    ): Boolean {

        if (value.isNullOrBlank()) {
            return false
        }

        val root =
            rootInActiveWindow
                ?: return false

        val node =
            if (!target.isNullOrBlank()) {

                findEditableNode(
                    root,
                    target.trim()
                )
                    ?: findFocusedEditableNode(root)

            } else {

                findFocusedEditableNode(root)
            }
                ?: findAnyEditableNode(root)
                ?: return false

        val arguments =
            Bundle()

        arguments.putCharSequence(
            AccessibilityNodeInfo
                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            value
        )

        return try {

            node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
            )

        } catch (_: Exception) {

            false
        }
    }

    // =========================================================
    // FIND FOCUSED EDITABLE
    // =========================================================

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

            val child =
                root.getChild(i)
                    ?: continue

            val result =
                findFocusedEditableNode(
                    child
                )

            if (result != null) {
                return result
            }
        }

        return null
    }

    // =========================================================
    // FIND ANY EDITABLE FIELD
    // =========================================================

    private fun findAnyEditableNode(
        root: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (
            root.isEditable &&
            root.isVisibleToUser
        ) {
            return root
        }

        for (i in 0 until root.childCount) {

            val child =
                root.getChild(i)
                    ?: continue

            val result =
                findAnyEditableNode(
                    child
                )

            if (result != null) {
                return result
            }
        }

        return null
    }

    // =========================================================
    // FIND EDITABLE BY TARGET
    // =========================================================

    private fun findEditableNode(
        root: AccessibilityNodeInfo,
        target: String
    ): AccessibilityNodeInfo? {

        val targetLower =
            target.trim().lowercase()

        if (targetLower.isBlank()) {
            return null
        }

        if (
            root.isEditable &&
            root.isVisibleToUser
        ) {

            val text =
                root.text
                    ?.toString()
                    ?.trim()
                    ?.lowercase()

            val description =
                root.contentDescription
                    ?.toString()
                    ?.trim()
                    ?.lowercase()

            val hint =
                root.hintText
                    ?.toString()
                    ?.trim()
                    ?.lowercase()

            if (
                text?.contains(targetLower) == true ||
                description?.contains(targetLower) == true ||
                hint?.contains(targetLower) == true
            ) {

                return root
            }
        }

        for (i in 0 until root.childCount) {

            val child =
                root.getChild(i)
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

    // =========================================================
    // SCROLL
    // =========================================================

    private fun scrollWindow(
        action: Int
    ): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val scrollable =
            findScrollableNode(root)
                ?: return false

        return try {

            scrollable.performAction(
                action
            )

        } catch (_: Exception) {

            false
        }
    }

    // =========================================================
    // FIND SCROLLABLE NODE
    // =========================================================

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

            val child =
                root.getChild(i)
                    ?: continue

            val result =
                findScrollableNode(
                    child
                )

            if (result != null) {
                return result
            }
        }

        return null
    }
}