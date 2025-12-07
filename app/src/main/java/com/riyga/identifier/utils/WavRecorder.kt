package com.riyga.identifier.utils

import android.content.ContentValues
import android.content.Context
import android.media.AudioRecord
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val APP_NAME = "SoundClassifier"
private const val FILE_NAME_FORMAT = "yyyy-MM-dd HH_mm_ss"

class WavRecorder(
    private val audioRecord: AudioRecord,
    private val context: Context,
    private val sampleRate: Int = 48000,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16
) {
    private var outputStream: FileOutputStream? = null
    private var temporaryFile: File? = null
    private var isRecording = false
    private var totalAudioDataSize = 0
    private val audioLock = Any()

    /**
     * Start recording audio to a temporary file.
     */
    fun startRecording() {
        synchronized(audioLock) {
            if (isRecording) return

            try {
                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioRecord is not initialized")
                }

                // Создаем временный файл для записи
                temporaryFile = File(context.cacheDir, "temp_audio_recording.wav")
                outputStream = FileOutputStream(temporaryFile!!).apply {
                    writeWavHeader(this, 0) // Пишем временный заголовок
                }

                isRecording = true
                Log.i(
                    "WavRecorder",
                    "Recording started. Temporary file: ${temporaryFile?.absolutePath}"
                )
            } catch (e: Exception) {
                Log.e("WavRecorder", "Error starting recording: ${e.message}")
            }
        }
    }

    /**
     * Stop recording and save the file to permanent storage.
     * Returns the file path of the saved file.
     */
    fun stopRecording(): String? {
        synchronized(audioLock) {
            if (!isRecording) return null

            try {
                outputStream?.let {
                    updateWavHeader(it, totalAudioDataSize) // Обновляем заголовок WAV
                    it.flush()
                    it.close()
                }

                // Переносим временный файл в постоянное хранилище
                val filePath = saveTemporaryFileToMediaStore()

                Log.i("WavRecorder", "Recording saved successfully.")
                return filePath
            } catch (e: Exception) {
                Log.e("WavRecorder", "Error stopping recording: ${e.message}")
                return null
            } finally {
                cleanup()
            }
        }
    }

    /**
     * Cancel the recording and delete the temporary file.
     */
    fun cancelRecording() {
        synchronized(audioLock) {
            if (!isRecording) return

            try {
                outputStream?.close() // Закрываем поток
                temporaryFile?.delete() // Удаляем временный файл
                Log.i("WavRecorder", "Recording canceled and temporary file deleted.")
            } catch (e: Exception) {
                Log.e("WavRecorder", "Error canceling recording: ${e.message}")
            } finally {
                cleanup()
            }
        }
    }

    /**
     * Write audio data to the temporary file.
     */
    fun writeAudioDataLoop(buffer: ShortArray, bytesRead: Int) {
        synchronized(audioLock) {
            if (!isRecording || outputStream == null) {
                Log.e("WavRecorder", "OutputStream is not initialized or recording is not started!")
                return
            }

            val byteArray = buffer.toByteArray(bytesRead)
            try {
                outputStream?.write(byteArray)
                totalAudioDataSize += byteArray.size
            } catch (e: IOException) {
                Log.e("WavRecorder", "Error writing PCM data: ${e.message}")
            }
        }
    }

    /**
     * Save the temporary file to MediaStore as a WAV file.
     * Returns the file path of the saved file.
     */
    private fun saveTemporaryFileToMediaStore(): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "${getCurrentTimestamp()}.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/$APP_NAME")
        }

        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { output ->
                temporaryFile?.inputStream()?.copyTo(output)
            }
            // Return the URI as a string
            return uri.toString()
        }
        temporaryFile?.delete() // Удаляем временный файл
        return null
    }

    /**
     * Write a WAV header with placeholder values.
     */
    private fun writeWavHeader(outputStream: OutputStream, dataSize: Int) {
        val header = ByteArray(44)

        // RIFF chunk descriptor
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        val chunkSize = 36 + dataSize
        header.setIntLittleEndian(4, chunkSize)

        // Format
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        // Subchunk1Size (16 for PCM)
        header.setIntLittleEndian(16, 16)

        // Audio format (1 for PCM)
        header.setShortLittleEndian(20, 1)

        // Number of channels (1 for mono)
        header.setShortLittleEndian(22, channels)

        // Sample rate
        header.setIntLittleEndian(24, sampleRate)

        // Byte rate (SampleRate * NumChannels * BitsPerSample / 8)
        val byteRate = sampleRate * channels * bitsPerSample / 8
        header.setIntLittleEndian(28, byteRate)

        // Block align (NumChannels * BitsPerSample / 8)
        header.setShortLittleEndian(32, channels * bitsPerSample / 8)

        // Bits per sample (16)
        header.setShortLittleEndian(34, bitsPerSample)

        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        // Subchunk2Size (NumSamples * NumChannels * BitsPerSample / 8)
        header.setIntLittleEndian(40, dataSize)

        outputStream.write(header)
    }

    private fun updateWavHeader(outputStream: FileOutputStream, dataSize: Int) {
        outputStream.channel.use {
            it.position(0)
            writeWavHeader(outputStream, dataSize)
        }
    }

    private fun ShortArray.toByteArray(size: Int): ByteArray {
        val byteArray = ByteArray(size * 2)
        for (i in 0 until size) {
            byteArray[i * 2] = (this[i].toInt() and 0xFF).toByte()
            byteArray[i * 2 + 1] = ((this[i].toInt() shr 8) and 0xFF).toByte()
        }
        return byteArray
    }

    private fun ByteArray.setIntLittleEndian(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
        this[offset + 2] = ((value shr 16) and 0xFF).toByte()
        this[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun ByteArray.setShortLittleEndian(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat(FILE_NAME_FORMAT, Locale.getDefault())
        return formatter.format(Date())
    }

    private fun cleanup() {
        outputStream = null
        temporaryFile = null
        totalAudioDataSize = 0
        isRecording = false
    }
}