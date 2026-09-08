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

    fun start() {

        if (active.get()) {
            return
        }

        active.set(true)

        setState(State.LISTENING)

        listen()
    }

    fun stop() {

        active.set(false)

        try {
            speechRecognizer.stop()
        } catch (_: Exception) {
        }

        setState(State.IDLE)
    }

    fun isActive(): Boolean {
        return active.get()
    }

    fun setProcessing() {

        if (!active.get()) {
            return
        }

        setState(State.PROCESSING)
    }

    fun setSpeaking() {

        if (!active.get()) {
            return
        }

        setState(State.SPEAKING)
    }

    fun resumeListening() {

        if (!active.get()) {
            return
        }

        handler.postDelayed({

            if (active.get()) {

                setState(State.LISTENING)

                listen()
            }

        }, 350)
    }

    private fun listen() {

        if (!active.get()) {
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

                    resumeListening()

                    return@start
                }

                onText(cleanText)
            },

            { error ->

                if (!active.get()) {
                    return@start
                }

                onError(error)

                resumeListening()
            }
        )
    }

    private fun setState(
        newState: State
    ) {

        state = newState

        handler.post {

            onStateChanged(newState)
        }
    }

    fun destroy() {

        active.set(false)

        handler.removeCallbacksAndMessages(null)

        try {
            speechRecognizer.stop()
        } catch (_: Exception) {
        }

        setState(State.IDLE)
    }
}
