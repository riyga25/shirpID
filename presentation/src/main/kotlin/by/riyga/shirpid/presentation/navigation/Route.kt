package by.riyga.shirpid.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Start : Route

    @Serializable
    data object Progress : Route

    @Serializable
    data object Archive : Route

    @Serializable
    data class Record(
        val recordId: Long? = null,
        val fromArchive: Boolean = false,
        val fileUri: String? = null
    ) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object License : Route
}