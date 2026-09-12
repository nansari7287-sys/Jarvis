package com.example.jarvis.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

/**
 * JARVIS Next-Gen Holographic Voice Overlay.
 * 
 * Features added:
 * - Glassmorphism Glowing Background
 * - Overshoot Entry & Decelerate Exit Animations
 * - Neon Glowing Text Shadows
 * - Haptic Engine Integration for State Changes
 */
class VoiceOverlayManager(
    private val activity: Activity
) {

    private var overlayRoot: FrameLayout? = null
    private var overlayContainer: FrameLayout? = null
    private var orbView: JarvisOrbView? = null
    private var statusText: TextView? = null

    private var currentState = OrbState.IDLE
    private var isVisible = false

    // =========================================================
    // INITIALIZATION & SHOW
    // =========================================================

    fun show() {

        if (isVisible) return

        if (overlayRoot == null) {
            buildOverlay()
        }

        isVisible = true
        overlayRoot?.visibility = View.VISIBLE

        // Cinematic Pop-In Animation (Scale + Alpha + TranslationY)
        overlayContainer?.let { container ->
            container.translationY = -150f
            container.alpha = 0f
            container.scaleX = 0.8f
            container.scaleY = 0.8f

            val animator = ObjectAnimator.ofPropertyValuesHolder(
                container,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
            ).apply {
                duration = 450
                interpolator = OvershootInterpolator(1.2f)
            }
            animator.start()
        }

        triggerHapticFeedback()
    }

    // =========================================================
    // BUILD OVERLAY UI (GLASSMORPHISM & GLOW)
    // =========================================================

    private fun buildOverlay() {

        val decor = activity.window.decorView as ViewGroup

        val root = FrameLayout(activity).apply {
            setBackgroundColor(Color.TRANSPARENT)
            elevation = dp(20f)
        }

        val container = FrameLayout(activity).apply {
            background = createGlassmorphismBackground()
            elevation = dp(15f)
            setPadding(
                dp(12f).toInt(),
                dp(10f).toInt(),
                dp(16f).toInt(),
                dp(10f).toInt()
            )
        }

        // -----------------------------------------------------
        // SCI-FI ORB
        // -----------------------------------------------------

        val orb = JarvisOrbView(activity).apply {
            // ERROR FIXED: Updated from setVoiceState to setOrbState
            setOrbState(currentState)
        }

        val orbParams = FrameLayout.LayoutParams(
            dp(70f).toInt(),
            dp(70f).toInt()
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        
        container.addView(orb, orbParams)

        // -----------------------------------------------------
        // GLOWING NEON STATUS TEXT
        // -----------------------------------------------------

        val status = TextView(activity).apply {
            text = stateText(currentState)
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 1
            // Adding Neon Text Glow
            setShadowLayer(15f, 0f, 0f, Color.parseColor("#00FFFF"))
            setPadding(dp(76f).toInt(), 0, 0, 0)
        }

        val statusParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        container.addView(status, statusParams)

        // -----------------------------------------------------
        // CONTAINER POSITIONING (Top Center Float)
        // -----------------------------------------------------

        val containerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            dp(90f).toInt()
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = dp(60f).toInt()
        }

        root.addView(container, containerParams)
        decor.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        overlayRoot = root
        overlayContainer = container
        orbView = orb
        statusText = status
    }

    // =========================================================
    // DYNAMIC STATE UPDATES
    // =========================================================

    fun updateState(state: OrbState) {
        
        // Agar pehle se same state hai, toh re-render mat karo
        if (currentState == state) return
        currentState = state

        orbView?.setOrbState(state)

        statusText?.let { tv ->
            tv.text = stateText(state)
            
            // State ke hisaab se neon text ka color change karna
            val glowColor = when (state) {
                OrbState.LISTENING -> Color.parseColor("#00FF00") // Green
                OrbState.THINKING -> Color.parseColor("#8A2BE2")  // Purple
                OrbState.SPEAKING -> Color.parseColor("#FF00FF")  // Magenta
                OrbState.ERROR -> Color.parseColor("#FF0000")     // Red
                else -> Color.parseColor("#00FFFF")               // Cyan
            }
            tv.setShadowLayer(20f, 0f, 0f, glowColor)
        }

        if (state == OrbState.IDLE) {
            hide()
        } else {
            show()
            // Haptic trigger on important state changes
            if (state == OrbState.LISTENING || state == OrbState.THINKING) {
                triggerHapticFeedback()
            }
        }
    }

    // =========================================================
    // CINEMATIC HIDE
    // =========================================================

    fun hide() {
        if (!isVisible || overlayContainer == null) return
        isVisible = false

        // Smooth Slide Up & Fade Out
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            overlayContainer,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -150f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f)
        ).apply {
            duration = 350
            interpolator = DecelerateInterpolator(1.5f)
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isVisible) {
                    overlayRoot?.visibility = View.GONE
                }
            }
        })
        
        animator.start()
    }

    // =========================================================
    // DESTROY & CLEANUP
    // =========================================================

    fun destroy() {
        overlayRoot?.let { root ->
            val parent = root.parent as? ViewGroup
            parent?.removeView(root)
        }

        overlayRoot = null
        overlayContainer = null
        orbView = null
        statusText = null
    }

    // =========================================================
    // TEXT GENERATOR
    // =========================================================

    private fun stateText(state: OrbState): String {
        return when (state) {
            OrbState.IDLE -> "Standby"
            OrbState.LISTENING -> "Listening..."
            OrbState.THINKING -> "Processing Data..."
            OrbState.SPEAKING -> "System Active..."
            OrbState.ERROR -> "System Error"
        }
    }

    // =========================================================
    // PREMIUM GLASSMORPHISM BACKGROUND
    // =========================================================

    private fun createGlassmorphismBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(35f) // Deep curved pill shape
            
            // Dark transparent background
            setColor(Color.argb(200, 10, 15, 25))
            
            // Glowing Cyber-Neon Border
            setStroke(
                dp(1.5f).toInt(),
                Color.argb(180, 0, 229, 255)
            )
        }
    }

    // =========================================================
    // HAPTIC ENGINE (FUTURISTIC FEEL)
    // =========================================================

    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Creates a crisp, premium "tick" feel on modern devices
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            // Ignore if device lacks vibration motor
        }
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private fun dp(value: Float): Float {
        return value * activity.resources.displayMetrics.density
    }
}
