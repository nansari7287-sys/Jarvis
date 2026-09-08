package com.example.jarvis.voice

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import org.tensorflow.lite.Interpreter

class WakeWordEngine(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onError: (String) -> Unit = {}
) {

    private val running = AtomicBoolean(false)

    private var interpreter: Interpreter? = null
    private var modelFile: File? = null

    companion object {
        private const val MODEL_NAME = "hey_jarvis_v0.1.tflite"
    }

    fun start() {
        if (running.get()) return

        try {
            loadModel()
            running.set(true)
        } catch (e: Exception) {
            running.set(false)
            onError("Wake word engine start failed.")
        }
    }

    fun stop() {
        running.set(false)
    }

    fun isRunning(): Boolean {
        return running.get()
    }

    /**
     * Sends one audio frame to the wake-word model.
     *
     * This method is intentionally kept separate from microphone capture.
     * AudioCaptureManager will provide PCM audio frames later.
     */
    fun processAudio(pcmData: FloatArray) {
        if (!running.get()) return

        val model = interpreter ?: return

        try {
            /*
             * The exact input/output tensor shape of the supplied
             * wake-word model must be verified before inference.
             *
             * We therefore do not make assumptions about the model
             * tensor shape here.
             */
            val inputTensor = model.getInputTensor(0)
            val inputShape = inputTensor.shape()

            if (inputShape.isEmpty()) return

            // Actual model-specific preprocessing/inference will be
            // connected after the model tensor specification is verified.
        } catch (e: Exception) {
            onError("Wake word processing failed.")
        }
    }

    private fun loadModel() {
        val assetManager = context.assets

        val tempFile = File(
            context.cacheDir,
            MODEL_NAME
        )

        if (!tempFile.exists()) {
            assetManager.open(MODEL_NAME).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        modelFile = tempFile

        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }

        interpreter = Interpreter(
            tempFile,
            options
        )
    }

    fun release() {
        running.set(false)

        try {
            interpreter?.close()
        } catch (_: Exception) {
        }

        interpreter = null

        try {
            modelFile?.delete()
        } catch (_: Exception) {
        }

        modelFile = null
    }
}