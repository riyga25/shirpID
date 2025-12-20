package by.riyga.shirpid.presentation.utils

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

object AnalyticsUtil {

    fun logEvent(eventName: String) {
        Firebase.analytics.logEvent(eventName, null)
    }

    fun screenView(screenName: String) {
        println("AnalyticsUtil screenView $screenName")
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
    }
}