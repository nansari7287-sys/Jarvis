package com.example.jarvis.voice

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayDeque
import kotlin.math.ceil

/**
 * openWakeWord audio feature pipeline.
 *
 * 16 kHz / 16-bit PCM
 *        ↓
 * melspectrogram.tflite
 *        ↓
 * 32-bin mel features
 *        ↓
 * 76 x 32 windows
 *        ↓
 * embedding_model.tflite
 *        ↓
 * 96-D embeddings
 *        ↓
 * 16 x 96 wake-word feature window
 */
class AudioFeatureExtractor(
    context: Context
) {

    companion object {
        private const val SAMPLE_RATE = 16_000

        // openWakeWord streaming step.
        private const val STEP_SAMPLES = 1_280

        // Embedding model input.
        private const val MEL_WINDOW_SIZE = 76
        private const val MEL_FEATURES = 32

        // Wake-word classifier input.
        private const val FEATURE_WINDOW_SIZE = 16
        private const val EMBEDDING_SIZE = 96

        private const val MEL_MODEL_NAME =
            "melspectrogram.tflite"

        private const val EMBEDDING_MODEL_NAME =
            "embedding_model.tflite"
    }

    private val melInterpreter: Interpreter
    private val embeddingInterpreter: Interpreter

    /**
     * Maximum 10 seconds of raw audio history.
     */
    private val rawDataBuffer =
        ArrayDeque<Short>(SAMPLE_RATE * 10)

    /**
     * Streaming mel-spectrogram history.
     *
     * Official openWakeWord initializes this with
     * 76 x 32 values before real audio arrives.
     */
    private var melSpectrogramBuffer =
        createInitialMelBuffer()

    /**
     * Recent 96-D embeddings.
     */
    private val featureBuffer =
        ArrayDeque<FloatArray>()

    /**
     * Number of samples received since the last
     * feature extraction step.
     */
    private var accumulatedSamples = 0

    init {
        melInterpreter = Interpreter(
            loadModel(
                context,
                MEL_MODEL_NAME
            ),
            Interpreter.Options().apply {
                setNumThreads(2)
            }
        )

        // The official TFLite pipeline starts the mel model
        // with a 1280-sample input.
        melInterpreter.resizeInput(
            0,
            intArrayOf(1, STEP_SAMPLES)
        )
        melInterpreter.allocateTensors()

        embeddingInterpreter = Interpreter(
            loadModel(
                context,
                EMBEDDING_MODEL_NAME
            ),
            Interpreter.Options().apply {
                setNumThreads(2)
            }
        )

        embeddingInterpreter.allocateTensors()
    }

    /**
     * Feed normalized Float audio [-1, +1].
     *
     * This method is useful with AudioCaptureManager.
     */
    @Synchronized
    fun processAudio(
        audio: FloatArray
    ): List<Array<FloatArray>> {

        if (audio.isEmpty()) {
            return emptyList()
        }

        val pcm = ShortArray(audio.size)

        for (i in audio.indices) {
            val sample = audio[i]
                .coerceIn(-1.0f, 1.0f)

            pcm[i] = (
                sample * 32767.0f
            ).toInt()
                .coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                )
                .toShort()
        }

        return processPcm(pcm)
    }

    /**
     * Feed native 16-bit PCM audio.
     *
     * Returns zero or more [16 x 96] feature windows.
     */
    @Synchronized
    fun processPcm(
        pcm: ShortArray
    ): List<Array<FloatArray>> {

        if (pcm.isEmpty()) {
            return emptyList()
        }

        /*
         * Add incoming audio to the streaming buffer.
         */
        for (sample in pcm) {
            rawDataBuffer.addLast(sample)
            accumulatedSamples++
        }

        /*
         * Keep at most 10 seconds of raw audio.
         */
        while (
            rawDataBuffer.size >
            SAMPLE_RATE * 10
        ) {
            rawDataBuffer.removeFirst()
        }

        val results =
            mutableListOf<Array<FloatArray>>()

        /*
         * Only process when at least one complete
         * 80 ms / 1280-sample block is available.
         */
        while (accumulatedSamples >= STEP_SAMPLES) {

            updateStreamingMelSpectrogram()

            val embeddings =
                createEmbeddingsFromLatestMelData()

            for (embedding in embeddings) {

                featureBuffer.addLast(
                    embedding
                )

                /*
                 * Keep roughly 10 seconds of feature history.
                 */
                while (
                    featureBuffer.size >
                    120
                ) {
                    featureBuffer.removeFirst()
                }

                /*
                 * The Jarvis wake-word model expects:
                 *
                 * [1, 16, 96]
                 *
                 * We return the inner [16, 96] array.
                 */
                if (
                    featureBuffer.size >=
                    FEATURE_WINDOW_SIZE
                ) {
                    results.add(
                        getLatestFeatureWindow()
                    )
                }
            }

            accumulatedSamples -= STEP_SAMPLES
        }

        return results
    }

    /**
     * Computes new mel-spectrogram frames using the
     * same streaming strategy as openWakeWord.
     */
    private fun updateStreamingMelSpectrogram() {

        if (rawDataBuffer.size < 400) {
            return
        }

        /*
         * openWakeWord includes 3 x 160 samples of
         * additional context around the current chunk.
         */
        val contextSamples =
            STEP_SAMPLES + (160 * 3)

        val available =
            minOf(
                rawDataBuffer.size,
                contextSamples
            )

        val samples =
            ShortArray(available)

        val skip =
            rawDataBuffer.size - available

        var outputIndex = 0

        for ((index, sample) in
            rawDataBuffer.withIndex()
        ) {

            if (index >= skip) {
                samples[outputIndex++] =
                    sample
            }
        }

        val melOutput =
            runMelModel(samples)

        /*
         * openWakeWord uses:
         *
         * spec = spec / 10 + 2
         */
        for (frame in melOutput) {

            val transformed =
                FloatArray(MEL_FEATURES)

            for (i in 0 until MEL_FEATURES) {
                transformed[i] =
                    frame[i] / 10.0f + 2.0f
            }

            melSpectrogramBuffer.add(
                transformed
            )
        }

        /*
         * Keep the same general history size used
         * by openWakeWord.
         *
         * 97 frames ~= 1 second at 16 kHz.
         */
        val maximumFrames =
            97 * 10

        while (
            melSpectrogramBuffer.size >
            maximumFrames
        ) {
            melSpectrogramBuffer.removeFirst()
        }
    }

    /**
     * Run melspectrogram.tflite.
     *
     * IMPORTANT:
     * The model receives 16-bit PCM values converted
     * to Float32. Do NOT normalize them to [-1,1].
     */
    private fun runMelModel(
        pcm: ShortArray
    ): List<FloatArray> {

        val sampleCount =
            pcm.size

        val input =
            Array(1) {
                FloatArray(sampleCount)
            }

        for (i in pcm.indices) {
            input[0][i] =
                pcm[i].toFloat()
        }

        resizeMelModelIfNecessary(
            sampleCount
        )

        val outputShape =
            melInterpreter
                .getOutputTensor(0)
                .shape()

        /*
         * Expected:
         *
         * [1, frames, 32]
         */
        val frames =
            if (outputShape.size >= 3) {
                outputShape[1]
            } else {
                1
            }

        val output =
            Array(1) {
                Array(frames) {
                    FloatArray(MEL_FEATURES)
                }
            }

        melInterpreter.run(
            input,
            output
        )

        val result =
            ArrayList<FloatArray>()

        for (frame in output[0]) {
            result.add(
                frame.copyOf()
            )
        }

        return result
    }

    /**
     * Resize the dynamic mel model input when needed.
     */
    private fun resizeMelModelIfNecessary(
        sampleCount: Int
    ) {

        val shape =
            melInterpreter
                .getInputTensor(0)
                .shape()

        if (
            shape.size != 2 ||
            shape[1] != sampleCount
        ) {

            melInterpreter.resizeInput(
                0,
                intArrayOf(
                    1,
                    sampleCount
                )
            )

            melInterpreter.allocateTensors()
        }
    }

    /**
     * Convert the newest mel frames into 96-D
     * speech embeddings.
     *
     * Windows are 76 frames long and move by
     * 8 frames, matching openWakeWord.
     */
    private fun createEmbeddingsFromLatestMelData():
        List<FloatArray> {

        if (
            melSpectrogramBuffer.size <
            MEL_WINDOW_SIZE
        ) {
            return emptyList()
        }

        val results =
            mutableListOf<FloatArray>()

        /*
         * Only the newly available region needs to
         * generate embeddings.
         *
         * For each 1280-sample audio step,
         * openWakeWord advances approximately
         * 8 mel frames.
         */
        val start =
            maxOf(
                0,
                melSpectrogramBuffer.size -
                    MEL_WINDOW_SIZE -
                    8 + 1
            )

        val end =
            melSpectrogramBuffer.size -
                MEL_WINDOW_SIZE

        if (end < start) {
            return emptyList()
        }

        /*
         * Usually this produces one embedding per
         * streaming audio step.
         */
        val windows =
            mutableListOf<Array<Array<FloatArray>>>()

        var frameStart = start

        while (
            frameStart <= end
        ) {

            val window =
                Array(MEL_WINDOW_SIZE) {
                    Array(MEL_FEATURES) {
                        FloatArray(1)
                    }
                }

            for (
                frameIndex
                in 0 until MEL_WINDOW_SIZE
            ) {

                val source =
                    melSpectrogramBuffer[
                        frameStart + frameIndex
                    ]

                for (
                    featureIndex
                    in 0 until MEL_FEATURES
                ) {

                    window[
                        frameIndex
                    ][featureIndex][0] =
                        source[featureIndex]
                }
            }

            windows.add(window)

            frameStart += 8
        }

        if (windows.isEmpty()) {
            return emptyList()
        }

        /*
         * The embedding model accepts:
         *
         * [batch, 76, 32, 1]
         */
        val batch =
            Array(windows.size) {
                windows[it]
            }

        return runEmbeddingModel(batch)
    }

    /**
     * Run embedding_model.tflite.
     */
    private fun runEmbeddingModel(
        input: Array<Array<Array<FloatArray>>>
    ): List<FloatArray> {

        val batchSize =
            input.size

        if (batchSize == 0) {
            return emptyList()
        }

        resizeEmbeddingModelIfNecessary(
            batchSize
        )

        /*
         * Output for batch N is effectively:
         *
         * [N, 1, 96]
         *
         * We flatten each item to 96 values.
         */
        val output =
            Array(batchSize) {
                Array(1) {
                    FloatArray(
                        EMBEDDING_SIZE
                    )
                }
            }

        embeddingInterpreter.run(
            input,
            output
        )

        val results =
            mutableListOf<FloatArray>()

        for (i in 0 until batchSize) {

            results.add(
                output[i][0].copyOf()
            )
        }

        return results
    }

    /**
     * The embedding model was exported with a
     * dynamic batch dimension.
     */
    private fun resizeEmbeddingModelIfNecessary(
        batchSize: Int
    ) {

        val shape =
            embeddingInterpreter
                .getInputTensor(0)
                .shape()

        if (
            shape.size != 4 ||
            shape[0] != batchSize
        ) {

            embeddingInterpreter.resizeInput(
                0,
                intArrayOf(
                    batchSize,
                    MEL_WINDOW_SIZE,
                    MEL_FEATURES,
                    1
                )
            )

            embeddingInterpreter.allocateTensors()
        }
    }

    /**
     * Return the newest 16 x 96 feature window.
     */
    private fun getLatestFeatureWindow():
        Array<FloatArray> {

        val result =
            Array(FEATURE_WINDOW_SIZE) {
                FloatArray(
                    EMBEDDING_SIZE
                )
            }

        val start =
            featureBuffer.size -
                FEATURE_WINDOW_SIZE

        var outputIndex = 0

        for (
            index in start until featureBuffer.size
        ) {

            featureBuffer[index].copyInto(
                result[outputIndex]
            )

            outputIndex++
        }

        return result
    }

    /**
     * Reset streaming state.
     */
    @Synchronized
    fun reset() {

        rawDataBuffer.clear()

        featureBuffer.clear()

        accumulatedSamples = 0

        melSpectrogramBuffer =
            createInitialMelBuffer()
    }

    /**
     * Release TFLite resources.
     */
    @Synchronized
    fun close() {

        reset()

        melInterpreter.close()

        embeddingInterpreter.close()
    }

    private fun createInitialMelBuffer():
        ArrayDeque<FloatArray> {

        val buffer =
            ArrayDeque<FloatArray>(
                MEL_WINDOW_SIZE
            )

        repeat(MEL_WINDOW_SIZE) {

            val frame =
                FloatArray(
                    MEL_FEATURES
                ) {
                    1.0f
                }

            buffer.addLast(frame)
        }

        return buffer
    }

    /**
     * Load a .tflite file directly from assets.
     */
    private fun loadModel(
        context: Context,
        fileName: String
    ): MappedByteBuffer {

        val descriptor =
            context.assets.openFd(fileName)

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