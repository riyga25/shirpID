package by.riyga.shirpid.presentation.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.exoplayer.ExoPlayer
import by.riyga.shirpid.presentation.utils.setState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal class MediaPlayerController(context: Context) : AudioPlayer {
    private val player = ExoPlayer.Builder(context).build()
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _playerState: MutableStateFlow<PlayerState> by lazy { MutableStateFlow(PlayerState()) }
    override val playerState by lazy { _playerState.asStateFlow() }

    private var playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                STATE_READY -> {
                    _playerState.setState { copy(duration = player.duration) }
                }

                STATE_ENDED -> {
                    player.pause()
                    player.seekTo(0)
                    _playerState.setState {
                        copy(progress = 0, isPlaying = false)
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _playerState.setState { copy(isPlaying = isPlaying) }

            if (isPlaying) {
                scope.launch {
                    startProgressUpdate()
                }
            } else {
                stopProgressUpdate()
            }
        }
    }

    override fun prepare(soundUri: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(soundUri)
            .build()

        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.prepare()

        if (!playerState.value.initialized) {
            player.addListener(playerListener)
        }

        _playerState.setState {
            PlayerState(
                initialized = true,
                title = soundUri,
                duration = player.duration
            )
        }
    }

    override fun start() {
        if (player.playbackState == STATE_ENDED) {
            player.seekTo(0)
        }
        player.play()
    }

    override fun pause() {
        if (player.isPlaying)
            player.pause()
    }

    override fun stop() {
        player.stop()
    }

    override fun release() {
        player.stop()
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    private fun startProgressUpdate() {
        updateJob = scope.launch {
            while (true) {
                delay(1.seconds / 20)
                _playerState.setState { copy(progress = player.currentPosition) }
            }
        }
    }

    private fun stopProgressUpdate() {
        updateJob?.cancel()
    }

    private fun addListener() {
        player.addListener(playerListener)
    }
}