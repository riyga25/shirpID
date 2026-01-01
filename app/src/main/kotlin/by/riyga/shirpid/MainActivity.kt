package by.riyga.shirpid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import by.riyga.shirpid.presentation.ui.ComposeApp
import by.riyga.shirpid.presentation.utils.RecognizeService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeApp()
        }

        initNotifications()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, RecognizeService::class.java))
    }

    private fun initNotifications() {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(RecognizeService.CHANNEL_ID, "Уведомления", importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
