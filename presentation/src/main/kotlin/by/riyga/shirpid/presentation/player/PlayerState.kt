package by.riyga.shirpid.presentation.player

data class PlayerState(
    val initialized: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Long = 0L,
    val duration: Long = 0L,
    val title: String = "",
)