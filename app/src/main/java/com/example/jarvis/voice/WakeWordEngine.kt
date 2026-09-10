package com.example.jarvis.voice

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Real Hey Jarvis wake-word classifier.
 *
 * Pipeline:
 *
 * AudioCaptureManager
 *        ↓
 * AudioFeatureExtractor
 *        ↓
 * [16, 96]
 *        ↓
 * hey_jarvis_v0.1.tflite
 *        ↓
 * confidence score
 *        ↓
 * Hey Jarvis detected
 */
class WakeWordEngine(
    context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onError: (String) -> Unit = {}
) {

    companion object {

        private const val MODEL_NAME =
            "hey_jarvis_v0.1.tflite"

        /*
         * The wake-word model outputs [1, 1].
         *
         * 0.50 is the initial detection threshold.
         * We can tune this later after real-device testing.
         */
        private const val DETECTION_THRESHOLD =
            0.50f

        /*
         * Prevent multiple detections from one utterance.
         */
        private const val DETECTION_COOLDOWN_MS =
            2_000L
    }

    private val featureExtractor =
        AudioFeatureExtractor(context)

    private val interpreter: Interpreter

    @Volatile
    private var running = false

    @Volatile
    private var enabled = false

    private var lastDetectionTime = 0L

    init {

        try {

            interpreter = Interpreter(
                loadModel(
                    context,
                    MODEL_NAME
                ),
                Interpreter.Options().apply {
                    setNumThreads(2)
                }
            )

            interpreter.allocateTensors()

            validateModel()

        } catch (e: Exception) {

            throw IllegalStateException(
                "Hey Jarvis model initialization failed: " +
                    e.message,
                e
            )
        }
    }

    /**
     * Enable wake-word detection.
     */
    @Synchronized
    fun start() {

        if (running) {
            return
        }

        running = true
        enabled = true

        featureExtractor.reset()

        lastDetectionTime = 0L
    }

    /**
     * Temporarily disable detection.
     */
    @Synchronized
    fun stop() {

        running = false
        enabled = false

        featureExtractor.reset()
    }

    /**
     * Enable/disable without destroying the model.
     */
    @Synchronized
    fun setEnabled(value: Boolean) {

        enabled = value

        if (!value) {
            featureExtractor.reset()
        }
    }

    fun isRunning(): Boolean {
        return running
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    /**
     * Feed normalized microphone audio.
     *
     * AudioCaptureManager currently produces:
     *
     * FloatArray [-1.0 .. +1.0]
     */
    fun processAudio(
        audio: FloatArray
    ) {

        if (!running || !enabled) {
            return
        }

        if (audio.isEmpty()) {
            return
        }

        try {

            val featureWindows =
                featureExtractor.processAudio(
                    audio
                )

            for (window in featureWindows) {

                if (!running || !enabled) {
                    return
                }

                val score =
                    runWakeWordModel(
                        window
                    )

                if (score >= DETECTION_THRESHOLD) {

                    handleDetection(score)

                    /*
                     * Do not process additional windows
                     * from the same audio batch after detection.
                     */
                    return
                }
            }

        } catch (e: Exception) {

            onError(
                "Wake word processing failed: " +
                    (e.message ?: "unknown error")
            )
        }
    }

    /**
     * Feed native 16-bit PCM.
     */
    fun processPcm(
        pcm: ShortArray
    ) {

        if (!running || !enabled) {
            return
        }

        if (pcm.isEmpty()) {
            return
        }

        try {

            val featureWindows =
                featureExtractor.processPcm(
                    pcm
                )

            for (window in featureWindows) {

                if (!running || !enabled) {
                    return
                }

                val score =
                    runWakeWordModel(
                        window
                    )

                if (score >= DETECTION_THRESHOLD) {

                    handleDetection(score)

                    return
                }
            }

        } catch (e: Exception) {

            onError(
                "Wake word processing failed: " +
                    (e.message ?: "unknown error")
            )
        }
    }

    /**
     * Run hey_jarvis_v0.1.tflite.
     *
     * Model input:
     *
     * [1, 16, 96]
     *
     * Model output:
     *
     * [1, 1]
     */
    private fun runWakeWordModel(
        featureWindow: Array<FloatArray>
    ): Float {

        if (
            featureWindow.size != 16
        ) {
            throw IllegalArgumentException(
                "Invalid wake-word feature window. " +
                    "Expected 16 rows, got " +
                    featureWindow.size
            )
        }

        for (row in featureWindow) {

            if (row.size != 96) {

                throw IllegalArgumentException(
                    "Invalid wake-word embedding size. " +
                        "Expected 96, got " +
                        row.size
                )
            }
        }

        /*
         * TFLite input:
         *
         * [1, 16, 96]
         */
        val input =
            Array(1) {
                featureWindow
            }

        /*
         * TFLite output:
         *
         * [1, 1]
         */
        val output =
            Array(1) {
                FloatArray(1)
            }

        synchronized(interpreter) {

            interpreter.run(
                input,
                output
            )
        }

        return output[0][0]
    }

    /**
     * Handle a successful detection.
     */
    private fun handleDetection(
        score: Float
    ) {

        val now =
            System.currentTimeMillis()

        /*
         * Cooldown prevents duplicate detections.
         */
        if (
            now - lastDetectionTime <
            DETECTION_COOLDOWN_MS
        ) {
            return
        }

        lastDetectionTime = now

        /*
         * Pause classification until the voice service
         * decides what happens next.
         */
        featureExtractor.reset()

        onWakeWordDetected()
    }

    /**
     * Verify that the supplied model is the expected
     * [1,16,96] -> [1,1] classifier.
     */
    private fun validateModel() {

        val inputShape =
            interpreter
                .getInputTensor(0)
                .shape()

        val outputShape =
            interpreter
                .getOutputTensor(0)
                .shape()

        val validInput =
            inputShape.size == 3 &&
                inputShape[0] == 1 &&
                inputShape[1] == 16 &&
                inputShape[2] == 96

        val validOutput =
            outputShape.size == 2 &&
                outputShape[0] == 1 &&
                outputShape[1] == 1

        if (!validInput) {

            throw IllegalStateException(
                "Invalid Hey Jarvis input shape: " +
                    inputShape.contentToString() +
                    ". Expected [1, 16, 96]."
            )
        }

        if (!validOutput) {

            throw IllegalStateException(
                "Invalid Hey Jarvis output shape: " +
                    outputShape.contentToString() +
                    ". Expected [1, 1]."
            )
        }
    }

    /**
     * Release all resources.
     */
    @Synchronized
    fun release() {

        running = false
        enabled = false

        featureExtractor.close()
        interpreter.close()
    }

    /**
     * Load model from assets.
     */
    private fun loadModel(
        context: Context,
        fileName: String
    ): MappedByteBuffer {

        val descriptor =
            context.assets.openFd(
                fileName
            )

        FileInputStream(
            descriptor.fileDescriptor
        ).use { inputStream ->

            val channel =
                inputStream.channel

            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }
}