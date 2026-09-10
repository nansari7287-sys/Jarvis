package com.example.jarvis.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Controls JARVIS background voice interaction.
 *
 * Flow:
 * STANDBY
 *   ↓ Hey Jarvis
 * LISTENING
 *   ↓ command
 * THINKING
 *   ↓
 * EXECUTING / SPEAKING
 *   ↓
 * LISTENING again for continuous conversation
 *
 * Wake-word detection itself is handled separately by VoiceService.
 */
class BackgroundVoiceController(
    private val context: Context,
    private val onStateChanged: (VoiceState) -> Unit,
    private val onWakeDetected: () -> Unit,
    private val onCommand: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())

    private val running = AtomicBoolean(false)
    private val wakeMode = AtomicBoolean(false)
    private val commandMode = AtomicBoolean(false)

    private var speechRecognizer: SpeechRecognizerManager? = null

    private var restartScheduled = false
    private var waitingForResponse = false

    companion object {
        private const val RESTART_DELAY = 350L
    }

    // =========================================================
    // START
    // =========================================================

    fun start() {
        if (running.get()) return

        running.set(true)
        waitingForResponse = false

        setState(VoiceState.STANDBY)
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
        waitingForResponse = false

        cancelRestart()
        stopRecognition()

        setState(VoiceState.STANDBY)
    }

    // =========================================================
    // DISABLE WAKE MODE
    // =========================================================

    fun disableWakeMode() {

        wakeMode.set(false)
        commandMode.set(false)
        waitingForResponse = false

        cancelRestart()
        stopRecognition()

        setState(VoiceState.IDLE)
    }

    // =========================================================
    // WAKE DETECTED
    // =========================================================

    fun wakeDetected() {

        if (!running.get()) return
        if (!wakeMode.get()) return

        commandMode.set(true)
        waitingForResponse = false

        cancelRestart()
        stopRecognition()

        onWakeDetected.invoke()

        startCommandListening()
    }

    // =========================================================
    // START COMMAND LISTENING
    // =========================================================

    private fun startCommandListening() {

        if (!running.get()) return
        if (!wakeMode.get()) return
        if (!commandMode.get()) return
        if (waitingForResponse) return

        cancelRestart()

        setState(VoiceState.LISTENING)

        val recognizer = getRecognizer()

        recognizer.start(

            { text ->

                if (!running.get()) return@start
                if (!wakeMode.get()) return@start
                if (!commandMode.get()) return@start
                if (waitingForResponse) return@start

                val cleanText = text.trim()

                if (cleanText.isBlank()) {
                    restartCommandListening()
                    return@start
                }

                /*
                 * Prevent duplicate callbacks while the current
                 * command is being processed.
                 */
                waitingForResponse = true

                stopRecognition()

                setState(VoiceState.THINKING)

                onCommand.invoke(cleanText)
            },

            { error ->

                if (!running.get()) return@start
                if (!wakeMode.get()) return@start
                if (!commandMode.get()) return@start
                if (waitingForResponse) return@start

                /*
                 * Don't permanently kill the voice session because
                 * SpeechRecognizer occasionally reports transient
                 * errors such as timeout/no-match.
                 */
                onError.invoke(error)

                restartCommandListening()
            }
        )
    }

    // =========================================================
    // PROCESSING
    // =========================================================

    fun setProcessing() {

        if (!running.get()) return

        waitingForResponse = true

        stopRecognition()

        setState(VoiceState.THINKING)
    }

    // =========================================================
    // EXECUTING
    // =========================================================

    fun setExecuting() {

        if (!running.get()) return

        waitingForResponse = true

        setState(VoiceState.EXECUTING)
    }

    // =========================================================
    // SPEAKING
    // =========================================================

    fun setSpeaking() {

        if (!running.get()) return

        waitingForResponse = true

        stopRecognition()

        setState(VoiceState.SPEAKING)
    }

    // =========================================================
    // SPEECH FINISHED
    // =========================================================

    fun speechFinished() {

        if (!running.get()) return
        if (!wakeMode.get()) return
        if (!commandMode.get()) return

        /*
         * IMPORTANT:
         *
         * Do NOT return to permanent STANDBY here.
         *
         * After JARVIS speaks, continue listening so the user
         * can naturally say:
         *
         * "haan"
         * "accha Instagram kholo"
         * "volume badhao"
         * "nahi, doosra wala"
         *
         * without pressing the microphone again.
         */
        waitingForResponse = false

        startCommandListening()
    }

    // =========================================================
    // RESUME LISTENING
    // =========================================================

    fun resumeListening() {

        if (!running.get()) return
        if (!wakeMode.get()) return
        if (!commandMode.get()) return

        waitingForResponse = false

        startCommandListening()
    }

    // =========================================================
    // FORCE LISTENING
    // =========================================================

    fun forceListening() {

        if (!running.get()) return
        if (!wakeMode.get()) return

        commandMode.set(true)
        waitingForResponse = false

        cancelRestart()
        stopRecognition()

        startCommandListening()
    }

    // =========================================================
    // MANUAL COMMAND MODE
    // =========================================================

    fun startCommandMode() {

        if (!running.get()) {
            start()
        }

        commandMode.set(true)
        waitingForResponse = false

        startCommandListening()
    }

    // =========================================================
    // COMMAND FINISHED WITHOUT SPEECH
    // =========================================================

    fun commandFinished() {

        if (!running.get()) return
        if (!wakeMode.get()) return
        if (!commandMode.get()) return

        waitingForResponse = false

        startCommandListening()
    }

    // =========================================================
    // RETURN TO STANDBY
    // =========================================================

    fun returnToStandby() {

        if (!running.get()) return
        if (!wakeMode.get()) return

        commandMode.set(false)
        waitingForResponse = false

        cancelRestart()
        stopRecognition()

        setState(VoiceState.STANDBY)
    }

    // =========================================================
    // STOP
    // =========================================================

    fun stop() {

        running.set(false)
        wakeMode.set(false)
        commandMode.set(false)

        waitingForResponse = false

        cancelRestart()
        stopRecognition()

        setState(VoiceState.IDLE)
    }

    // =========================================================
    // RESTART LISTENING
    // =========================================================

    private fun restartCommandListening() {

        if (!running.get()) return
        if (!wakeMode.get()) return
        if (!commandMode.get()) return
        if (waitingForResponse) return
        if (restartScheduled) return

        restartScheduled = true

        handler.postDelayed({

            restartScheduled = false

            if (!running.get()) return@postDelayed
            if (!wakeMode.get()) return@postDelayed
            if (!commandMode.get()) return@postDelayed
            if (waitingForResponse) return@postDelayed

            startCommandListening()

        }, RESTART_DELAY)
    }

    // =========================================================
    // RECOGNIZER
    // =========================================================

    private fun getRecognizer(): SpeechRecognizerManager {

        speechRecognizer?.let {
            return it
        }

        val created = SpeechRecognizerManager(context)

        speechRecognizer = created

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

        handler.removeCallbacksAndMessages(null)
    }

    // =========================================================
    // STATE
    // =========================================================

    private fun setState(state: VoiceState) {

        handler.post {
            onStateChanged.invoke(state)
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

    fun isWaitingForResponse(): Boolean {
        return waitingForResponse
    }

    // =========================================================
    // DESTROY
    // =========================================================

    fun destroy() {

        running.set(false)
        wakeMode.set(false)
        commandMode.set(false)

        waitingForResponse = false

        cancelRestart()
        stopRecognition()

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        speechRecognizer = null

        setState(VoiceState.IDLE)
    }
}