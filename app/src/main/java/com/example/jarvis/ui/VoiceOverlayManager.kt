package com.example.jarvis.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.example.jarvis.voice.VoiceState

/**
 * JARVIS voice overlay.
 *
 * Voice wake hone par screen ke upar compact JARVIS orb/status
 * dikhata hai. Existing Activity UI ko replace nahi karta.
 */
class VoiceOverlayManager(
    private val activity: Activity
) {

    private var overlayRoot: FrameLayout? = null
    private var orbView: JarvisOrbView? = null
    private var statusText: TextView? = null

    private var currentState =
        VoiceState.IDLE

    // =========================================================
    // SHOW
    // =========================================================

    fun show() {

        if (overlayRoot != null) {
            return
        }

        val decor =
            activity.window.decorView as ViewGroup

        val root =
            FrameLayout(activity)

        root.setBackgroundColor(
            Color.TRANSPARENT
        )

        val container =
            FrameLayout(activity).apply {

                background =
                    createContainerBackground()

                elevation =
                    dp(12f)

                setPadding(
                    dp(12f).toInt(),
                    dp(10f).toInt(),
                    dp(12f).toInt(),
                    dp(10f).toInt()
                )
            }

        // -----------------------------------------------------
        // ORB
        // -----------------------------------------------------

        val orb =
            JarvisOrbView(activity).apply {

                setVoiceState(
                    currentState
                )
            }

        val orbParams =
            FrameLayout.LayoutParams(
                dp(74f).toInt(),
                dp(74f).toInt()
            ).apply {

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        container.addView(
            orb,
            orbParams
        )

        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        val status =
            TextView(activity).apply {

                text =
                    stateText(
                        currentState
                    )

                setTextColor(
                    Color.WHITE
                )

                textSize =
                    13f

                maxLines = 1

                setPadding(
                    dp(12f).toInt(),
                    0,
                    0,
                    0
                )
            }

        val statusParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    Gravity.CENTER_VERTICAL

                leftMargin =
                    dp(74f).toInt()
            }

        container.addView(
            status,
            statusParams
        )

        // -----------------------------------------------------
        // CONTAINER POSITION
        // -----------------------------------------------------

        val containerParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(94f).toInt()
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL

                topMargin =
                    dp(55f).toInt()

                leftMargin =
                    dp(16f).toInt()

                rightMargin =
                    dp(16f).toInt()
            }

        root.addView(
            container,
            containerParams
        )

        decor.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        overlayRoot =
            root

        orbView =
            orb

        statusText =
            status

        updateState(
            currentState
        )
    }

    // =========================================================
    // STATE
    // =========================================================

    fun updateState(
        state: VoiceState
    ) {

        currentState =
            state

        val orb =
            orbView

        val status =
            statusText

        if (orb != null) {

            orb.setVoiceState(
                state
            )
        }

        if (status != null) {

            status.text =
                stateText(
                    state
                )
        }

        if (state == VoiceState.IDLE) {

            hide()
        } else {

            overlayRoot?.visibility =
                View.VISIBLE
        }
    }

    // =========================================================
    // HIDE
    // =========================================================

    fun hide() {

        overlayRoot?.visibility =
            View.GONE
    }

    // =========================================================
    // DESTROY
    // =========================================================

    fun destroy() {

        val root =
            overlayRoot

        if (root != null) {

            val parent =
                root.parent as? ViewGroup

            parent?.removeView(
                root
            )
        }

        overlayRoot = null
        orbView = null
        statusText = null
    }

    // =========================================================
    // STATUS TEXT
    // =========================================================

    private fun stateText(
        state: VoiceState
    ): String {

        return when (state) {

            VoiceState.IDLE ->
                "JARVIS"

            VoiceState.STANDBY ->
                "Ready"

            VoiceState.LISTENING ->
                "Listening..."

            VoiceState.THINKING ->
                "Thinking..."

            VoiceState.EXECUTING ->
                "Executing..."

            VoiceState.SPEAKING ->
                "Speaking..."
        }
    }

    // =========================================================
    // BACKGROUND
    // =========================================================

    private fun createContainerBackground():
        GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.argb(
                    235,
                    8,
                    16,
                    22
                )
            )

            cornerRadius =
                dp(28f)

            setStroke(
                dp(1f).toInt(),
                Color.argb(
                    150,
                    60,
                    220,
                    230
                )
            )
        }
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(
        value: Float
    ): Float {

        return value *
            activity.resources
                .displayMetrics
                .density
    }
}
