package com.example.jarvis.ui

// ============================================================================
// J.A.R.V.I.S. TITAN HOLOGRAM ORB ENGINE (V500.0)
// Architect: DrakoXNaeem
// Features: Cyberpunk Neon Glow, Hardware Acceleration Safe, Smooth Interpolation
// ============================================================================

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.min

enum class OrbState(val primaryColor: Int, val glowColor: Int) {
    IDLE(Color.parseColor("#00E5FF"), Color.parseColor("#008B8B")),       // Deep Cyan
    LISTENING(Color.parseColor("#00FF99"), Color.parseColor("#009933")), // Matrix Green
    THINKING(Color.parseColor("#9900FF"), Color.parseColor("#4B0082")),  // Quantum Purple
    SPEAKING(Color.parseColor("#FF9100"), Color.parseColor("#B34700")),  // Amber / Orange
    ERROR(Color.parseColor("#FF0033"), Color.parseColor("#8B0000"))      // Combat Red
}

class JarvisOrbView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentState = OrbState.IDLE
    
    // Core Paints
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Animation Tracking Variables
    private var pulseRadius = 0.85f
    private var rotationAngle = 0f
    private var currentColor = currentState.primaryColor
    private var currentGlow = currentState.glowColor

    private var pulseAnimator: ValueAnimator? = null
    private var rotationAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null

    init {
        // LAYER_TYPE_SOFTWARE is required for setShadowLayer to work properly on all Android versions
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        startAnimations()
    }

    private fun startAnimations() {
        // Smooth heartbeat pulsing effect
        pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.05f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { 
                pulseRadius = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Continuous sci-fi mechanical rotation
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 5000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setOrbState(newState: OrbState) {
        if (currentState == newState) return

        // Smooth Color & Physics Transition Algorithm
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, newState.primaryColor).apply {
            duration = 600
            addUpdateListener { animator ->
                currentColor = animator.animatedValue as Int
                currentGlow = ArgbEvaluator().evaluate(animator.animatedFraction, currentGlow, newState.glowColor) as Int
                
                // Adjust kinetic speeds based on AI cognitive states
                when (newState) {
                    OrbState.SPEAKING -> {
                        pulseAnimator?.duration = 300
                        rotationAnimator?.duration = 1200
                    }
                    OrbState.THINKING -> {
                        pulseAnimator?.duration = 600
                        rotationAnimator?.duration = 2000
                    }
                    OrbState.LISTENING -> {
                        pulseAnimator?.duration = 450
                        rotationAnimator?.duration = 1800
                    }
                    OrbState.ERROR -> {
                        pulseAnimator?.duration = 200
                        rotationAnimator?.duration = 800
                    }
                    else -> {
                        pulseAnimator?.duration = 1200
                        rotationAnimator?.duration = 5000
                    }
                }
                invalidate()
            }
            start()
        }
        currentState = newState
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(cx, cy) * 0.55f // Kept slightly small to allow glow bleeding
        val dynamicRadius = baseRadius * pulseRadius

        // 1. Draw 3D Radial Core Glow
        val gradient = RadialGradient(
            cx, cy, dynamicRadius * 1.5f,
            intArrayOf(Color.WHITE, currentColor, Color.TRANSPARENT),
            floatArrayOf(0.1f, 0.6f, 1.0f),
            Shader.TileMode.CLAMP
        )
        corePaint.shader = gradient
        corePaint.setShadowLayer(50f * pulseRadius, 0f, 0f, currentGlow)
        canvas.drawCircle(cx, cy, dynamicRadius, corePaint)

        // Reset shader for hardware overlays
        corePaint.shader = null

        // 2. Outer Rotating Quantum Rings (Layer 1)
        outerRingPaint.color = currentColor
        outerRingPaint.strokeWidth = 6f
        outerRingPaint.alpha = 200
        outerRingPaint.setShadowLayer(15f, 0f, 0f, currentGlow)
        
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        canvas.drawArc(
            cx - baseRadius * 1.4f, cy - baseRadius * 1.4f,
            cx + baseRadius * 1.4f, cy + baseRadius * 1.4f,
            15f, 240f, false, outerRingPaint
        )
        canvas.restore()

        // 3. Inner Reverse Rotating Rings (Layer 2)
        canvas.save()
        canvas.rotate(-rotationAngle * 1.8f, cx, cy)
        outerRingPaint.strokeWidth = 3f
        outerRingPaint.alpha = 150
        canvas.drawArc(
            cx - baseRadius * 1.2f, cy - baseRadius * 1.2f,
            cx + baseRadius * 1.2f, cy + baseRadius * 1.2f,
            60f, 150f, false, outerRingPaint
        )
        // Extra HUD Dash
        canvas.drawArc(
            cx - baseRadius * 1.2f, cy - baseRadius * 1.2f,
            cx + baseRadius * 1.2f, cy + baseRadius * 1.2f,
            240f, 60f, false, outerRingPaint
        )
        canvas.restore()
    }
}
