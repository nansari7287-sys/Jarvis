package com.example.jarvis.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

// AI ke alag-alag states define kar rahe hain
enum class OrbState(val primaryColor: Int, val glowColor: Int) {
    IDLE(Color.parseColor("#00E5FF"), Color.parseColor("#008B8B")),       // Cyan
    LISTENING(Color.parseColor("#00FF00"), Color.parseColor("#32CD32")), // Green
    THINKING(Color.parseColor("#8A2BE2"), Color.parseColor("#4B0082")),  // Purple
    SPEAKING(Color.parseColor("#FF00FF"), Color.parseColor("#C71585")),  // Magenta
    ERROR(Color.parseColor("#FF0000"), Color.parseColor("#8B0000"))      // Red
}

class JarvisOrbView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentState = OrbState.IDLE
    
    // Core aur Glow Paints
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        alpha = 150
    }

    // Animation Variables
    private var pulseRadius = 0f
    private var rotationAngle = 0f
    private var currentColor = currentState.primaryColor
    private var currentGlow = currentState.glowColor

    private var pulseAnimator: ValueAnimator? = null
    private var rotationAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null

    init {
        // Hardware acceleration zaroori hai shadow/glow effects ke liye
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        startAnimations()
    }

    private fun startAnimations() {
        // Orb ka धड़कने (Pulsing) ka effect
        pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.0f).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { 
                pulseRadius = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Outer ring ka ghumne (Rotation) ka effect
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 4000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // State change karne ka function (Jaise hi AI bolna shuru kare ya sune)
    fun setOrbState(newState: OrbState) {
        if (currentState == newState) return

        // Smooth Color Transition (Ek color se dusre color me smoothly change hona)
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, newState.primaryColor).apply {
            duration = 500
            addUpdateListener { animator ->
                currentColor = animator.animatedValue as Int
                currentGlow = ArgbEvaluator().evaluate(animator.animatedFraction, currentGlow, newState.glowColor) as Int
                
                // Animators ki speed state ke hisaab se change karna
                when (newState) {
                    OrbState.SPEAKING -> {
                        pulseAnimator?.duration = 400
                        rotationAnimator?.duration = 1500
                    }
                    OrbState.THINKING -> {
                        pulseAnimator?.duration = 800
                        rotationAnimator?.duration = 2000
                    }
                    else -> {
                        pulseAnimator?.duration = 1200
                        rotationAnimator?.duration = 4000
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
        val baseRadius = min(cx, cy) * 0.6f

        // 1. Draw Core Glow and Shadow
        corePaint.color = currentColor
        corePaint.setShadowLayer(80f * pulseRadius, 0f, 0f, currentGlow)
        canvas.drawCircle(cx, cy, baseRadius * pulseRadius, corePaint)

        // 2. Draw Inner Solid Core (Thoda dark feel dene ke liye)
        corePaint.clearShadowLayer()
        corePaint.color = Color.WHITE
        corePaint.alpha = 50
        canvas.drawCircle(cx, cy, baseRadius * 0.9f * pulseRadius, corePaint)

        // 3. Draw Rotating Outer Rings (Sci-Fi look ke liye)
        outerRingPaint.color = currentColor
        outerRingPaint.setShadowLayer(20f, 0f, 0f, currentGlow)
        
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        // Outer ring 1
        canvas.drawArc(
            cx - baseRadius * 1.3f, cy - baseRadius * 1.3f,
            cx + baseRadius * 1.3f, cy + baseRadius * 1.3f,
            0f, 270f, false, outerRingPaint
        )
        canvas.restore()

        canvas.save()
        canvas.rotate(-rotationAngle * 1.5f, cx, cy)
        // Outer ring 2 (Upti direction me ghumegi)
        outerRingPaint.strokeWidth = 4f
        canvas.drawArc(
            cx - baseRadius * 1.15f, cy - baseRadius * 1.15f,
            cx + baseRadius * 1.15f, cy + baseRadius * 1.15f,
            90f, 180f, false, outerRingPaint
        )
        canvas.restore()
    }
}