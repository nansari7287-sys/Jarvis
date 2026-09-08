
package com.example.jarvis.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Controls the background voice lifecycle used by VoiceService.
 *
 * Important:
 * This controller manages the voice state and command-recognition
 * cycle. The Android foreground service owns its lifecycle.
 *
 * Wake-word detection is intentionally kept separate from the
 * normal SpeechRecognizer command cycle.
 */
class BackgroundVoiceController(
    private val context: Context,
    private val onStateChanged: (VoiceState) -> Unit,
    private val onWakeDetected: () -> Unit,
    private val onCommand: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val handler =
        Handler(Looper.getMainLooper())

    private val running =
        AtomicBoolean(false)

    private val wakeMode =
        AtomicBoolean(false)

    private val commandMode =
        AtomicBoolean(false)

    private var speechRecognizer: SpeechRecognizerManager? = null

    private var restartScheduled = false

    companion object {
        private const val RESTART_DELAY = 500L
    }

    // =========================================================
    // START
    // =========================================================

    fun start() {

        if (running.get()) {
            return
        }

        running.set(true)

        setState(
            VoiceState.STANDBY
        )
    }

    // =========================================================
    // ENABLE WAKE MODE
    // =========================================================

    fun enableWakeMode() {

        if (!running.get()) {
            start()
        }

        wakeMode.set(true)

        commandMode.set(false)

        stopRecognition()

        setState(
            VoiceState.STANDBY
        )

        /*
         * The actual always-on wake-word engine will be attached
         * to this controller separately.
         *
         * We deliberately do not start Android SpeechRecognizer
         * here as an always-on wake-word detector.
         */
    }

    // =========================================================
    // DISABLE WAKE MODE
    // =========================================================

    fun disableWakeMode() {

        wakeMode.set(false)

        commandMode.set(false)

        cancelRestart()

        stopRecognition()

        setState(
            VoiceState.IDLE
        )
    }

    // =========================================================
    // WAKE DETECTED
    // =========================================================

    fun wakeDetected() {

        if (!running.get()) {
            return
        }

        if (!wakeMode.get()) {
            return
        }

        commandMode.set(true)

        onWakeDetected.invoke()

        startCommandListening()
    }

    // =========================================================
    // START COMMAND LISTENING
    // =========================================================

    private fun startCommandListening() {

        if (!running.get()) {
            return
        }

        if (!wakeMode.get()) {
            return
        }

        if (!commandMode.get()) {
            return
        }

        cancelRestart()

        setState(
            VoiceState.LISTENING
        )

        val recognizer =
            getRecognizer()

        recognizer.start(

            { text ->

                if (!running.get()) {
                    return@start
                }

                if (!wakeMode.get()) {
                    return@start
                }

                if (!commandMode.get()) {
                    return@start
                }

                val cleanText =
                    text
                        .trim()

                if (cleanText.isBlank()) {

                    restartCommandListening()

                    return@start
                }

                stopRecognition()

                setState(
                    VoiceState.THINKING
                )

                onCommand.invoke(
                    cleanText
                )
            },

            { error ->

                if (!running.get()) {
                    return@start
                }

                if (!wakeMode.get()) {
                    return@start
                }

                onError.invoke(
                    error
                )

                restartCommandListening()
            }
        )
    }

    // =========================================================
    // PROCESSING
    // =========================================================

    fun setProcessing() {

        if (!running.get()) {
            return
        }

        setState(
            VoiceState.THINKING
        )

        stopRecognition()
    }

    // =========================================================
    // EXECUTING
    // =========================================================

    fun setExecuting() {

        if (!running.get()) {
            return
        }

        setState(
            VoiceState.EXECUTING
        )
    }

    // =========================================================
    // SPEAKING
    // =========================================================

    fun setSpeaking() {

        if (!running.get()) {
            return
        }

        stopRecognition()

        setState(
            VoiceState.SPEAKING
        )
    }

    // =========================================================
    // SPEECH FINISHED
    // =========================================================

    fun speechFinished() {

        if (!running.get()) {
            return
        }

        if (!wakeMode.get()) {
            return
        }

        /*
         * After JARVIS finishes speaking, return to standby.
         *
         * We don't immediately start command recognition here.
         * The wake-word engine should hear "Hey Jarvis" again.
         */
        commandMode.set(false)

        setState(
            VoiceState.STANDBY
        )
    }

    // =========================================================
    // MANUAL COMMAND MODE
    // =========================================================

    fun startCommandMode() {

        if (!running.get()) {
            start()
        }

        commandMode.set(true)

        startCommandListening()
    }

    // =========================================================
    // STOP
    // =========================================================

    fun stop() {

        running.set(false)

        wakeMode.set(false)

        commandMode.set(false)

        cancelRestart()

        stopRecognition()

        setState(
            VoiceState.IDLE
        )
    }

    // =========================================================
    // RESTART LISTENING
    // =========================================================

    private fun restartCommandListening() {

        if (!running.get()) {
            return
        }

        if (!wakeMode.get()) {
            return
        }

        if (!commandMode.get()) {
            return
        }

        if (restartScheduled) {
            return
        }

        restartScheduled = true

        handler.postDelayed({

            restartScheduled = false

            if (!running.get()) {
                return@postDelayed
            }

            if (!wakeMode.get()) {
                return@postDelayed
            }

            if (!commandMode.get()) {
                return@postDelayed
            }

            startCommandListening()

        }, RESTART_DELAY)
    }

    // =========================================================
    // RECOGNIZER
    // =========================================================

    private fun getRecognizer():
        SpeechRecognizerManager {

        val existing =
            speechRecognizer

        if (existing != null) {
            return existing
        }

        val created =
            SpeechRecognizerManager(
                context
            )

        speechRecognizer =
            created

        return created
    }

    // =========================================================
    // STOP RECOGNITION
    // =========================================================

    private fun stopRecognition() {

        try {
            speechRecognizer?.stop()
        } catch (_: Exception) {
        }
    }

    // =========================================================
    // CANCEL RESTART
    // =========================================================

    private fun cancelRestart() {

        restartScheduled = false

        handler.removeCallbacksAndMessages(
            null
        )
    }

    // =========================================================
    // STATE
    // =========================================================

    private fun setState(
        state: VoiceState
    ) {

        handler.post {

            onStateChanged.invoke(
                state
            )
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    fun isRunning(): Boolean {
        return running.get()
    }

    fun isWakeModeEnabled(): Boolean {
        return wakeMode.get()
    }

    fun isCommandModeActive(): Boolean {
        return commandMode.get()
    }

    fun destroy() {

        running.set(false)

        wakeMode.set(false)

        commandMode.set(false)

        cancelRestart()

        stopRecognition()

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        speechRecognizer = null

        setState(
            VoiceState.IDLE
        )
    }
}