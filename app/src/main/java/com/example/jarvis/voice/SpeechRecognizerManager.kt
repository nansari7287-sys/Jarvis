package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerManager(
    private val context: Context
) {

    private var recognizer: SpeechRecognizer? = null

    private var isListening = false

    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available.")
            return
        }

        stop()

        recognizer = SpeechRecognizer
            .createSpeechRecognizer(context)
            .apply {

                setRecognitionListener(
                    object : RecognitionListener {

                        override fun onReadyForSpeech(
                            params: Bundle?
                        ) {
                            isListening = true
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

                            onError(
                                errorMessage(error)
                            )
                        }

                        override fun onResults(
                            results: Bundle?
                        ) {

                            isListening = false

                            val text = results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()
                                .orEmpty()

                            if (text.isBlank()) {

                                onError(
                                    "I couldn't hear that."
                                )

                            } else {

                                onResult(text)
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

                val intent = Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
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
                }

                startListening(intent)
            }
    }

    fun stop() {

        isListening = false

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

    fun destroy() {

        isListening = false

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