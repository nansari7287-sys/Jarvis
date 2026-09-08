package com.example.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechManager(
    context: Context
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(
        context.applicationContext,
        this
    )

    private var ready = false

    private var onSpeakStarted: (() -> Unit)? = null
    private var onSpeakFinished: (() -> Unit)? = null

    override fun onInit(status: Int) {

        ready = status == TextToSpeech.SUCCESS

        if (!ready) {
            return
        }

        val hindiResult = tts.setLanguage(Locale("hi", "IN"))

        if (
            hindiResult == TextToSpeech.LANG_MISSING_DATA ||
            hindiResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            tts.language = Locale.US
        }

        tts.setSpeechRate(1.0f)
        tts.setPitch(1.0f)

        tts.setOnUtteranceProgressListener(
            object : android.speech.tts.UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    onSpeakStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakFinished?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    onSpeakFinished?.invoke()
                }
            }
        )
    }

    fun speak(
        text: String,
        onStarted: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {

        val cleanText = text.trim()

        if (cleanText.isBlank()) {
            onFinished?.invoke()
            return
        }

        if (!ready) {
            onFinished?.invoke()
            return
        }

        onSpeakStarted = onStarted
        onSpeakFinished = onFinished

        tts.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_response"
        )
    }

    fun stop() {
        if (ready) {
            tts.stop()
        }
    }

    fun isSpeaking(): Boolean {
        return ready && tts.isSpeaking
    }

    fun shutdown() {
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