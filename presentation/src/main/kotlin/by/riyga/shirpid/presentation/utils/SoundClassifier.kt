package by.riyga.shirpid.presentation.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
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
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.collections.take
import kotlin.math.abs
import kotlin.math.exp

class SoundClassifier(
    private val context: Context,
    private val externalScope: CoroutineScope
) {
    private var options: Options = Options()

    companion object {
        private const val TAG = "ShirpID"
    }

    private val _birdEvents = MutableSharedFlow<Pair<Int, Float>>(extraBufferCapacity = 1)
    val birdEvents: SharedFlow<Pair<Int, Float>> get() = _birdEvents

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private var interpreter: Interpreter? = null
    private var metaInterpreter: Interpreter? = null
    private var modelBuffer: MappedByteBuffer? = null
    private var metaModelBuffer: MappedByteBuffer? = null

    private lateinit var predictionProbs: FloatArray
    private lateinit var metaPredictionProbs: FloatArray
    private lateinit var metaInputBuffer: FloatBuffer

    var modelInputLength = 0
    private var modelNumClasses = 0
    private var metaModelNumClasses = 0
    private val inferenceInterval = 1000L

    private var audioRecord: AudioRecord? = null
    private var wavManager: WavManager? = null

    private var noiseSuppressor: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    private val audioLock = Any()

    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(
            options.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        options.sampleRate * 2
    )

    var isModelsInitialized = false
        private set

    fun start() {
        println("lol start")
        synchronized(audioLock) {
            if (_isRecording.value) return

            try {
                if (audioRecord == null || audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = initializeAudioRecord()
                }

                wavManager = WavManager(context = context)

                audioRecord?.startRecording()
                wavManager?.startRecording(audioRecord = audioRecord!!)

                externalScope.launch(Dispatchers.IO) { startRecognition() }
                _isRecording.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting audio record: ${e.message}")
            }
        }
    }

    fun stop(saveRecording: Boolean = true): String? {
        synchronized(audioLock) {
            if (!_isRecording.value) return null

            try {
                audioRecord?.apply {
                    if (state == AudioRecord.STATE_INITIALIZED) stop()
                }

                noiseSuppressor?.enabled = false
                noiseSuppressor?.release()
                noiseSuppressor = null

                agc?.enabled = false
                agc?.release()
                agc = null

                audioRecord?.release()
                audioRecord = null

                val filePath = if (saveRecording) wavManager?.stopRecording() else null
                if (!saveRecording) wavManager?.cancelRecording()
                wavManager = null

                _isRecording.value = false
                return filePath
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio record: ${e.message}")
                return null
            }
        }
    }

    fun releaseModels() {
        try {
            interpreter?.close()
            metaInterpreter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreters: ${e.message}")
        } finally {
            interpreter = null
            metaInterpreter = null
            modelBuffer?.clear()
            metaModelBuffer?.clear()
            modelBuffer = null
            metaModelBuffer = null
            isModelsInitialized = false
            predictionProbs = FloatArray(0)
            metaPredictionProbs = FloatArray(0)
            modelInputLength = 0
            modelNumClasses = 0
            metaModelNumClasses = 0
        }
    }

    fun initializeModels(
        opt: Options = Options()
    ) {
        if (isModelsInitialized) return

        options = opt

        modelBuffer = loadModelFromAssets(options.modelPath)
        metaModelBuffer = loadModelFromAssets(options.metaModelPath)

        interpreter = Interpreter(modelBuffer!!)
        metaInterpreter = Interpreter(metaModelBuffer!!)

        interpreter?.allocateTensors()
        metaInterpreter?.allocateTensors()

        setupModelBuffers()

        warmUpModel()
        runMetaInterpreter()
        isModelsInitialized = true
    }

    @SuppressLint("MissingPermission")
    private fun initializeAudioRecord(): AudioRecord {
        val record = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            options.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)
            noiseSuppressor?.enabled = true
            Log.i(TAG, "NoiseSuppressor enabled: ${noiseSuppressor?.enabled}")
        }

        if (AutomaticGainControl.isAvailable()) {
            agc = AutomaticGainControl.create(record.audioSessionId)
            agc?.enabled = true
            Log.i(TAG, "AutomaticGainControl enabled: ${agc?.enabled}")
        }

        return record
    }

    private fun loadModelFromAssets(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).apply {
            fileDescriptor.close()
            inputStream.close()
        }
    }

    private fun setupModelBuffers() {
        val inputShape = interpreter!!.getInputTensor(0).shape()
        val outputShape = interpreter!!.getOutputTensor(0).shape()

        modelInputLength = inputShape[1]
        modelNumClasses = outputShape[1]
        predictionProbs = FloatArray(modelNumClasses) { Float.NaN }

        val metaInputShape = metaInterpreter!!.getInputTensor(0).shape()
        val metaOutputShape = metaInterpreter!!.getOutputTensor(0).shape()

        metaModelNumClasses = metaOutputShape[1]
        metaPredictionProbs = FloatArray(metaModelNumClasses) { 1f }

        val metaInputLength = metaInputShape[1]
        metaInputBuffer = FloatBuffer.allocate(metaInputLength)
    }

    fun runMetaInterpreter() {
        try {
            metaInputBuffer.put(0, options.latitude)
            metaInputBuffer.put(1, options.longitude)
            metaInputBuffer.put(2, options.week)
            metaInputBuffer.rewind()

            val metaOutputBuffer = FloatBuffer.allocate(metaModelNumClasses)
            metaInterpreter?.run(metaInputBuffer, metaOutputBuffer)
            metaOutputBuffer.rewind()
            metaOutputBuffer.get(metaPredictionProbs)
        } catch (e: Throwable) {
            Log.e(TAG, "Error run MetaInterpreter: ${e.message}")
        }
    }

    fun classify(inputBuffer: FloatBuffer): List<IndexedValue<Float>> {
        Log.d("WAV", "SoundClassifier.classify() called")
        Log.d("WAV", "Input buffer position: ${inputBuffer.position()}, limit: ${inputBuffer.limit()}, capacity: ${inputBuffer.capacity()}")

        // Ensure the input buffer has the correct size and position
        inputBuffer.rewind()
        Log.d("WAV", "Input buffer rewound, position: ${inputBuffer.position()}")

        val outputBuffer = FloatBuffer.allocate(modelNumClasses)
        Log.d("WAV", "Created output buffer with capacity: ${outputBuffer.capacity()}")

        try {
            interpreter?.run(inputBuffer, outputBuffer)
            Log.d("WAV", "Model inference completed successfully")
        } catch (e: Exception) {
            Log.e("WAV", "Error during model inference: ${e.message}", e)
            return emptyList()
        }

        outputBuffer.rewind()
        outputBuffer.get(predictionProbs)
        Log.d("WAV", "Retrieved predictions, first few values: ${predictionProbs.take(5)}")

        val result = processModelOutput(outputBuffer)
        Log.d("WAV", "processModelOutput returned ${result.size} results")

        return result
    }

    private suspend fun startRecognition() {
        val circularBuffer = ShortArray(modelInputLength)
        var bufferIndex = 0
        var chunk = 0

        while (externalScope.isActive && _isRecording.value) {
            val recordingBuffer = ShortArray(modelInputLength)
            val samples = audioRecord?.read(recordingBuffer, 0, recordingBuffer.size) ?: 0
            if (samples > 0) {
                wavManager?.writeAudioDataLoop(recordingBuffer, samples)
                bufferIndex =
                    updateCircularBuffer(circularBuffer, recordingBuffer, samples, bufferIndex)

                val inputBuffer = normalize(circularBuffer, bufferIndex)
                val result = classify(inputBuffer)

                result.forEach { prediction ->
                    externalScope.launch { _birdEvents.emit(prediction.index to prediction.value) }
                }

                println("lol chunk #$chunk")
                chunk++
//                delay(inferenceInterval)
            }
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

    private fun normalize(buffer: ShortArray, startIndex: Int): FloatBuffer {
        val fb = FloatBuffer.allocate(modelInputLength)
        val maxAmp = buffer.maxOf { abs(it.toInt()) }.coerceAtLeast(1)
        for (i in 0 until modelInputLength) {
            fb.put(buffer[(i + startIndex) % modelInputLength].toFloat() / maxAmp)
        }
        fb.rewind()
        return fb
    }

    private fun processModelOutput(outputBuffer: FloatBuffer): List<IndexedValue<Float>> {
        Log.d("WAV", "processModelOutput() called")
        outputBuffer.rewind()
        outputBuffer.get(predictionProbs)

        Log.d("WAV", "predictionProbs size: ${predictionProbs.size}")
        Log.d("WAV", "First 5 prediction values: ${predictionProbs.take(5)}")
        Log.d("WAV", "Last 5 prediction values: ${predictionProbs.takeLast(5)}")

        // Apply BirdNET's flat_sigmoid transformation
        val sensitivity = -1.0f
        val bias = 1.0f
        val transformedBias = (bias - 1.0f) * 10.0f

        val sigmoidValues = predictionProbs.map { value ->
            val clippedValue = (value + transformedBias).coerceIn(-20f, 20f)
            1.0f / (1.0f + exp(sensitivity * clippedValue))
        }

        Log.d("WAV", "After flat sigmoid - first 5 values: ${sigmoidValues.take(5)}")
        Log.d("WAV", "After flat sigmoid - last 5 values: ${sigmoidValues.takeLast(5)}")

        // Check if location data is properly set (latitude and longitude should not be default values)
        val hasValidLocation = options.latitude != -1f && options.longitude != -1f

        Log.d("WAV", "Location data valid: $hasValidLocation, lat: ${options.latitude}, lon: ${options.longitude}")

        // Apply metadata filter (location filter) conditionally
        val probList = sigmoidValues.mapIndexed { i, sigmoidValue ->
            if (i < metaPredictionProbs.size) {
                val locationProb = metaPredictionProbs[i]

                if (hasValidLocation) {
                    // Apply location filter when location data is valid
                    sigmoidValue * locationProb
                } else {
                    // Don't apply location filter when location data is not set (default -1, -1)
                    // This allows detection when location is unknown (like with microphone)
                    sigmoidValue
                }
            } else {
                // If index is out of bounds for metaPredictionProbs
                sigmoidValue
            }
        }

        Log.d("WAV", "After metadata filtering - first 5 values: ${probList.take(5)}")
        Log.d("WAV", "After metadata filtering - last 5 values: ${probList.takeLast(5)}")

        val sortedPredictions = probList.withIndex()
            .map { IndexedValue(it.index, it.value) }
            .sortedByDescending { it.value }

        Log.d("WAV", "After sorting - first 5 values: ${sortedPredictions.take(5).map { it.value }}")

        val topN = 3
        val topPredictions = sortedPredictions
            .take(topN)
            .filter { it.value >= options.confidenceThreshold }
            .also {
                Log.d("WAV", "After confidence filtering - ${it.size} predictions remain, threshold: ${options.confidenceThreshold}")
                it.forEachIndexed { index, prediction ->
                    Log.d("WAV", "Filtered prediction[$index]: index=${prediction.index}, value=${prediction.value}")
                }
            }

        Log.d("WAV", "processModelOutput returning ${topPredictions.size} results")
        return topPredictions
    }

    private fun warmUpModel() {
        repeat(options.warmupRuns) {
            val dummyInput = FloatBuffer.allocate(modelInputLength).apply {
                for (i in 0 until modelInputLength) put(0f)
                rewind()
            }
            val dummyOutput = FloatBuffer.allocate(modelNumClasses)
            interpreter?.run(dummyInput, dummyOutput)
        }
    }

    class Options(
        val modelPath: String = "BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite",
        val metaModelPath: String = "BirdNET_GLOBAL_6K_V2.4_MData_Model_V2_FP16.tflite",
        val sampleRate: Int = 48000,
        val warmupRuns: Int = 3,
        val confidenceThreshold: Float = 0.3f,
        val latitude: Float = -1F,
        val longitude: Float = -1F,
        val week: Float = -1F,
    )
}