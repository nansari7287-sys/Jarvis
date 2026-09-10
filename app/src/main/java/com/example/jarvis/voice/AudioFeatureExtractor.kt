package com.example.jarvis.voice

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayDeque

/**
 * openWakeWord feature extraction pipeline.
 *
 * 16 kHz PCM
 *      ↓
 * melspectrogram.tflite
 *      ↓
 * 32-bin mel features
 *      ↓
 * 76 x 32 windows
 *      ↓
 * embedding_model.tflite
 *      ↓
 * 96-D embeddings
 *      ↓
 * 16 x 96 feature window
 */
class AudioFeatureExtractor(
    context: Context
) {

    companion object {
        private const val SAMPLE_RATE = 16_000

        // 80 ms at 16 kHz.
        private const val STEP_SAMPLES = 1_280

        // embedding_model.tflite
        private const val MEL_WINDOW_SIZE = 76
        private const val MEL_FEATURES = 32

        // hey_jarvis_v0.1.tflite
        private const val FEATURE_WINDOW_SIZE = 16
        private const val EMBEDDING_SIZE = 96

        private const val MEL_MODEL_NAME =
            "melspectrogram.tflite"

        private const val EMBEDDING_MODEL_NAME =
            "embedding_model.tflite"
    }

    private val melInterpreter: Interpreter
    private val embeddingInterpreter: Interpreter

    private val rawDataBuffer =
        ArrayDeque<Short>()

    private val melSpectrogramBuffer =
        ArrayDeque<FloatArray>()

    private val featureBuffer =
        ArrayDeque<FloatArray>()

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

        resizeMelModelIfNecessary(
            STEP_SAMPLES
        )

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

        reset()
    }

    /**
     * Accept normalized Float audio [-1, +1].
     */
    @Synchronized
    fun processAudio(
        audio: FloatArray
    ): List<Array<FloatArray>> {

        if (audio.isEmpty()) {
            return emptyList()
        }

        val pcm =
            ShortArray(audio.size)

        for (i in audio.indices) {

            val sample =
                audio[i].coerceIn(
                    -1.0f,
                    1.0f
                )

            pcm[i] =
                (
                    sample * 32767.0f
                )
                    .toInt()
                    .coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    )
                    .toShort()
        }

        return processPcm(pcm)
    }

    /**
     * Accept native 16-bit PCM audio.
     *
     * Returns [16 x 96] windows.
     */
    @Synchronized
    fun processPcm(
        pcm: ShortArray
    ): List<Array<FloatArray>> {

        if (pcm.isEmpty()) {
            return emptyList()
        }

        for (sample in pcm) {
            rawDataBuffer.addLast(sample)
            accumulatedSamples++
        }

        // Keep maximum 10 seconds of audio history.
        while (
            rawDataBuffer.size >
            SAMPLE_RATE * 10
        ) {
            rawDataBuffer.removeFirst()
        }

        val results =
            mutableListOf<Array<FloatArray>>()

        while (
            accumulatedSamples >= STEP_SAMPLES
        ) {

            updateStreamingMelSpectrogram()

            val embeddings =
                createEmbeddingsFromLatestMelData()

            for (embedding in embeddings) {

                featureBuffer.addLast(
                    embedding
                )

                while (
                    featureBuffer.size >
                    120
                ) {
                    featureBuffer.removeFirst()
                }

                if (
                    featureBuffer.size >=
                    FEATURE_WINDOW_SIZE
                ) {
                    results.add(
                        getLatestFeatureWindow()
                    )
                }
            }

            accumulatedSamples -=
                STEP_SAMPLES
        }

        return results
    }

    /**
     * Run the mel-spectrogram model.
     */
    private fun updateStreamingMelSpectrogram() {

        if (rawDataBuffer.size < 400) {
            return
        }

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

        for (
            index in 0 until rawDataBuffer.size
        ) {

            if (index >= skip) {

                samples[outputIndex] =
                    rawDataBuffer.elementAt(index)

                outputIndex++
            }
        }

        val melOutput =
            runMelModel(samples)

        /*
         * openWakeWord normalization:
         *
         * spectrogram / 10 + 2
         */
        for (frame in melOutput) {

            val transformed =
                FloatArray(
                    MEL_FEATURES
                )

            for (
                i in 0 until MEL_FEATURES
            ) {

                transformed[i] =
                    frame[i] / 10.0f + 2.0f
            }

            melSpectrogramBuffer.addLast(
                transformed
            )
        }

        while (
            melSpectrogramBuffer.size >
            97 * 10
        ) {
            melSpectrogramBuffer.removeFirst()
        }
    }

    /**
     * Run melspectrogram.tflite.
     *
     * The model expects Float32 PCM values.
     */
    private fun runMelModel(
        pcm: ShortArray
    ): List<FloatArray> {

        if (pcm.isEmpty()) {
            return emptyList()
        }

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

        val frames =
            if (
                outputShape.size >= 3
            ) {
                outputShape[1]
            } else {
                1
            }

        val output =
            Array(1) {
                Array(frames) {
                    FloatArray(
                        MEL_FEATURES
                    )
                }
            }

        melInterpreter.run(
            input,
            output
        )

        val result =
            mutableListOf<FloatArray>()

        for (frame in output[0]) {

            result.add(
                frame.copyOf()
            )
        }

        return result
    }

    /**
     * Resize dynamic mel input.
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
     * Convert 76 x 32 mel windows into 96-D
     * embeddings.
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

        var frameStart = start

        while (
            frameStart <= end
        ) {

            val window =
                Array(
                    MEL_WINDOW_SIZE
                ) {
                    Array(
                        MEL_FEATURES
                    ) {
                        FloatArray(1)
                    }
                }

            for (
                frameIndex in
                0 until MEL_WINDOW_SIZE
            ) {

                val source =
                    melSpectrogramBuffer.elementAt(
                        frameStart +
                            frameIndex
                    )

                for (
                    featureIndex in
                    0 until MEL_FEATURES
                ) {

                    window[
                        frameIndex
                    ][featureIndex][0] =
                        source[featureIndex]
                }
            }

            val embedding =
                runEmbeddingModel(
                    window
                )

            if (embedding != null) {
                results.add(
                    embedding
                )
            }

            frameStart += 8
        }

        return results
    }

    /**
     * Run embedding_model.tflite.
     *
     * Input:
     * [1, 76, 32, 1]
     *
     * Output:
     * [1, 1, 96]
     */
    private fun runEmbeddingModel(
        input: Array<Array<FloatArray>>
    ): FloatArray? {

        resizeEmbeddingModelIfNecessary()

        val output =
            Array(1) {
                Array(1) {
                    FloatArray(
                        EMBEDDING_SIZE
                    )
                }
            }

        try {

            embeddingInterpreter.run(
                arrayOf(input),
                output
            )

            return output[0][0].copyOf()

        } catch (e: Exception) {

            throw IllegalStateException(
                "Embedding inference failed: " +
                    e.message,
                e
            )
        }
    }

    /**
     * Ensure embedding model has:
     *
     * [1, 76, 32, 1]
     */
    private fun resizeEmbeddingModelIfNecessary() {

        val shape =
            embeddingInterpreter
                .getInputTensor(0)
                .shape()

        if (
            shape.size != 4 ||
            shape[0] != 1 ||
            shape[1] != MEL_WINDOW_SIZE ||
            shape[2] != MEL_FEATURES ||
            shape[3] != 1
        ) {

            embeddingInterpreter.resizeInput(
                0,
                intArrayOf(
                    1,
                    MEL_WINDOW_SIZE,
                    MEL_FEATURES,
                    1
                )
            )

            embeddingInterpreter.allocateTensors()
        }
    }

    /**
     * Get latest [16 x 96] feature window.
     */
    private fun getLatestFeatureWindow():
        Array<FloatArray> {

        val result =
            Array(
                FEATURE_WINDOW_SIZE
            ) {
                FloatArray(
                    EMBEDDING_SIZE
                )
            }

        val start =
            featureBuffer.size -
                FEATURE_WINDOW_SIZE

        var outputIndex = 0

        for (
            index in
            start until featureBuffer.size
        ) {

            val embedding =
                featureBuffer.elementAt(
                    index
                )

            embedding.copyInto(
                result[outputIndex]
            )

            outputIndex++
        }

        return result
    }

    /**
     * Reset all streaming state.
     */
    @Synchronized
    fun reset() {

        rawDataBuffer.clear()

        melSpectrogramBuffer.clear()

        featureBuffer.clear()

        accumulatedSamples = 0

        /*
         * Initial mel history.
         */
        repeat(
            MEL_WINDOW_SIZE
        ) {

            melSpectrogramBuffer.addLast(
                FloatArray(
                    MEL_FEATURES
                ) {
                    1.0f
                }
            )
        }
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

    /**
     * Load TFLite model from assets.
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