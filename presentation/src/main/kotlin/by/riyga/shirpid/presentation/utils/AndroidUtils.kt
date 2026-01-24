package by.riyga.shirpid.presentation.utils

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Utility functions for Android platform operations
 */
object AndroidUtils {
    /**
     * Checks if permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Opens app settings screen
     */
    fun openAppSettings(context: Context) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).also {
            context.startActivity(it)
        }
    }

    /**
     * Shares content via Intent
     */
    fun share(
        context: Context,
        subject: Uri,
        chooserText: String?
    ) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, subject)
            type = "audio/x-wav"
        }
        val shareIntent = Intent.createChooser(sendIntent, chooserText)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(context, shareIntent, null)
    }

    /**
     * Updates app locale
     */
    fun updateAppLocale(context: Context, languageCode: String) {
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
            localeManager.applicationLocales = LocaleList(locale)
        } else {
            @Suppress("DEPRECATION")
            val config = context.resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    /**
     * Checks if audio file exists at URI
     */
    fun isAudioExists(context: Context, uri: String?): Boolean {
        if (uri == null) return false

        val projection = arrayOf(MediaStore.Audio.Media._ID)

        return try {
            context.contentResolver.query(
                uri.toUri(),
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            println("isAudioExists error ${e.message}")
            false
        }
    }

    /**
     * Deletes audio file at URI
     */
    fun deleteAudio(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
    }
}
