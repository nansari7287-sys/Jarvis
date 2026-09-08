package com.example.jarvis.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(
    context: Context
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(
        context.applicationContext,
        this
    )

    private val mainHandler = Handler(
        Looper.getMainLooper()
    )

    private var ready = false
    private var speaking = false

    private var onSpeakStarted: (() -> Unit)? = null
    private var onSpeakFinished: (() -> Unit)? = null

    override fun onInit(status: Int) {

        ready = status == TextToSpeech.SUCCESS

        if (!ready) {
            return
        }

        /*
         * Hindi is the primary language.
         */
        val hindiResult = tts.setLanguage(
            Locale("hi", "IN")
        )

        /*
         * If Hindi voice data is unavailable,
         * fall back to English.
         */
        if (
            hindiResult == TextToSpeech.LANG_MISSING_DATA ||
            hindiResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            tts.setLanguage(Locale.US)
        }

        tts.setSpeechRate(1.0f)
        tts.setPitch(1.0f)

        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(
                    utteranceId: String?
                ) {

                    speaking = true

                    mainHandler.post {
                        onSpeakStarted?.invoke()
                    }
                }

                override fun onDone(
                    utteranceId: String?
                ) {

                    speaking = false

                    mainHandler.post {
                        val callback =
                            onSpeakFinished

                        onSpeakStarted = null
                        onSpeakFinished = null

                        callback?.invoke()
                    }
                }

                override fun onError(
                    utteranceId: String?
                ) {

                    speaking = false

                    mainHandler.post {
                        val callback =
                            onSpeakFinished

                        onSpeakStarted = null
                        onSpeakFinished = null

                        callback?.invoke()
                    }
                }
            }
        )
    }

    fun speak(
        text: String,
        onStarted: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {

        val cleanText = text
            .trim()

        if (cleanText.isBlank()) {
            onFinished?.invoke()
            return
        }

        if (!ready) {
            onFinished?.invoke()
            return
        }

        /*
         * Stop any previous response first.
         */
        try {
            tts.stop()
        } catch (_: Exception) {
        }

        speaking = false

        onSpeakStarted = onStarted
        onSpeakFinished = onFinished

        val result = tts.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_response_${System.currentTimeMillis()}"
        )

        if (result == TextToSpeech.ERROR) {

            speaking = false

            val callback =
                onSpeakFinished

            onSpeakStarted = null
            onSpeakFinished = null

            callback?.invoke()
        }
    }

    fun stop() {

        if (!ready) {
            return
        }

        try {
            tts.stop()
        } catch (_: Exception) {
        }

        speaking = false

        onSpeakStarted = null
        onSpeakFinished = null
    }

    fun isSpeaking(): Boolean {
        return ready && (
            speaking || tts.isSpeaking
        )
    }

    fun isReady(): Boolean {
        return ready
    }

    fun shutdown() {

        speaking = false

        onSpeakStarted = null
        onSpeakFinished = null

        mainHandler.removeCallbacksAndMessages(null)

        try {
            tts.stop()
        } catch (_: Exception) {
        }

        try {
            tts.shutdown()
        } catch (_: Exception) {
        }

        ready = false
    }
}