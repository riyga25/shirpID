package org.tensorflow.lite.examples.soundclassifier

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.util.Locale
import kotlin.math.cos

class SoundClassifier(
    context: Context,
    private val binding: ActivityMainBinding,
    private val options: Options = Options()
) {
    private val mContext = context.applicationContext
    private val recognitionScope = CoroutineScope(Dispatchers.IO)

    /** Names of the model's output classes.  */
    lateinit var labelList: List<String>

    /** Names of the model's output classes.  */
    lateinit var assetList: List<String>

    private lateinit var interpreter: Interpreter
    private lateinit var metaInterpreter: Interpreter

    private lateinit var predictionProbs: FloatArray
    private lateinit var metaPredictionProbs: FloatArray
    private lateinit var inputBuffer: FloatBuffer
    private lateinit var metaInputBuffer: FloatBuffer

    private var modelInputLength = 0
    private var modelNumClasses = 0
    private var metaModelInputLength = 0
    private var metaModelNumClasses = 0
    private var inferenceInterval = 800L

    var isRecording = false

    /** Used to record audio samples. */
    private lateinit var audioRecord: AudioRecord

    init {
        loadLabels()
        loadAssetList()
        setupInterpreter()
        setupMetaInterpreter()
        warmUpModel()
    }

    fun start() {
        if (!isRecording) {
            startAudioRecord()
            isRecording = true
        }
    }

    fun stop() {
        if (isRecording) {
            recognitionScope.cancel()
            audioRecord.stop()
            isRecording = false
        }
    }

    private fun loadLabels() {
        try {
            val filename = "labels_ru.txt"
            val labels = mContext.assets.open(filename).bufferedReader().use { it.readLines() }
            labelList = labels.map { it.trim().capitalize(Locale.ROOT) }
            Log.i(TAG, "Loaded ${labelList.size} labels from $filename")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load labels: ${e.message}")
        }
    }

    /** Retrieve asset list from "asset_list" file */
    private fun loadAssetList() {
        try {
            val filename = options.assetFile
            val assets = mContext.assets.open(filename).bufferedReader().use { it.readLines() }
            assetList = assets.map { it.trim() }
            Log.i(TAG, "Loaded ${assetList.size} assets from $filename")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load asset list from ${options.assetFile}: ${e.message}")
            assetList = emptyList()
        }
    }

    private fun setupInterpreter() {
        try {
            val modelFile = File(mContext.getDir("filesdir", Context.MODE_PRIVATE), options.modelPath)
            val buffer = FileChannel.open(modelFile.toPath(), StandardOpenOption.READ).use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            }
            interpreter = Interpreter(buffer, Interpreter.Options())

            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()
            modelInputLength = inputShape[1]
            modelNumClasses = outputShape[1]

            predictionProbs = FloatArray(modelNumClasses) { Float.NaN }
            inputBuffer = FloatBuffer.allocate(modelInputLength)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize interpreter: ${e.message}")
        }
    }

    private fun setupMetaInterpreter() {
        try {
            val metaModelFile = File(mContext.getDir("filesdir", Context.MODE_PRIVATE), options.metaModelPath)
            val buffer = FileChannel.open(metaModelFile.toPath(), StandardOpenOption.READ).use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            }
            metaInterpreter = Interpreter(buffer, Interpreter.Options())

            val inputShape = metaInterpreter.getInputTensor(0).shape()
            val outputShape = metaInterpreter.getOutputTensor(0).shape()
            metaModelInputLength = inputShape[1]
            metaModelNumClasses = outputShape[1]

            metaPredictionProbs = FloatArray(metaModelNumClasses) { 1f }
            metaInputBuffer = FloatBuffer.allocate(metaModelInputLength)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize meta-interpreter: ${e.message}")
        }
    }

    fun runMetaInterpreter(location: Location) {
        Log.i(TAG, "Location is: ${location.latitude}/${location.longitude}")

        try {
            val dayOfYear = LocalDate.now().dayOfYear
            val weekMeta = cos(Math.toRadians(dayOfYear * 7.5)) + 1.0
            val lat = location.latitude.toFloat()
            val lon = location.longitude.toFloat()

            metaInputBuffer.put(0, lat)
            metaInputBuffer.put(1, lon)
            metaInputBuffer.put(2, weekMeta.toFloat())
            metaInputBuffer.rewind() // Reset position to beginning of buffer
            val metaOutputBuffer = FloatBuffer.allocate(metaModelNumClasses)
            metaOutputBuffer.rewind()
            metaInterpreter.run(metaInputBuffer, metaOutputBuffer)
            metaOutputBuffer.rewind()
            metaOutputBuffer.get(metaPredictionProbs) // Copy data to metaPredictionProbs.

            metaPredictionProbs = metaPredictionProbs.map { prob ->
                when {
                    prob >= options.metaProbabilityThreshold1 -> 1f
                    prob >= options.metaProbabilityThreshold2 -> 0.8f
                    prob >= options.metaProbabilityThreshold3 -> 0.5f
                    else -> 0f
                }
            }.toFloatArray()
        } catch (e: Throwable) {
            Log.e(TAG, "Error run MetaInterpreter: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioRecord() {
        try {
            val bufferSize = maxOf(
                AudioRecord.getMinBufferSize(
                    options.sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ),
                options.sampleRate * 2
            )
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.UNPROCESSED,
                options.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            audioRecord.startRecording()
            recognitionScope.launch { startRecognition() }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioRecord: ${e.message}")
        }
    }

    private suspend fun startRecognition() {
        val circularBuffer = ShortArray(modelInputLength)
        var bufferIndex = 0

        while (recognitionScope.isActive) {
            val recordingBuffer = ShortArray(modelInputLength)
            val samples = audioRecord.read(recordingBuffer, 0, recordingBuffer.size) ?: 0
            if (samples > 0) {
                bufferIndex = updateCircularBuffer(circularBuffer, recordingBuffer, samples, bufferIndex)
                val inputBuffer = prepareInputBuffer(circularBuffer, bufferIndex)
                if (inputBuffer != null) {
                    val outputBuffer = FloatBuffer.allocate(modelNumClasses)
                    interpreter.run(inputBuffer, outputBuffer)
                    processModelOutput(outputBuffer)
                }
            }
            delay(inferenceInterval)
        }
    }

    private fun updateCircularBuffer(buffer: ShortArray, data: ShortArray, count: Int, index: Int): Int {
        var currentIndex = index
        for (i in 0 until count) {
            buffer[currentIndex] = data[i]
            currentIndex = (currentIndex + 1) % buffer.size
        }
        return currentIndex
    }

    private fun prepareInputBuffer(buffer: ShortArray, index: Int): FloatBuffer? {
        val input = FloatBuffer.allocate(modelInputLength)
        var allZero = true
        for (i in 0 until modelInputLength) {
            val sample = buffer[(i + index) % modelInputLength]
            if (sample != 0.toShort()) allZero = false
            input.put(sample.toFloat() / 32768f)
        }
        return if (allZero) null else input.apply { rewind() }
    }

    private fun processModelOutput(outputBuffer: FloatBuffer) {
        outputBuffer.rewind()
        outputBuffer.get(predictionProbs)

        val probList = if (binding.checkIgnoreMeta.isChecked) {
            predictionProbs.map { 1 / (1 + kotlin.math.exp(-it)) } // Apply sigmoid
        } else {
            predictionProbs.mapIndexed { i, value ->
                metaPredictionProbs[i] / (1 + kotlin.math.exp(-value))
            }
        }

        val max = probList.withIndex().maxByOrNull { it.value }

        max?.let {
            // TODO push bird index to state
            Log.i(TAG, "Predictions max: ${labelList[it.index]} / ${"%.2f".format(it.value * 100)}%")
        }
    }

    private fun warmUpModel() {
        repeat(options.warmupRuns) {
            val dummyInput = FloatBuffer.allocate(modelInputLength).apply {
                for (i in 0 until modelInputLength) put(0f)
                rewind()
            }
            val dummyOutput = FloatBuffer.allocate(modelNumClasses)
            interpreter.run(dummyInput, dummyOutput)
        }
    }

    class Options(
        val assetFile: String = "assets.txt",
        val modelPath: String = "model.tflite",
        val metaModelPath: String = "metaModel.tflite",
        val sampleRate: Int = 48000,
        val warmupRuns: Int = 3,
        val metaProbabilityThreshold1: Float = 0.01f,
        val metaProbabilityThreshold2: Float = 0.008f,
        val metaProbabilityThreshold3: Float = 0.001f
    )

    companion object {
        private const val TAG = "SoundClassifier"
    }
}
