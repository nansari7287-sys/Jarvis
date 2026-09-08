package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context
) {

    private var recognizer: SpeechRecognizer? = null

    private var isListening = false
    private var sessionActive = false
    private var resultDelivered = false

    private var currentOnResult: ((String) -> Unit)? = null
    private var currentOnError: ((String) -> Unit)? = null

    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available.")
            return
        }

        currentOnResult = onResult
        currentOnError = onError

        sessionActive = true
        resultDelivered = false

        if (recognizer == null) {
            createRecognizer()
        }

        startListening()
    }

    private fun createRecognizer() {

        recognizer = SpeechRecognizer
            .createSpeechRecognizer(context)
            .apply {

                setRecognitionListener(
                    object : RecognitionListener {

                        override fun onReadyForSpeech(
                            params: Bundle?
                        ) {
                            isListening = true
                            resultDelivered = false
                        }

                        override fun onBeginningOfSpeech() {
                        }

                        override fun onRmsChanged(
                            rmsdB: Float
                        ) {
                        }

                        override fun onBufferReceived(
                            buffer: ByteArray?
                        ) {
                        }

                        override fun onEndOfSpeech() {
                            isListening = false
                        }

                        override fun onError(
                            error: Int
                        ) {

                            isListening = false

                            if (!sessionActive) {
                                return
                            }

                            /*
                             * NO_MATCH and SPEECH_TIMEOUT are normal
                             * during continuous listening.
                             *
                             * The VoiceSessionManager decides whether
                             * listening should start again.
                             */
                            currentOnError?.invoke(
                                errorMessage(error)
                            )
                        }

                        override fun onResults(
                            results: Bundle?
                        ) {

                            isListening = false

                            if (!sessionActive) {
                                return
                            }

                            if (resultDelivered) {
                                return
                            }

                            val text = results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()
                                .orEmpty()
                                .trim()

                            resultDelivered = true

                            if (text.isBlank()) {

                                currentOnError?.invoke(
                                    "I couldn't hear that."
                                )

                            } else {

                                currentOnResult?.invoke(text)
                            }
                        }

                        override fun onPartialResults(
                            partialResults: Bundle?
                        ) {
                        }

                        override fun onEvent(
                            eventType: Int,
                            params: Bundle?
                        ) {
                        }
                    }
                )
            }
    }

    private fun startListening() {

        if (!sessionActive) {
            return
        }

        if (isListening) {
            return
        }

        resultDelivered = false

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            /*
             * Hindi-first.
             *
             * Android speech services can still understand
             * Hinglish/English depending on the installed
             * recognition provider.
             */
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "hi-IN"
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "hi-IN"
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1200L
            )

            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                700L
            )

            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                500L
            )
        }

        try {

            recognizer?.startListening(intent)

        } catch (e: Exception) {

            isListening = false

            if (sessionActive) {
                currentOnError?.invoke(
                    "Speech recognition start failed."
                )
            }
        }
    }

    /**
     * Start another recognition cycle without destroying
     * the SpeechRecognizer object.
     *
     * This is used by continuous voice mode.
     */
    fun restart() {

        if (!sessionActive) {
            return
        }

        if (isListening) {
            return
        }

        startListening()
    }

    fun stop() {

        sessionActive = false
        isListening = false
        resultDelivered = false

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }

        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
    }

    fun isListening(): Boolean {
        return isListening
    }

    fun isSessionActive(): Boolean {
        return sessionActive
    }

    fun destroy() {

        sessionActive = false
        isListening = false
        resultDelivered = false

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }

        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }

        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null

        currentOnResult = null
        currentOnError = null
    }

    private fun errorMessage(
        error: Int
    ): String {

        return when (error) {

            SpeechRecognizer.ERROR_AUDIO ->
                "Audio recording error."

            SpeechRecognizer.ERROR_CLIENT ->
                "Speech recognition client error."

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is required."

            SpeechRecognizer.ERROR_NETWORK ->
                "Network error during speech recognition."

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Speech recognition network timeout."

            SpeechRecognizer.ERROR_NO_MATCH ->
                "I couldn't understand that."

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Speech recognizer is busy."

            SpeechRecognizer.ERROR_SERVER ->
                "Speech recognition server error."

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "I didn't hear anything."

            else ->
                "Speech recognition error."
        }
    }
}