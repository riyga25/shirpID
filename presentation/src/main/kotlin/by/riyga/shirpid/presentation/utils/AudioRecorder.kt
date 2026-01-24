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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.FloatBuffer
import kotlin.math.abs

class AudioRecorder(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val soundClassifier: SoundClassifier,
    private val sampleRate: Int = 48000
) {
    companion object {
        private const val TAG = "AudioRecorder"
    }

    private val _birdEvents = MutableSharedFlow<Pair<Int, Float>>(extraBufferCapacity = 1)
    val birdEvents: SharedFlow<Pair<Int, Float>> get() = _birdEvents

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private var audioRecord: AudioRecord? = null
    private var wavManager: WavManager? = null

    private val audioLock = Any()

    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        sampleRate * 2
    )

    fun start() {
        synchronized(audioLock) {
            if (_isRecording.value) return

            try {
                if (audioRecord == null || audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = initializeAudioRecord()
                }

                wavManager = WavManager(context = context, sampleRate = sampleRate)

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

    @SuppressLint("MissingPermission")
    private fun initializeAudioRecord(): AudioRecord {
        val record = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        return record
    }

    private suspend fun startRecognition() {
        val modelInputLength = soundClassifier.modelInputLength
        if (modelInputLength == 0) {
            Log.e(TAG, "Model input length is 0, cannot start recognition")
            return
        }

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

                val inputBuffer = normalize(circularBuffer, bufferIndex, modelInputLength)
                val result = soundClassifier.classify(inputBuffer)

                result.forEach { prediction ->
                    externalScope.launch { _birdEvents.emit(prediction.index to prediction.value) }
                }

                println("lol chunk #$chunk")
                chunk++
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

    private fun normalize(buffer: ShortArray, startIndex: Int, modelInputLength: Int): FloatBuffer {
        val fb = FloatBuffer.allocate(modelInputLength)
        val maxAmp = buffer.maxOf { abs(it.toInt()) }.coerceAtLeast(1)
        for (i in 0 until modelInputLength) {
            fb.put(buffer[(i + startIndex) % modelInputLength].toFloat() / maxAmp)
        }
        fb.rewind()
        return fb
    }
}
