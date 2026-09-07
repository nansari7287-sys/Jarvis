package com.example.jarvis.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.jarvis.ai.JarvisCommand
import com.example.jarvis.ai.JarvisStep

/**
 * JARVIS ke structured commands ko Android actions me convert karta hai.
 *
 * IMPORTANT:
 * - Sirf allowed/safe actions execute kiye jaate hain.
 * - Sensitive actions ko confirmation ki zarurat ho sakti hai.
 * - Screen-level automation AccessibilityService karegi.
 */
class CommandExecutor(
    private val context: Context
) {

    fun execute(command: JarvisCommand): Boolean {

        if (command.requiresConfirmation) {
            showConfirmationRequired(command)
            return false
        }

        // Multi-step command
        if (command.steps.isNotEmpty()) {
            var success = true

            for (step in command.steps) {
                if (step.requiresConfirmation) {
                    showConfirmationRequired(
                        JarvisCommand(
                            action = step.action,
                            target = step.target,
                            value = step.value,
                            requiresConfirmation = true
                        )
                    )
                    return false
                }

                if (!executeStep(step)) {
                    success = false
                    break
                }
            }

            return success
        }

        return executeAction(
            action = command.action,
            target = command.target,
            value = command.value
        )
    }

    private fun executeStep(step: JarvisStep): Boolean {
        return executeAction(
            action = step.action,
            target = step.target,
            value = step.value
        )
    }

    private fun executeAction(
        action: String,
        target: String?,
        value: String?
    ): Boolean {

        return when (action.uppercase()) {

            "OPEN_APP" -> openApp(target)

            "OPEN_URL",
            "WEB_SEARCH" -> openUrl(
                if (action.uppercase() == "WEB_SEARCH") {
                    "https://www.google.com/search?q=" +
                        Uri.encode(value ?: target ?: "")
                } else {
                    value ?: target ?: ""
                }
            )

            "YOUTUBE" -> openUrl(
                if (!value.isNullOrBlank()) {
                    "https://www.youtube.com/results?search_query=" +
                        Uri.encode(value)
                } else {
                    "https://www.youtube.com"
                }
            )

            "INSTAGRAM" -> openUrl(
                "https://www.instagram.com/"
            )

            "WHATSAPP" -> openWhatsApp()

            "BACK" -> performAccessibilityAction("BACK")

            "HOME" -> performAccessibilityAction("HOME")

            "RECENT_APPS" -> performAccessibilityAction("RECENTS")

            "SCROLL_UP" -> performAccessibilityAction("SCROLL_UP")

            "SCROLL_DOWN" -> performAccessibilityAction("SCROLL_DOWN")

            "CLICK" -> {
                // AccessibilityService screen element ko handle karegi.
                performAccessibilityAction(
                    "CLICK",
                    target,
                    value
                )
            }

            "TYPE" -> {
                // AccessibilityService text field me value enter karegi.
                performAccessibilityAction(
                    "TYPE",
                    target,
                    value
                )
            }

            "WAIT" -> {
                // WAIT ko unrestricted delay ke roop me execute nahi karte.
                // Accessibility layer timing handle karegi.
                true
            }

            "NO_ACTION" -> true

            else -> {
                Toast.makeText(
                    context,
                    "JARVIS: Action not supported: $action",
                    Toast.LENGTH_SHORT
                ).show()

                false
            }
        }
    }

    private fun openApp(target: String?): Boolean {

        val packageName = when (target?.lowercase()) {

            "instagram" ->
                "com.instagram.android"

            "youtube" ->
                "com.google.android.youtube"

            "whatsapp" ->
                "com.whatsapp"

            "chrome" ->
                "com.android.chrome"

            "google" ->
                "com.google.android.googlequicksearchbox"

            "settings" ->
                "com.android.settings"

            else -> null
        }

        if (packageName == null) {
            Toast.makeText(
                context,
                "JARVIS: App not supported: $target",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        return try {

            val intent =
                context.packageManager.getLaunchIntentForPackage(packageName)

            if (intent == null) {
                Toast.makeText(
                    context,
                    "App installed nahi hai: $target",
                    Toast.LENGTH_SHORT
                ).show()

                false
            } else {

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                true
            }

        } catch (_: Exception) {

            Toast.makeText(
                context,
                "App open nahi ho saka: $target",
                Toast.LENGTH_SHORT
            ).show()

            false
        }
    }

    private fun openWhatsApp(): Boolean {

        return try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("whatsapp://")
            )

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            true

        } catch (_: Exception) {

            openUrl("https://web.whatsapp.com")
        }
    }

    private fun openUrl(url: String): Boolean {

        if (url.isBlank()) {
            return false
        }

        return try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            true

        } catch (_: Exception) {

            Toast.makeText(
                context,
                "Link open nahi ho saka",
                Toast.LENGTH_SHORT
            ).show()

            false
        }
    }

    /**
     * AccessibilityService ke liye bridge.
     *
     * Actual screen automation JARVISAccessibilityService
     * ke through hogi.
     */
    private fun performAccessibilityAction(
        action: String,
        target: String? = null,
        value: String? = null
    ): Boolean {

        val service =
            com.example.jarvis.accessibility.JarvisAccessibilityService.instance

        if (service == null) {

            Toast.makeText(
                context,
                "JARVIS Accessibility Service enabled nahi hai",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        return service.performJarvisAction(
            action = action,
            target = target,
            value = value
        )
    }

    private fun showConfirmationRequired(
        command: JarvisCommand
    ) {

        Toast.makeText(
            context,
            "Confirmation required: ${command.action}",
            Toast.LENGTH_LONG
        ).show()
    }
}
