package by.riyga.shirpid.presentation.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.collections.take
import kotlin.math.exp

class SoundClassifier(
    private val context: Context
) {
    private var options: Options = Options()

    companion object {
        private const val TAG = "ShirpID"
    }

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

    var isModelsInitialized = false
        private set


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
        val warmupRuns: Int = 3,
        val confidenceThreshold: Float = 0.3f,
        val latitude: Float = -1F,
        val longitude: Float = -1F,
        val week: Float = -1F,
    )
}