package com.example.jarvis.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.LinearLayout
import android.widget.TextView
import com.example.jarvis.R

/**
 * ============================================================================
 * J.A.R.V.I.S. PROFESSIONAL FLOATING OVERLAY MANAGER (GOD TIER)
 * ============================================================================
 * Architect: 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎
 * Developer: 𝑵𝒂𝒆𝒆𝒎
 * 
 * Features:
 * - Draggable Floating HUD
 * - Smooth Neon Color Transitions
 * - Main-Thread Safe Execution
 * - Hardware Accelerated Animations
 * ============================================================================
 */
class OverlayWindowManager(private val context: Context) {

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private lateinit var windowParams: WindowManager.LayoutParams
    
    // UI Elements
    private var statusTextView: TextView? = null
    private var indicatorDot: View? = null
    private var audioVisualizer: LinearLayout? = null
    private var hudContainer: LinearLayout? = null
    
    // Engine Variables
    private var isVisible = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentColor = Color.parseColor("#00E5FF") // Default Jarvis Cyan

    // Dragging Variables
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isVisible) return

        mainHandler.post {
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            windowParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 150 // Default height from bottom
            }

            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.layout_jarvis_overlay, null)

            // Bind Views
            statusTextView = overlayView?.findViewById(R.id.overlayStatusText)
            indicatorDot = overlayView?.findViewById(R.id.overlayDot)
            audioVisualizer = overlayView?.findViewById(R.id.audioVisualizer)
            hudContainer = overlayView?.findViewById(R.id.jarvisHudContainer)

            // Make it Draggable
            setupDragListener()

            try {
                windowManager.addView(overlayView, windowParams)
                isVisible = true
                applyBreathingAnimation()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        overlayView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    // Slight scale up on touch
                    hudContainer?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(150)?.start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    windowParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    // Invert Y because gravity is BOTTOM
                    windowParams.y = initialY - (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, windowParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Revert scale on release
                    hudContainer?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
                    true
                }
                else -> false
            }
        }
    }

    fun updateState(state: OrbState, customText: String? = null) {
        if (!isVisible || statusTextView == null) return

        mainHandler.post {
            val targetText = customText ?: when (state) {
                OrbState.IDLE -> "SYSTEM STANDBY"
                OrbState.LISTENING -> "AWAITING COMMAND"
                OrbState.THINKING -> "PROCESSING..."
                OrbState.SPEAKING -> "TRANSMITTING"
                OrbState.ERROR -> "SYSTEM FAULT"
            }

            val targetColor = when (state) {
                OrbState.LISTENING -> Color.parseColor("#00FF00") // Neon Green
                OrbState.THINKING -> Color.parseColor("#00E5FF")  // Jarvis Cyan
                OrbState.SPEAKING -> Color.parseColor("#FF9100")  // Neon Orange
                OrbState.ERROR -> Color.parseColor("#FF0000")     // Neon Red
                else -> Color.parseColor("#00E5FF")
            }

            statusTextView?.text = targetText

            // Animate Color Transition smoothly
            animateColorChange(targetColor)

            // Handle Visualizer UI
            if (state == OrbState.LISTENING || state == OrbState.THINKING || state == OrbState.SPEAKING) {
                if (audioVisualizer?.visibility != View.VISIBLE) {
                    audioVisualizer?.visibility = View.VISIBLE
                    audioVisualizer?.alpha = 0f
                    audioVisualizer?.animate()?.alpha(1f)?.setDuration(300)?.start()
                }
            } else {
                audioVisualizer?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                    audioVisualizer?.visibility = View.GONE
                }?.start()
            }
        }
    }

    private fun animateColorChange(newColor: Int) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, newColor)
        colorAnimation.duration = 400 // 400ms smooth transition
        colorAnimation.addUpdateListener { animator ->
            val animatedValue = animator.animatedValue as Int
            statusTextView?.setTextColor(animatedValue)
            statusTextView?.setShadowLayer(25f, 0f, 0f, animatedValue)
            indicatorDot?.setBackgroundColor(animatedValue)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                indicatorDot?.outlineSpotShadowColor = animatedValue
                hudContainer?.outlineSpotShadowColor = animatedValue
            }
        }
        colorAnimation.start()
        currentColor = newColor
    }

    private fun applyBreathingAnimation() {
        val pulse = ScaleAnimation(
            1f, 1.2f, 
            1f, 1.2f, 
            Animation.RELATIVE_TO_SELF, 0.5f, 
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        indicatorDot?.startAnimation(pulse)
    }

    fun hide() {
        mainHandler.post {
            if (isVisible && overlayView != null) {
                try {
                    // Smooth fade out before removing
                    overlayView?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                        windowManager.removeView(overlayView)
                        overlayView = null
                        isVisible = false
                    }?.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
