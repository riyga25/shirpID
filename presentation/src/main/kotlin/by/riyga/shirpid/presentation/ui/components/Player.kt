package by.riyga.shirpid.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.riyga.shirpid.presentation.player.PlayerState
import by.riyga.shirpid.presentation.utils.AnalyticsUtil


@Composable
fun Player(
    modifier: Modifier = Modifier,
    mediaState: PlayerState,
    fileName: String? = null,
    onPause: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    Box(
        Modifier
            .background(
                colorScheme.tertiary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (mediaState.progress > 0) {
            val progressValue = if (mediaState.duration != 0L) {
                mediaState.progress.toFloat() / mediaState.duration
            } else 0f

            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (mediaState.isPlaying) {
                    Icons.Default.PauseCircle
                } else {
                    Icons.Default.PlayCircle
                },
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            AnalyticsUtil.logEvent("click_play_stop_button")
                            if (mediaState.isPlaying) {
                                onPause()
                            } else {
                                onPlay()
                            }
                        }
                    ),
                tint = colorScheme.tertiary
            )
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = fileName ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${formatTime(mediaState.progress)}/${formatTime(mediaState.duration)}",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Player(
        mediaState = PlayerState(
            progress = 1L,
            duration = 12345
        ),
        fileName = "any name"
    )
}

private fun formatTime(millis: Long?): String {
    if (millis == null) return ""

    val timeMin = millis / 60000
    val timeSec = (millis / 1000) % 60

    val minString = if (timeMin < 10) "0$timeMin" else "$timeMin"
    val secString = if (timeSec < 10) "0$timeSec" else "$timeSec"

    return "$minString:$secString"
}