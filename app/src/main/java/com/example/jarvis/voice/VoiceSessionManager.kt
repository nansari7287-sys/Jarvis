package com.example.jarvis.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

class VoiceSessionManager(
    private val context: Context,
    private val speechRecognizer: SpeechRecognizerManager,
    private val onText: (String) -> Unit,
    private val onStateChanged: (State) -> Unit,
    private val onError: (String) -> Unit
) {

    enum class State {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING
    }

    private val handler = Handler(Looper.getMainLooper())

    private val active = AtomicBoolean(false)

    private var state = State.IDLE

    private var restartScheduled = false

    companion object {
        private const val LISTEN_RESTART_DELAY = 450L
    }

    /**
     * Start continuous voice conversation.
     *
     * Once started, JARVIS keeps listening automatically
     * after every completed command.
     */
    fun start() {

        if (active.get()) {
            return
        }

        active.set(true)
        restartScheduled = false

        setState(State.LISTENING)

        startListening()
    }

    /**
     * Completely stop the voice session.
     */
    fun stop() {

        active.set(false)
        restartScheduled = false

        handler.removeCallbacksAndMessages(null)

        try {
            speechRecognizer.stop()
        } catch (_: Exception) {
        }

        setState(State.IDLE)
    }

    fun isActive(): Boolean {
        return active.get()
    }

    fun getState(): State {
        return state
    }

    /**
     * Called when JARVIS is processing a command.
     */
    fun setProcessing() {

        if (!active.get()) {
            return
        }

        cancelPendingRestart()

        try {
            speechRecognizer.stop()
        } catch (_: Exception) {
        }

        setState(State.PROCESSING)
    }

    /**
     * Called when JARVIS starts speaking.
     */
    fun setSpeaking() {

        if (!active.get()) {
            return
        }

        cancelPendingRestart()

        setState(State.SPEAKING)
    }

    /**
     * Called after TTS finishes speaking.
     *
     * JARVIS automatically returns to listening mode.
     */
    fun resumeListening() {

        if (!active.get()) {
            return
        }

        scheduleListeningRestart()
    }

    /**
     * Start listening again after a short delay.
     *
     * The delay prevents SpeechRecognizer from being restarted
     * while Android is still releasing the previous recognition session.
     */
    private fun scheduleListeningRestart() {

        if (!active.get()) {
            return
        }

        if (restartScheduled) {
            return
        }

        restartScheduled = true

        handler.postDelayed({

            restartScheduled = false

            if (!active.get()) {
                return@postDelayed
            }

            startListening()

        }, LISTEN_RESTART_DELAY)
    }

    /**
     * Start one speech-recognition cycle.
     */
    private fun startListening() {

        if (!active.get()) {
            return
        }

        if (speechRecognizer.isListening()) {
            return
        }

        setState(State.LISTENING)

        speechRecognizer.start(

            { text ->

                if (!active.get()) {
                    return@start
                }

                val cleanText = text.trim()

                if (cleanText.isBlank()) {
                    scheduleListeningRestart()
                    return@start
                }

                /*
                 * Stop this recognition cycle before handing
                 * the command to JARVIS.
                 */
                try {
                    speechRecognizer.stop()
                } catch (_: Exception) {
                }

                setState(State.PROCESSING)

                onText(cleanText)
            },

            { error ->

                if (!active.get()) {
                    return@start
                }

                onError(error)

                /*
                 * Most SpeechRecognizer errors are recoverable.
                 * Automatically start another recognition cycle.
                 */
                scheduleListeningRestart()
            }
        )
    }

    /**
     * Manually force listening mode.
     */
    fun forceListening() {

        if (!active.get()) {
            return
        }

        cancelPendingRestart()

        try {
            speechRecognizer.stop()
        } catch (_: Exception) {
        }

        handler.post {

            if (!active.get()) {
                return@post
            }

            startListening()
        }
    }

    /**
     * Cancel an already scheduled recognition restart.
     */
    private fun cancelPendingRestart() {

        restartScheduled = false

        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Update UI state safely on the main thread.
     */
    private fun setState(newState: State) {

        state = newState

        handler.post {
            onStateChanged(newState)
        }
    }

    /**
     * Destroy the voice session permanently.
     */
    fun destroy() {

        active.set(false)
        restartScheduled = false

        handler.removeCallbacksAndMessages(null)

        try {
            speechRecognizer.stop()
        } catch (_: Exception) {
        }

        state = State.IDLE

        handler.post {
            onStateChanged(State.IDLE)
        }
    }
}