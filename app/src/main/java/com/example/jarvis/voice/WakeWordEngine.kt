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
            onError("Hey Jarvis engine start failed.")
        }
    }

    fun stop() {
        running.set(false)
    }

    fun isRunning(): Boolean = running.get()

    fun processAudio(pcmData: FloatArray) {
        if (!running.get()) return

        val model = interpreter ?: return

        try {
            val inputTensor = model.getInputTensor(0)

            // Model format must be verified before real inference.
            // Do not assume audio tensor shape.
            if (inputTensor.shape().isEmpty()) return

        } catch (_: Exception) {
            onError("Hey Jarvis audio processing failed.")
        }
    }

    private fun loadModel() {
        val file = File(context.cacheDir, MODEL_NAME)

        if (!file.exists()) {
            context.assets.open(MODEL_NAME).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }

        modelFile = file

        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }

        interpreter = Interpreter(file, options)
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