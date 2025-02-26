package com.riyga.identifier

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.channels.Channels
import java.time.LocalDate
import java.util.Locale
import kotlin.math.cos

class SoundClassifier(
    context: Context,
    private val options: Options = Options(),
    private val externalScope: CoroutineScope
) {

    companion object {
        private const val TAG = "SoundClassifier"
    }

    private val mContext = context.applicationContext

    private val _birdEvents = MutableSharedFlow<Pair<String, Float>>(extraBufferCapacity = 1)
    val birdEvents: SharedFlow<Pair<String, Float>> get() = _birdEvents

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

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

    private var audioRecord: AudioRecord? = null
    private var wavRecorder: WavRecorder? = null

    private val audioLock = Any()

    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(
            options.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        options.sampleRate * 2
    )

    init {
        try {
            initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed: ${e.message}")
        }
    }

    fun start() {
        synchronized(audioLock) {
            if (_isRecording.value) return

            try {
                if (audioRecord == null || audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = initializeAudioRecord()
                }

                wavRecorder = WavRecorder(
                    audioRecord = audioRecord!!,
                    context = mContext
                )

                audioRecord?.startRecording()
                wavRecorder?.startRecording()

                externalScope.launch(Dispatchers.IO) {
                    startRecognition()
                }
                _isRecording.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting audio record: ${e.message}")
            }
        }

    }

    fun stop(saveRecording: Boolean = true) {
        synchronized(audioLock) {
            if (!_isRecording.value) return

            try {
                audioRecord?.apply {
                    if (state == AudioRecord.STATE_INITIALIZED) {
                        stop()
                        release()
                    }
                }
                audioRecord = null
                _isRecording.value = false

                if (saveRecording) {
                    wavRecorder?.stopRecording()
                } else {
                    wavRecorder?.cancelRecording()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio record: ${e.message}")
            }
        }
    }

    private fun initialize() {
        loadLabels()
        loadAssetList()
        interpreter = setupInterpreter(options.modelPath, ::onInterpreterReady)
        metaInterpreter = setupInterpreter(options.metaModelPath, ::onMetaInterpreterReady)
        warmUpModel()
    }

    @SuppressLint("MissingPermission")
    private fun initializeAudioRecord(): AudioRecord {
        return AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            options.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }

    private fun loadLabels() {
        try {
            val labels =
                mContext.assets.open(options.labelsFile).bufferedReader().use { it.readLines() }
            labelList = labels.map { it.trim().capitalize(Locale.ROOT) }
            Log.i(TAG, "Loaded ${labelList.size} labels from ${options.labelsFile}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load labels: ${e.message}")
        }
    }

    /** Retrieve asset list from "asset_list" file */
    private fun loadAssetList() {
        try {
            val assets =
                mContext.assets.open(options.assetFile).bufferedReader().use { it.readLines() }
            assetList = assets.map { it.trim() }
            Log.i(TAG, "Loaded ${assetList.size} assets from ${options.assetFile}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load asset list from ${options.assetFile}: ${e.message}")
            assetList = emptyList()
        }
    }

    private fun loadModelFromAssets(context: Context, fileName: String): ByteBuffer {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val fileSize = inputStream.available()
        val buffer = ByteBuffer.allocateDirect(fileSize).order(ByteOrder.nativeOrder())
        val channel = Channels.newChannel(inputStream)
        channel.read(buffer)
        buffer.rewind()
        return buffer
    }

    private fun onInterpreterReady(
        interpreter: Interpreter,
        inputShape: IntArray,
        outputShape: IntArray
    ) {
        modelInputLength = inputShape[1]
        modelNumClasses = outputShape[1]
        predictionProbs = FloatArray(modelNumClasses) { Float.NaN }
        inputBuffer = FloatBuffer.allocate(modelInputLength)
    }

    private fun onMetaInterpreterReady(
        interpreter: Interpreter,
        inputShape: IntArray,
        outputShape: IntArray
    ) {
        metaModelInputLength = inputShape[1]
        metaModelNumClasses = outputShape[1]
        metaPredictionProbs = FloatArray(metaModelNumClasses) { 1f }
        metaInputBuffer = FloatBuffer.allocate(metaModelInputLength)
    }

    private fun setupInterpreter(
        modelPath: String,
        onReady: (Interpreter, IntArray, IntArray) -> Unit
    ): Interpreter {
        try {
            val buffer = loadModelFromAssets(mContext, modelPath)
            val interpreter = Interpreter(buffer, Interpreter.Options())

            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()

            onReady(interpreter, inputShape, outputShape)

            Log.i(TAG, "Interpreter initialized for $modelPath")
            return interpreter
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize interpreter for $modelPath: ${e.message}")
            throw RuntimeException("Interpreter initialization failed")
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

    private suspend fun startRecognition() {
        val circularBuffer = ShortArray(modelInputLength)
        var bufferIndex = 0

        while (externalScope.isActive && _isRecording.value) {
            val recordingBuffer = ShortArray(modelInputLength)
            val samples = audioRecord?.read(recordingBuffer, 0, recordingBuffer.size) ?: 0
            if (samples > 0) {
                wavRecorder?.writeAudioDataLoop(recordingBuffer, samples)
                bufferIndex =
                    updateCircularBuffer(circularBuffer, recordingBuffer, samples, bufferIndex)
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

    private fun updateCircularBuffer(
        buffer: ShortArray,
        data: ShortArray,
        count: Int,
        index: Int
    ): Int {
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

        val probList = if (false) { // check location is disabled
            predictionProbs.map { 1 / (1 + kotlin.math.exp(-it)) } // Apply sigmoid
        } else {
            predictionProbs.mapIndexed { i, value ->
                metaPredictionProbs[i] / (1 + kotlin.math.exp(-value))
            }
        }

        val max = probList.withIndex().maxByOrNull { it.value }

        max?.let {
            externalScope.launch { _birdEvents.emit(labelList[it.index] to it.value) }
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
        val labelsFile: String = "labels_ru.txt",
        val modelPath: String = "BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite",
        val metaModelPath: String = "BirdNET_GLOBAL_6K_V2.4_MData_Model_FP16.tflite",
        val sampleRate: Int = 48000, // 48 кГц
        val warmupRuns: Int = 3,
        val metaProbabilityThreshold1: Float = 0.01f,
        val metaProbabilityThreshold2: Float = 0.008f,
        val metaProbabilityThreshold3: Float = 0.001f
    )
}
