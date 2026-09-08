package com.example.jarvis.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class AudioCaptureManager(
    private val context: Context,
    private val onAudioFrame: (FloatArray) -> Unit,
    private val onError: (String) -> Unit = {}
) {

    private val running = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG =
            AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT =
            AudioFormat.ENCODING_PCM_16BIT

        private const val FRAME_SIZE = 512
    }

    fun start() {
        if (running.get()) return

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onError("Microphone permission is required.")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize <= 0) {
            onError("Unable to initialize microphone.")
            return
        }

        val bufferSize = maxOf(
            minBufferSize,
            FRAME_SIZE * 2
        )

        try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                onError("Microphone initialization failed.")
                return
            }

            audioRecord = recorder
            running.set(true)

            recorder.startRecording()

            captureThread = thread(
                start = true,
                name = "JarvisAudioCapture"
            ) {
                captureLoop(recorder)
            }

        } catch (_: SecurityException) {
            running.set(false)
            onError("Microphone permission is required.")
        } catch (_: Exception) {
            running.set(false)
            onError("Audio capture failed.")
        }
    }

    private fun captureLoop(
        recorder: AudioRecord
    ) {
        val pcmBuffer = ShortArray(FRAME_SIZE)

        while (running.get()) {

            val read = try {
                recorder.read(
                    pcmBuffer,
                    0,
                    pcmBuffer.size,
                    AudioRecord.READ_BLOCKING
                )
            } catch (_: Exception) {
                -1
            }

            if (read <= 0) {
                if (running.get()) {
                    onError("Microphone read failed.")
                }
                break
            }

            val audioFrame = FloatArray(read)

            for (i in 0 until read) {
                audioFrame[i] =
                    pcmBuffer[i] / 32768.0f
            }

            onAudioFrame(audioFrame)
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return

        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }

        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }

        audioRecord = null
        captureThread = null
    }

    fun isRunning(): Boolean {
        return running.get()
    }

    fun release() {
        stop()
    }
}