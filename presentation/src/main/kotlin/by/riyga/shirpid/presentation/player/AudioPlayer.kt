package by.riyga.shirpid.presentation.player

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val playerState: StateFlow<PlayerState>

    fun prepare(soundUri: String)

    fun start()

    fun pause()

    fun stop()

    fun isPlaying(): Boolean

    fun release()
}