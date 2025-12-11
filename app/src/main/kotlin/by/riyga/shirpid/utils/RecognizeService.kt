package by.riyga.shirpid.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import by.riyga.shirpid.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RecognizeService: Service() {

    companion object {
        const val ACTION_STOP = "STOP_ACTION"

        const val CHANNEL_ID = "sound_recorder_channel"

        private val _birdsEvents = MutableSharedFlow<Pair<String, Float>>()
        val birdsEvents = _birdsEvents.asSharedFlow()
        
        // Store the audio file path
        var audioFilePath: String? = null
            private set
    }

    private val binder = LocalBinder()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var soundClassifier: SoundClassifier? = null

    inner class LocalBinder : Binder() {
        fun getService(): RecognizeService {
            return this@RecognizeService
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        soundClassifier?.stop(false)
    }

    fun stop(saveRecording: Boolean) {
        audioFilePath = soundClassifier?.stop(saveRecording)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun startForegroundService(latitude: Float, longitude: Float) {
        scope.launch {
            soundClassifier = SoundClassifier(
                context = this@RecognizeService,
                externalScope = scope
            )

            createNotificationChannel()

            val notification = createNotification("Подслушиваем пичуг...")
            startForeground(1, notification)

            soundClassifier?.runMetaInterpreter(
                longitude = longitude,
                latitude = latitude
            )
            soundClassifier?.start()

            soundClassifier?.birdEvents?.collect {
                _birdsEvents.emit(it)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            this.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Birds recognition service"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val stopIntent = Intent(this, RecognizeService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sound Recognition")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .build()
    }

    private fun showDetectionNotification(bird: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bird detected!")
            .setContentText(bird)
            .setSmallIcon(R.drawable.ic_mic)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }
}