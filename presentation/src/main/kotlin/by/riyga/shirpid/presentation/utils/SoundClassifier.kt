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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

class SoundClassifier(
    private val context: Context,
    private val options: Options = Options(),
    private val externalScope: CoroutineScope
) {

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

    private var modelInputLength = 0
    private var modelNumClasses = 0
    private var metaModelNumClasses = 0
    private var inferenceInterval = 800L

    private val silenceDetector = SilenceDetector(
        durationSeconds = 3,
        sampleRate = options.sampleRate
    )

    private var audioRecord: AudioRecord? = null
    private var wavRecorder: WavRecorder? = null

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

    private var isModelsInitialized = false

    init {
        try {
            initializeModels()
        } catch (e: Exception) {
            Log.e(TAG, "Model initialization failed: ${e.message}")
        }
    }

    fun start() {
        synchronized(audioLock) {
            if (_isRecording.value) return

            try {
                if (audioRecord == null || audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = initializeAudioRecord()
                }

                wavRecorder = WavRecorder(audioRecord = audioRecord!!, context = context)

                audioRecord?.startRecording()
                wavRecorder?.startRecording()

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

                val filePath = if (saveRecording) wavRecorder?.stopRecording() else null
                if (!saveRecording) wavRecorder?.cancelRecording()
                wavRecorder = null

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

    private fun initializeModels() {
        if (isModelsInitialized) return

        modelBuffer = loadModelFromAssets(options.modelPath)
        metaModelBuffer = loadModelFromAssets(options.metaModelPath)

        interpreter = Interpreter(modelBuffer!!)
        metaInterpreter = Interpreter(metaModelBuffer!!)

        interpreter?.allocateTensors()
        metaInterpreter?.allocateTensors()

        setupModelBuffers()

        warmUpModel()
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

//                silenceDetector.addSamples(recordingBuffer)
//
//                if (!silenceDetector.isSilence()) {
                    val inputBuffer = normalize(circularBuffer, bufferIndex)
                    val outputBuffer = FloatBuffer.allocate(modelNumClasses)
                    interpreter?.run(inputBuffer, outputBuffer)
                    processModelOutput(outputBuffer)
                    delay(inferenceInterval)
//                } else {
//                    delay(200) // быстро проверяем тишину, но не нагружаем CPU
//                }
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

    private fun processModelOutput(outputBuffer: FloatBuffer) {
        outputBuffer.rewind()
        outputBuffer.get(predictionProbs)

        val probList = predictionProbs.mapIndexed { i, value ->
            metaPredictionProbs[i] / (1 + exp(-value))
        }

        val sortedPredictions = probList.withIndex()
            .map { IndexedValue(it.index, it.value) }
            .sortedByDescending { it.value }

        val topN = 3
        val topPredictions = sortedPredictions
            .take(topN)
            .filter { it.value >= options.confidenceThreshold }

        topPredictions.forEach { prediction ->
            println("SoundClassifier detected bird ${prediction.index}")
            externalScope.launch { _birdEvents.emit(prediction.index to prediction.value) }
        }
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

class SilenceDetector(
    private val durationSeconds: Int,
    private val sampleRate: Int,
    private val threshold: Double = 1e-4
) {
    private val maxSamples: Int = durationSeconds * sampleRate
    private val buffer: FloatBuffer = FloatBuffer.allocate(maxSamples)

    fun addSamples(audioData: ShortArray) {
        // Конвертируем в float [-1..1]
        val floatData = FloatArray(audioData.size) { i -> audioData[i] / 32768f }
        val samplesToWrite = minOf(floatData.size, buffer.remaining())
        buffer.put(floatData, 0, samplesToWrite)
    }

    fun isSilence(): Boolean {
        if (buffer.position() < maxSamples) return false // ещё не накопили
        buffer.flip()
        val samples = FloatArray(buffer.remaining())
        buffer.get(samples)
        buffer.clear()
        val rmsValue = rms(samples)
        return abs(rmsValue) <= threshold
    }

    private fun rms(samples: FloatArray): Double {
        var sum = 0.0
        for (s in samples) {
            sum += s * s
        }
        return sqrt(sum / samples.size)
    }
}