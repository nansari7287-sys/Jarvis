package com.example.jarvis.accessibility

// ==========================================================================================
// J.A.R.V.I.S. TITAN UNIVERSAL OS ACCESSIBILITY ENGINE - ENTERPRISE V500.0
// DEVELOPED BY DRAKOX NAEEM
// PROJECT SCOPE: COMPLETE PHONE-WIDE SYSTEM CONTROL, ZERO CRASH, SELF-CONTAINED ENGINE
// ==========================================================================================

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "TITAN_ACCESSIBILITY"

        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        val isServiceActive: Boolean
            get() = instance != null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "[TITAN KERNEL] Accessibility Engine Hooked to System Successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Real-time window and UI event listener
    }

    override fun onInterrupt() {
        Log.w(TAG, "[TITAN KERNEL] Accessibility Service Interrupted.")
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
        Log.i(TAG, "[TITAN KERNEL] Accessibility Service Destroyed.")
    }

    // ========================================================================
    // [1] EXPANDED ACTION ROUTER (VOICE & AI COMMAND ENTRYPOINT)
    // ========================================================================

    fun performJarvisAction(
        action: String,
        target: String? = null,
        value: String? = null
    ): Boolean {
        return when (action.trim().uppercase()) {
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "RECENTS", "RECENT_APPS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "QUICK_SETTINGS" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "LOCK_SCREEN" -> globalLockScreen()
            "TAKE_SCREENSHOT" -> globalTakeScreenshot()

            // Scroller actions
            "SCROLL_UP" -> scrollWindow(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            "SCROLL_DOWN" -> scrollWindow(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

            // Dynamic Swipes (Short-form videos, Reels, Feeds)
            "SWIPE_UP", "NEXT_REEL" -> {
                universalSwipe(SwipeDirection.UP)
                true
            }
            "SWIPE_DOWN", "PREV_REEL" -> {
                universalSwipe(SwipeDirection.DOWN)
                true
            }
            "SWIPE_LEFT" -> {
                universalSwipe(SwipeDirection.LEFT)
                true
            }
            "SWIPE_RIGHT" -> {
                universalSwipe(SwipeDirection.RIGHT)
                true
            }

            // Universal Click & Type
            "CLICK" -> clickTarget(target, value)
            "TYPE" -> typeText(target, value)

            // Coordinated Touch Simulation
            "TAP" -> {
                val coords = parseCoordinates(value ?: target)
                if (coords != null) {
                    dispatchTap(coords.first, coords.second)
                    true
                } else false
            }

            else -> false
        }
    }

    // ========================================================================
    // [2] CLICK TARGET ENGINE (TEXT, CONTENT-DESC, ID, AND COORD FALLBACK)
    // ========================================================================

    fun clickText(text: String): Boolean {
        if (text.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val node = AccessibilityHelper.findText(root, text.trim())
        return AccessibilityHelper.click(node)
    }

    fun clickTarget(target: String?, value: String?): Boolean {
        val searchText = target?.trim()?.takeIf { it.isNotBlank() }
            ?: value?.trim()?.takeIf { it.isNotBlank() }
            ?: return false

        val root = rootInActiveWindow ?: return false

        if (clickByText(root, searchText)) return true
        if (clickByContentDescription(root, searchText)) return true
        return clickByResourceId(root, searchText)
    }

    private fun clickByText(root: AccessibilityNodeInfo, target: String): Boolean {
        val cleanTarget = target.trim()
        if (cleanTarget.isBlank()) return false

        val nodes = root.findAccessibilityNodeInfosByText(cleanTarget) ?: return false

        for (node in nodes) {
            if (node.isVisibleToUser && performSafeClick(node)) return true
        }
        return false
    }

    private fun clickByContentDescription(root: AccessibilityNodeInfo, target: String): Boolean {
        val targetLower = target.trim().lowercase()
        if (targetLower.isBlank()) return false
        return findNodeByDescription(root, targetLower)
    }

    private fun findNodeByDescription(node: AccessibilityNodeInfo, target: String): Boolean {
        val description = node.contentDescription?.toString()?.trim()?.lowercase()

        if (!description.isNullOrBlank() && description.contains(target) && node.isVisibleToUser) {
            if (performSafeClick(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findNodeByDescription(child, target)) return true
        }
        return false
    }

    private fun clickByResourceId(root: AccessibilityNodeInfo, target: String): Boolean {
        val cleanTarget = target.trim()
        if (cleanTarget.isBlank()) return false

        val nodes = try {
            root.findAccessibilityNodeInfosByViewId(cleanTarget)
        } catch (_: Exception) {
            emptyList()
        } ?: return false

        for (node in nodes) {
            if (node.isVisibleToUser && performSafeClick(node)) return true
        }
        return false
    }

    private fun performSafeClick(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isVisibleToUser && parent.isClickable) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
        }
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            dispatchTap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            return true
        }
        return false
    }

    // ========================================================================
    // [3] TEXT INJECTION ENGINE
    // ========================================================================

    fun typeText(target: String?, value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val root = rootInActiveWindow ?: return false

        val node = if (!target.isNullOrBlank()) {
            findEditableNode(root, target.trim()) ?: findFocusedEditableNode(root)
        } else {
            findFocusedEditableNode(root)
        } ?: findAnyEditableNode(root) ?: return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }

        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } catch (_: Exception) {
            false
        }
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable && root.isFocused && root.isVisibleToUser) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findFocusedEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun findAnyEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable && root.isVisibleToUser) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findAnyEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun findEditableNode(root: AccessibilityNodeInfo, target: String): AccessibilityNodeInfo? {
        val targetLower = target.trim().lowercase()
        if (targetLower.isBlank()) return null

        if (root.isEditable && root.isVisibleToUser) {
            val text = root.text?.toString()?.trim()?.lowercase()
            val description = root.contentDescription?.toString()?.trim()?.lowercase()
            val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                root.hintText?.toString()?.trim()?.lowercase()
            } else null

            if (text?.contains(targetLower) == true ||
                description?.contains(targetLower) == true ||
                hint?.contains(targetLower) == true
            ) {
                return root
            }
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findEditableNode(child, target)
            if (result != null) return result
        }
        return null
    }

    // ========================================================================
    // [4] AUTOMATED WORKFLOWS (WHATSAPP, INSTAGRAM, MESSAGING)
    // ========================================================================

    fun automateWhatsAppSend(messageText: String, onComplete: (Boolean) -> Unit = {}) {
        serviceScope.launch {
            delay(1200)
            val typed = typeText("Message", messageText) ||
                    typeText("संदेश", messageText) ||
                    typeText(null, messageText)

            if (!typed) {
                mainHandler.post { onComplete(false) }
                return@launch
            }

            delay(600)
            val sent = clickTarget("Send", null) ||
                    clickTarget("भेजें", null) ||
                    clickTarget("com.whatsapp:id/send", null)

            mainHandler.post { onComplete(sent) }
        }
    }

    // ========================================================================
    // [5] GESTURES, SWIPES & TOUCH SIMULATION
    // ========================================================================

    enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

    fun universalSwipe(direction: SwipeDirection, durationMs: Long = 300) {
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()

        when (direction) {
            SwipeDirection.UP -> dispatchSwipe(w / 2f, h * 0.8f, w / 2f, h * 0.2f, durationMs)
            SwipeDirection.DOWN -> dispatchSwipe(w / 2f, h * 0.2f, w / 2f, h * 0.8f, durationMs)
            SwipeDirection.LEFT -> dispatchSwipe(w * 0.85f, h / 2f, w * 0.15f, h / 2f, durationMs)
            SwipeDirection.RIGHT -> dispatchSwipe(w * 0.15f, h / 2f, w * 0.85f, h / 2f, durationMs)
        }
    }

    fun dispatchTap(x: Float, y: Float, durationMs: Long = 100) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun scrollWindow(action: Int): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(root) ?: return false
        return try {
            scrollable.performAction(action)
        } catch (_: Exception) {
            false
        }
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable && root.isVisibleToUser) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) return result
        }
        return null
    }

    // ========================================================================
    // [6] AI VISION & FULL SCREEN DATA DUMP (JSON)
    // ========================================================================

    fun dumpScreenHierarchyAsJson(): String {
        val root = rootInActiveWindow ?: return "{ \"error\": \"No active window\" }"
        val jsonArray = JSONArray()
        collectHierarchyNodes(root, jsonArray)

        return JSONObject().apply {
            put("package", root.packageName ?: "unknown")
            put("elements", jsonArray)
        }.toString()
    }

    private fun collectHierarchyNodes(node: AccessibilityNodeInfo, array: JSONArray) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val resId = node.viewIdResourceName ?: ""

        if (text.isNotBlank() || desc.isNotBlank() || node.isClickable || node.isEditable) {
            array.put(JSONObject().apply {
                put("text", text)
                put("desc", desc)
                put("id", resId)
                put("clickable", node.isClickable)
                put("editable", node.isEditable)
                put("x", rect.centerX())
                put("y", rect.centerY())
            })
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectHierarchyNodes(child, array)
        }
    }

    // ========================================================================
    // [7] OS GLOBAL HELPERS & INTERNAL UTILITY
    // ========================================================================

    private fun globalLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else false
    }

    private fun globalTakeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else false
    }

    private fun parseCoordinates(coordStr: String?): Pair<Float, Float>? {
        if (coordStr.isNullOrBlank()) return null
        return try {
            val parts = coordStr.replace("(", "").replace(")", "").split(",", " ")
                .filter { it.isNotBlank() }
            if (parts.size >= 2) Pair(parts[0].toFloat(), parts[1].toFloat()) else null
        } catch (_: Exception) {
            null
        }
    }

    // ========================================================================
    // [8] SELF-CONTAINED ACCESSIBILITY HELPER (NO EXTERNAL FILE REQUIRED)
    // ========================================================================

    object AccessibilityHelper {
        fun findText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
            val nodes = root.findAccessibilityNodeInfosByText(text) ?: return null
            for (node in nodes) {
                if (node.isVisibleToUser) return node
            }
            return null
        }

        fun click(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            var parent = node.parent
            while (parent != null) {
                if (parent.isVisibleToUser && parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
            return false
        }
    }
}
