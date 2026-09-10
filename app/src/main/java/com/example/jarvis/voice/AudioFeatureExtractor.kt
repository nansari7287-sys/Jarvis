package com.example.jarvis.voice

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayDeque
import kotlin.math.roundToInt

/**
 * Converts microphone PCM audio into the 16 x 96 feature windows
 * required by hey_jarvis_v0.1.tflite.
 *
 * Pipeline:
 *
 * 16 kHz PCM
 *     ↓
 * melspectrogram.tflite
 *     ↓
 * 76 x 32 mel window
 *     ↓
 * embedding_model.tflite
 *     ↓
 * 96-dimensional embedding
 *     ↓
 * 16 consecutive embeddings
 *     ↓
 * 16 x 96 wake-word input
 */
class AudioFeatureExtractor(
    context: Context
) {

    companion object {
        private const val SAMPLE_RATE = 16_000

        // openWakeWord processes audio in 80 ms chunks.
        private const val AUDIO_CHUNK_SAMPLES = 1_280

        // The embedding model requires 76 mel frames.
        private const val MEL_WINDOW_FRAMES = 76
        private const val MEL_FEATURES = 32

        // Wake-word classifier requires 16 consecutive 96-D embeddings.
        private const val EMBEDDING_WINDOW = 16
        private const val EMBEDDING_SIZE = 96

        // 3 extra 160-sample frames are used by the official
        // streaming preprocessing path.
        private const val MEL_CONTEXT_SAMPLES = 160 * 3

        private const val MEL_MODEL = "melspectrogram.tflite"
        private const val EMBEDDING_MODEL = "embedding_model.tflite"
    }

    private val melInterpreter: Interpreter
    private val embeddingInterpreter: Interpreter

    private val rawAudioBuffer = ArrayDeque<Short>()

    private val melBuffer = ArrayDeque<FloatArray>()

    private val embeddingBuffer = ArrayDeque<FloatArray>()

    private var accumulatedSamples = 0

    init {
        melInterpreter = Interpreter(
            loadModel(context, MEL_MODEL),
            Interpreter.Options().apply {
                setNumThreads(2)
            }
        )

        // The official pipeline initializes the mel model with
        // 1280-sample audio input.
        try {
            melInterpreter.resizeInput(
                0,
                intArrayOf(1, AUDIO_CHUNK_SAMPLES)
            )
            melInterpreter.allocateTensors()
        } catch (_: Exception) {
            // Some LiteRT/TFLite versions may already allocate
            // the dynamic input correctly.
        }

        embeddingInterpreter = Interpreter(
            loadModel(context, EMBEDDING_MODEL),
            Interpreter.Options().apply {
                setNumThreads(2)
            }
        )

        embeddingInterpreter.allocateTensors()
    }

    /**
     * Feed microphone audio as normalized Float samples [-1, 1].
     *
     * Returns zero or more 16x96 feature windows ready for
     * hey_jarvis_v0.1.tflite.
     */
    @Synchronized
    fun processAudio(audio: FloatArray): List<Array<FloatArray>> {
        if (audio.isEmpty()) return emptyList()

        val pcm = ShortArray(audio.size)

        for (i in audio.indices) {
            val value = audio[i].coerceIn(-1.0f, 1.0f)

            pcm[i] = (
                value * 32767.0f
            ).roundToInt().coerceIn(
                Short.MIN_VALUE.toInt(),
                Short.MAX_VALUE.toInt()
            ).toShort()
        }

        return processPcm(pcm)
    }

    /**
     * Feed raw 16-bit PCM samples.
     */
    @Synchronized
    fun processPcm(pcm: ShortArray): List<Array<FloatArray>> {
        if (pcm.isEmpty()) return emptyList()

        for (sample in pcm) {
            rawAudioBuffer.addLast(sample)
            accumulatedSamples++
        }

        val results = mutableListOf<Array<FloatArray>>()

        while (accumulatedSamples >= AUDIO_CHUNK_SAMPLES) {

            // Consume one 80 ms chunk.
            accumulatedSamples -= AUDIO_CHUNK_SAMPLES

            updateMelFeatures()

            // The official openWakeWord pipeline advances the
            // embedding window by 8 mel frames.
            if (melBuffer.size >= MEL_WINDOW_FRAMES) {
                val embedding = createEmbeddingFromLatestMelWindow()

                if (embedding != null) {
                    embeddingBuffer.addLast(embedding)

                    while (embeddingBuffer.size > EMBEDDING_WINDOW) {
                        embeddingBuffer.removeFirst()
                    }

                    if (embeddingBuffer.size == EMBEDDING_WINDOW) {
                        results.add(
                            createWakeWordInput()
                        )
                    }
                }

                // Move the mel window forward approximately
                // 80 ms / 8 mel frames.
                repeat(8) {
                    if (melBuffer.isNotEmpty()) {
                        melBuffer.removeFirst()
                    }
                }
            }
        }

        // Keep the raw buffer bounded.
        while (rawAudioBuffer.size > SAMPLE_RATE * 2) {
            rawAudioBuffer.removeFirst()
        }

        return results
    }

    /**
     * Generate new mel-spectrogram frames from the most recent
     * audio samples.
     */
    private fun updateMelFeatures() {

        if (rawAudioBuffer.size < 400) {
            return
        }

        val requestedSamples =
            AUDIO_CHUNK_SAMPLES + MEL_CONTEXT_SAMPLES

        val sampleCount =
            minOf(requestedSamples, rawAudioBuffer.size)

        val audio = ShortArray(sampleCount)

        var index = 0

        val skip = rawAudioBuffer.size - sampleCount

        for ((position, sample) in rawAudioBuffer.withIndex()) {
            if (position >= skip) {
                audio[index++] = sample
            }
        }

        val input = Array(1) {
            FloatArray(audio.size)
        }

        /*
         * openWakeWord's mel model accepts PCM values and its
         * official implementation converts the input to float32.
         */
        for (i in audio.indices) {
            input[0][i] = audio[i].toFloat()
        }

        try {
            ensureMelInputSize(audio.size)

            val output = Array(
                1
            ) {
                Array(
                    estimateMelFrames(audio.size)
                ) {
                    FloatArray(MEL_FEATURES)
                }
            }

            melInterpreter.run(input, output)

            val actualFrames = output[0]

            for (frame in actualFrames) {
                if (frame.size == MEL_FEATURES) {
                    melBuffer.addLast(frame.copyOf())
                }
            }

            // Keep enough history for streaming operation.
            while (melBuffer.size > 97 * 10) {
                melBuffer.removeFirst()
            }

        } catch (e: Exception) {
            throw IllegalStateException(
                "Mel-spectrogram inference failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Resize the dynamic mel model input when necessary.
     */
    private fun ensureMelInputSize(sampleCount: Int) {
        val currentShape =
            melInterpreter.getInputTensor(0).shape()

        if (
            currentShape.size != 2 ||
            currentShape[1] != sampleCount
        ) {
            melInterpreter.resizeInput(
                0,
                intArrayOf(1, sampleCount)
            )

            melInterpreter.allocateTensors()
        }
    }

    /**
     * The exported mel model normally produces approximately
     * ceil(samples / 160 - 3) frames.
     */
    private fun estimateMelFrames(sampleCount: Int): Int {
        return maxOf(
            1,
            kotlin.math.ceil(
                sampleCount / 160.0 - 3.0
            ).toInt()
        )
    }

    /**
     * Converts the newest 76x32 mel window into one 96-D
     * speech embedding.
     */
    private fun createEmbeddingFromLatestMelWindow():
        FloatArray? {

        if (melBuffer.size < MEL_WINDOW_FRAMES) {
            return null
        }

        val input = Array(1) {
            Array(MEL_WINDOW_FRAMES) {
                Array(MEL_FEATURES) {
                    FloatArray(1)
                }
            }
        }

        val start =
            melBuffer.size - MEL_WINDOW_FRAMES

        var frameIndex = 0

        for ((index, frame) in melBuffer.withIndex()) {
            if (index < start) continue

            for (featureIndex in 0 until MEL_FEATURES) {
                input[0][frameIndex][featureIndex][0] =
                    frame[featureIndex]
            }

            frameIndex++

            if (frameIndex == MEL_WINDOW_FRAMES) {
                break
            }
        }

        val output = Array(1) {
            Array(1) {
                Array(1) {
                    FloatArray(EMBEDDING_SIZE)
                }
            }
        }

        try {
            embeddingInterpreter.run(input, output)

            return output[0][0][0].copyOf()

        } catch (e: Exception) {
            throw IllegalStateException(
                "Embedding inference failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Creates the exact classifier input:
     *
     * [1, 16, 96]
     */
    private fun createWakeWordInput():
        Array<FloatArray> {

        val result =
            Array(EMBEDDING_WINDOW) {
                FloatArray(EMBEDDING_SIZE)
            }

        var index = 0

        for (embedding in embeddingBuffer) {
            embedding.copyInto(
                destination = result[index]
            )
            index++
        }

        return result
    }

    /**
     * Clear all streaming state.
     */
    @Synchronized
    fun reset() {
        rawAudioBuffer.clear()
        melBuffer.clear()
        embeddingBuffer.clear()
        accumulatedSamples = 0
    }

    fun close() {
        synchronized(this) {
            reset()
            melInterpreter.close()
            embeddingInterpreter.close()
        }
    }

    private fun loadModel(
        context: Context,
        fileName: String
    ): MappedByteBuffer {

        val fileDescriptor =
            context.assets.openFd(fileName)

        FileInputStream(
            fileDescriptor.fileDescriptor
        ).use { inputStream ->

            val fileChannel = inputStream.channel

            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }
}
