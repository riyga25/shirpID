package by.riyga.shirpid.presentation.utils

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toFile

object FileUtils {
    fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: uri.toFile().length()
        } catch (e: Exception) {
            1024 * 1024 * 10L // Fallback: 10MB
        }
    }
}