package by.riyga.shirpid.presentation.ui.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.LabelsRepository
import by.riyga.shirpid.data.location.LocationRepository
import by.riyga.shirpid.presentation.models.IdentifiedBird
import by.riyga.shirpid.presentation.player.AudioPlayer
import by.riyga.shirpid.presentation.player.PlayerState
import by.riyga.shirpid.presentation.utils.BaseViewModel
import by.riyga.shirpid.presentation.utils.SoundClassifier
import by.riyga.shirpid.presentation.utils.UiEffect
import by.riyga.shirpid.presentation.utils.UiEvent
import by.riyga.shirpid.presentation.utils.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs

class FileViewModel(
    private val locationRepository: LocationRepository,
    private val labelsRepository: LabelsRepository,
    private val soundClassifier: SoundClassifier,
    private val audioPlayer: AudioPlayer
) : BaseViewModel<FileContract.State, FileContract.Effect, FileContract.Event>() {
    private var selectedUri: Uri? = null
    private val modelInputLength = 144000

    val mediaState: StateFlow<PlayerState> = audioPlayer.playerState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val location = locationRepository.getCurrentLocation()

            val options = SoundClassifier.Options(
                latitude = location?.latitude?.toFloat() ?: -1F,
                longitude = location?.longitude?.toFloat() ?: -1F
            )

            soundClassifier.initializeModels(options)
        }
    }

    override fun createInitialState(): FileContract.State = FileContract.State()

    override fun onCleared() {
        super.onCleared()
        soundClassifier.releaseModels()
        audioPlayer.release()
    }

    fun onFileSelected(context: Context, uri: Uri?) {
        viewModelScope.launch {
            selectedUri = uri
            uri?.let {
                // Получаем имя файла
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        val fileName = cursor.getString(nameIndex)
                        setState { copy(fileName = fileName) }
                    }
                }

                launch {
                    processFile(context)
                }
                prepareAudioPlayer(it.toString())
            }
        }
    }

    fun prepareAudioPlayer(uri: String) {
        audioPlayer.prepare(uri)
    }

    fun pauseAudioPlayer() {
        audioPlayer.pause()
    }

    fun playAudioPlayer() {
        audioPlayer.start()
    }

    private suspend fun processFile(context: Context) {
        selectedUri?.let { uri ->
            processFileByChunks(
                context = context,
                uri = uri
            ) {
                saveIdentifications(it.chunk, it.result)
            }
        }
    }

    private suspend fun processFileByChunks(
        context: Context,
        uri: Uri,
        onChunkProcessed: (ChunkResult) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Читаем заголовок и определяем размер файла
                val header = ByteArray(44)
                inputStream.read(header)
                val fileSize = getFileSize(context.contentResolver, uri) // Получаем размер файла

                val sampleRate = ByteBuffer.wrap(header, 24, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                val channels = header[22].toInt() and 0xFF
                val bitsPerSample = header[34].toInt() and 0xFF

                val seconds = 3.0f
                val chunkSize = (sampleRate * seconds).toInt()
                val bytesPerSample = 2
                val estimatedChunks =
                    (fileSize / (chunkSize * bytesPerSample * channels)).coerceAtLeast(1)

                val pcmReadBuffer = ByteArray(4096 * bytesPerSample)
                val chunkBuffer = ShortArray(chunkSize)

                var chunkIndex = 0
                var bufferedSamples = 0
                var totalBytesRead = 44L

                while (true) {
                    val bytesRead = inputStream.read(pcmReadBuffer)
                    if (bytesRead <= 0) break

                    totalBytesRead += bytesRead

                    // Обновляем прогресс каждые 4096 байт
                    val progressPercent =
                        ((totalBytesRead * 100) / fileSize).toInt().coerceAtMost(99)
                    setState { copy(progressPercent = progressPercent) }

                    val samplesRead = bytesRead / bytesPerSample
                    var sampleIndex = 0

                    while (sampleIndex < samplesRead) {
                        val offset = sampleIndex * bytesPerSample
                        val lsb = pcmReadBuffer[offset].toInt() and 0xFF
                        val msb = pcmReadBuffer[offset + 1].toInt() shl 8
                        val sample = (msb or lsb).toShort()

                        chunkBuffer[bufferedSamples] = sample
                        bufferedSamples++

                        if (bufferedSamples == chunkSize) {
                            val result = processChunk(chunkBuffer.copyOf())
                            onChunkProcessed(ChunkResult(chunkIndex, result))

                            bufferedSamples = 0
                            chunkIndex++
                        }
                        sampleIndex++
                    }
                }

                setState { copy(progressPercent = 100) }

                // Process remaining samples
                if (bufferedSamples > 0) {
                    val paddedChunk = chunkBuffer.copyOf().also { chunk ->
                        for (i in bufferedSamples until chunkSize) {
                            chunk[i] = 0
                        }
                    }
                    val result = processChunk(paddedChunk)
                    onChunkProcessed(ChunkResult(chunkIndex, result))
                }
            }
        }
    }

    private fun processChunk(chunk: ShortArray): List<Pair<Int, Float>> {
        val processedChunk = if (chunk.size != modelInputLength) {
            val resizedChunk = ShortArray(modelInputLength) { 0 }
            val copySize = minOf(chunk.size, modelInputLength)
            System.arraycopy(chunk, 0, resizedChunk, 0, copySize)
            resizedChunk
        } else {
            chunk
        }

        val maxAmp = processedChunk.maxOf { abs(it.toInt()) }.coerceAtLeast(1)
        val fb = FloatBuffer.allocate(modelInputLength)

        for (i in processedChunk.indices) {
            val normalizedValue = processedChunk[i].toFloat() / maxAmp
            fb.put(normalizedValue)
        }
        fb.rewind()

        return soundClassifier.classify(fb).map { it.index to it.value }
    }

    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: uri.toFile().length()
        } catch (e: Exception) {
            1024 * 1024 * 10L // Fallback: 10MB
        }
    }

    override fun handleEvent(event: FileContract.Event) {
        super.handleEvent(event)
        when (event) {
            is FileContract.Event.SaveIdentifications -> {

            }
        }
    }

    private fun saveIdentifications(
        chunk: Int,
        result: List<Pair<Int, Float>>
    ) {
        if (result.isNotEmpty()) {
            val currentList = uiState.value.birds.toMutableMap()
            currentList[chunk] = result.map {
                IdentifiedBird(
                    index = it.first,
                    name = labelsRepository.getLabel(it.first),
                    confidence = it.second
                )
            }
            setState { copy(birds = currentList) }
        }
    }
}

class FileContract {
    data class State(
        val loading: Boolean = false,
        val birds: Map<Int, List<IdentifiedBird>> = emptyMap(),
        val fileName: String = "",
        val progressPercent: Int = 0
    ) : UiState

    sealed class Effect : UiEffect

    sealed class Event : UiEvent {
        data class SaveIdentifications(val chunk: Int, val result: List<Pair<Int, Float>>) : Event()
    }
}

data class ChunkResult(
    val chunk: Int,
    val result: List<Pair<Int, Float>>
)