package com.riyga.identifier.utils

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
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.channels.Channels
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sqrt

class SoundClassifier(
    private val context: Context,
    private val options: Options = Options(),
    private val externalScope: CoroutineScope
) {

    companion object {
        private const val TAG = "SoundClassifier"
    }

    private val _birdEvents = MutableSharedFlow<Pair<String, Float>>(extraBufferCapacity = 1)
    val birdEvents: SharedFlow<Pair<String, Float>> get() = _birdEvents

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private lateinit var labelList: List<String>

    private lateinit var interpreter: Interpreter
    private lateinit var metaInterpreter: Interpreter

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

    // Для антидребезга
//    private var lastLabel: String? = null
//    private var lastTime = 0L

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
                _isRecording.value = false

                val filePath = if (saveRecording) wavRecorder?.stopRecording() else null
                if (!saveRecording) wavRecorder?.cancelRecording()
                return filePath
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio record: ${e.message}")
                return null
            }
        }
    }

    private fun initialize() {
        loadLabels()
        interpreter = setupInterpreter(options.modelPath, ::onInterpreterReady)
        metaInterpreter = setupInterpreter(options.metaModelPath, ::onMetaInterpreterReady)
        warmUpModel()
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

    private fun loadLabels() {
        try {
            val labels =
                context.assets.open(options.labelsFile).bufferedReader().use { it.readLines() }
            labelList = labels.map { it.trim().replaceFirstChar { ch -> ch.uppercase() } }
            Log.i(TAG, "Loaded ${labelList.size} labels from ${options.labelsFile}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load labels: ${e.message}")
        }
    }

    private fun loadModelFromAssets(fileName: String): ByteBuffer {
        val inputStream = context.assets.open(fileName)
        val fileSize = inputStream.available()
        return ByteBuffer.allocateDirect(fileSize).order(ByteOrder.nativeOrder()).apply {
            Channels.newChannel(inputStream).read(this)
            rewind()
        }
    }

    private fun onInterpreterReady(
        interpreter: Interpreter,
        inputShape: IntArray,
        outputShape: IntArray
    ) {
        modelInputLength = inputShape[1]
        modelNumClasses = outputShape[1]
        predictionProbs = FloatArray(modelNumClasses) { Float.NaN }
    }

    private fun onMetaInterpreterReady(
        interpreter: Interpreter,
        inputShape: IntArray,
        outputShape: IntArray
    ) {
        metaModelNumClasses = outputShape[1]
        metaPredictionProbs = FloatArray(metaModelNumClasses) { 1f }
        metaInputBuffer = FloatBuffer.allocate(inputShape[1])
    }

    private fun setupInterpreter(
        modelPath: String,
        onReady: (Interpreter, IntArray, IntArray) -> Unit
    ): Interpreter {
        val buffer = loadModelFromAssets(modelPath)
        val interpreter = Interpreter(buffer, Interpreter.Options())
        onReady(
            interpreter,
            interpreter.getInputTensor(0).shape(),
            interpreter.getOutputTensor(0).shape()
        )
        return interpreter
    }

    fun runMetaInterpreter(latitude: Float, longitude: Float) {
        try {
//            val dayOfYear = 160
            val dayOfYear = LocalDate.now().dayOfYear
            val weekMeta = cos(Math.toRadians(dayOfYear * 7.5)) + 1.0

            metaInputBuffer.put(0, latitude)
            metaInputBuffer.put(1, longitude)
            metaInputBuffer.put(2, weekMeta.toFloat())
            metaInputBuffer.rewind()

            val metaOutputBuffer = FloatBuffer.allocate(metaModelNumClasses)
            metaInterpreter.run(metaInputBuffer, metaOutputBuffer)
            metaOutputBuffer.rewind()
            metaOutputBuffer.get(metaPredictionProbs)

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

                silenceDetector.addSamples(recordingBuffer)

                if (!silenceDetector.isSilence()) {
                    val inputBuffer = normalize(circularBuffer, bufferIndex)
                    val outputBuffer = FloatBuffer.allocate(modelNumClasses)
                    interpreter.run(inputBuffer, outputBuffer)
                    processModelOutput(outputBuffer)
                    delay(inferenceInterval)
                } else {
                    delay(200) // быстро проверяем тишину, но не нагружаем CPU
                }
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

        // 1. Сортируем все предсказания по убыванию уверенности
        val sortedPredictions = probList.withIndex()
            .map { IndexedValue(it.index, it.value) } // Убедимся, что это IndexedValue<Float>
            .sortedByDescending { it.value }

        // 2. Определяем, сколько топовых результатов мы хотим взять (например, 3)
        val topN = 3 // Вы можете сделать это настраиваемым через options

        // 3. Берем N лучших предсказаний, которые проходят порог уверенности
        val topPredictions = sortedPredictions
            .take(topN)
            .filter { it.value >= options.confidenceThreshold }

        // 4. Обрабатываем каждое из выбранных предсказаний
        topPredictions.forEachIndexed { i, prediction ->
            val label = labelList[prediction.index]
            val confidence = prediction.value

            externalScope.launch { _birdEvents.emit(label to confidence) }
        }

//        val max = probList.withIndex().maxByOrNull { it.value }
//        max?.let {
//            val label = labelList[it.index]
//            val confidence = it.value
//
//            if (confidence >= options.confidenceThreshold &&
//                (label != lastLabel || System.currentTimeMillis() - lastTime > options.antiDebounceMs)
//            ) {
//                lastLabel = label
//                lastTime = System.currentTimeMillis()
//                externalScope.launch { _birdEvents.emit(label to confidence) }
//            }
//        }
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
        val labelsFile: String = "labels_ru.txt",
        val modelPath: String = "BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite",
        val metaModelPath: String = "BirdNET_GLOBAL_6K_V2.4_MData_Model_V2_FP16.tflite",
        val sampleRate: Int = 48000,
        val warmupRuns: Int = 3,
        val metaProbabilityThreshold1: Float = 0.01f,
        val metaProbabilityThreshold2: Float = 0.008f,
        val metaProbabilityThreshold3: Float = 0.001f,
        val confidenceThreshold: Float = 0.4f
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