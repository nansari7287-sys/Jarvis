package com.example.jarvis.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.jarvis.voice.VoiceState
import kotlin.math.sin

/**
 * JARVIS animated voice orb.
 *
 * States:
 * IDLE       -> subtle static glow
 * STANDBY    -> slow breathing animation
 * LISTENING  -> active pulsing
 * THINKING   -> rotating/pulsing effect
 * EXECUTING  -> stronger active glow
 * SPEAKING   -> speaking waveform-like pulse
 */
class JarvisOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var voiceState = VoiceState.IDLE

    private var animationValue = 0f
    private var pulseValue = 0f
    private var rotationValue = 0f

    private var animator: ValueAnimator? = null

    init {
        isClickable = false
        isFocusable = false

        corePaint.style = Paint.Style.FILL

        glowPaint.style = Paint.Style.FILL

        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = dp(2f)

        wavePaint.style = Paint.Style.STROKE
        wavePaint.strokeWidth = dp(2f)

        startAnimation()
    }

    // =========================================================
    // STATE
    // =========================================================

    fun setVoiceState(
        state: VoiceState
    ) {

        if (voiceState == state) {
            return
        }

        voiceState = state

        invalidate()
    }

    fun getVoiceState(): VoiceState {
        return voiceState
    }

    // =========================================================
    // ANIMATION
    // =========================================================

    private fun startAnimation() {

        animator?.cancel()

        animator = ValueAnimator.ofFloat(
            0f,
            1f
        ).apply {

            duration = 1800L

            repeatCount =
                ValueAnimator.INFINITE

            repeatMode =
                ValueAnimator.RESTART

            interpolator =
                DecelerateInterpolator()

            addUpdateListener {

                animationValue =
                    it.animatedValue as Float

                pulseValue =
                    (sin(
                        animationValue *
                            Math.PI *
                            2.0
                    ) * 0.5 + 0.5).toFloat()

                rotationValue =
                    animationValue * 360f

                invalidate()
            }

            start()
        }
    }

    // =========================================================
    // DRAW
    // =========================================================

    override fun onDraw(
        canvas: Canvas
    ) {

        super.onDraw(canvas)

        val centerX =
            width / 2f

        val centerY =
            height / 2f

        val baseRadius =
            minOf(
                width,
                height
            ) * 0.25f

        val pulseAmount =
            when (voiceState) {

                VoiceState.IDLE ->
                    0.01f

                VoiceState.STANDBY ->
                    0.06f

                VoiceState.LISTENING ->
                    0.13f

                VoiceState.THINKING ->
                    0.09f

                VoiceState.EXECUTING ->
                    0.15f

                VoiceState.SPEAKING ->
                    0.18f
            }

        val radius =
            baseRadius *
                (
                    1f +
                        pulseValue *
                        pulseAmount
                )

        drawOuterGlow(
            canvas,
            centerX,
            centerY,
            radius
        )

        drawOuterRings(
            canvas,
            centerX,
            centerY,
            radius
        )

        drawCore(
            canvas,
            centerX,
            centerY,
            radius
        )

        if (
            voiceState == VoiceState.LISTENING ||
            voiceState == VoiceState.SPEAKING
        ) {

            drawVoiceWaves(
                canvas,
                centerX,
                centerY,
                radius
            )
        }

        if (
            voiceState == VoiceState.THINKING ||
            voiceState == VoiceState.EXECUTING
        ) {

            drawActivityRing(
                canvas,
                centerX,
                centerY,
                radius
            )
        }
    }

    // =========================================================
    // CORE
    // =========================================================

    private fun drawCore(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        val coreRadius =
            radius * 0.78f

        corePaint.shader =
            RadialGradient(
                centerX,
                centerY,
                coreRadius,
                intArrayOf(
                    0xFFFFFFFF.toInt(),
                    0xFFB8FFFF.toInt(),
                    0xFF26E6E6.toInt(),
                    0xFF073C4A.toInt()
                ),
                floatArrayOf(
                    0f,
                    0.25f,
                    0.65f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )

        canvas.drawCircle(
            centerX,
            centerY,
            coreRadius,
            corePaint
        )

        corePaint.shader = null
    }

    // =========================================================
    // OUTER GLOW
    // =========================================================

    private fun drawOuterGlow(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        val glowRadius =
            radius *
                when (voiceState) {

                    VoiceState.IDLE -> 1.45f
                    VoiceState.STANDBY -> 1.65f
                    VoiceState.LISTENING -> 1.9f
                    VoiceState.THINKING -> 1.8f
                    VoiceState.EXECUTING -> 2.0f
                    VoiceState.SPEAKING -> 2.1f
                }

        glowPaint.shader =
            RadialGradient(
                centerX,
                centerY,
                glowRadius,
                intArrayOf(
                    0x5526E6E6,
                    0x2226E6E6,
                    0x0016A6B6
                ),
                floatArrayOf(
                    0f,
                    0.45f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )

        canvas.drawCircle(
            centerX,
            centerY,
            glowRadius,
            glowPaint
        )

        glowPaint.shader = null
    }

    // =========================================================
    // OUTER RINGS
    // =========================================================

    private fun drawOuterRings(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        val intensity =
            when (voiceState) {

                VoiceState.IDLE -> 0.25f
                VoiceState.STANDBY -> 0.45f
                VoiceState.LISTENING -> 0.85f
                VoiceState.THINKING -> 0.70f
                VoiceState.EXECUTING -> 0.90f
                VoiceState.SPEAKING -> 1.0f
            }

        ringPaint.alpha =
            (255f * intensity).toInt()

        ringPaint.strokeWidth =
            dp(1.5f)

        canvas.drawCircle(
            centerX,
            centerY,
            radius * 1.15f,
            ringPaint
        )

        ringPaint.alpha =
            (180f * intensity).toInt()

        ringPaint.strokeWidth =
            dp(1f)

        canvas.drawCircle(
            centerX,
            centerY,
            radius * 1.38f +
                pulseValue *
                dp(5f),
            ringPaint
        )
    }

    // =========================================================
    // VOICE WAVES
    // =========================================================

    private fun drawVoiceWaves(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        wavePaint.alpha = 180

        wavePaint.strokeWidth =
            dp(1.5f)

        val waveRadius =
            radius *
                (
                    1.35f +
                        pulseValue *
                        0.35f
                )

        canvas.save()

        canvas.rotate(
            rotationValue * 0.15f,
            centerX,
            centerY
        )

        for (i in 0 until 8) {

            val angle =
                Math.toRadians(
                    (i * 45).toDouble()
                )

            val waveLength =
                dp(4f) +
                    pulseValue *
                    dp(
                        if (
                            voiceState ==
                            VoiceState.SPEAKING
                        ) 14f else 10f
                    )

            val startRadius =
                waveRadius +
                    dp(3f)

            val startX =
                centerX +
                    (
                        kotlin.math.cos(angle) *
                            startRadius
                    ).toFloat()

            val startY =
                centerY +
                    (
                        kotlin.math.sin(angle) *
                            startRadius
                    ).toFloat()

            val endRadius =
                startRadius +
                    waveLength

            val endX =
                centerX +
                    (
                        kotlin.math.cos(angle) *
                            endRadius
                    ).toFloat()

            val endY =
                centerY +
                    (
                        kotlin.math.sin(angle) *
                            endRadius
                    ).toFloat()

            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                wavePaint
            )
        }

        canvas.restore()
    }

    // =========================================================
    // ACTIVITY RING
    // =========================================================

    private fun drawActivityRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        ringPaint.alpha = 220

        ringPaint.strokeWidth =
            dp(2f)

        canvas.save()

        canvas.rotate(
            rotationValue,
            centerX,
            centerY
        )

        val rectRadius =
            radius * 1.55f

        val left =
            centerX - rectRadius

        val top =
            centerY - rectRadius

        val right =
            centerX + rectRadius

        val bottom =
            centerY + rectRadius

        val rect =
            android.graphics.RectF(
                left,
                top,
                right,
                bottom
            )

        canvas.drawArc(
            rect,
            0f,
            100f,
            false,
            ringPaint
        )

        canvas.drawArc(
            rect,
            180f,
            75f,
            false,
            ringPaint
        )

        canvas.restore()
    }

    // =========================================================
    // SIZE
    // =========================================================

    private fun dp(
        value: Float
    ): Float {

        return value *
            resources.displayMetrics.density
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDetachedFromWindow() {

        animator?.cancel()

        animator = null

        super.onDetachedFromWindow()
    }
}
