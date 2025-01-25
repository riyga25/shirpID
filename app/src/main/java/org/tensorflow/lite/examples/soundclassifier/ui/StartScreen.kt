package org.tensorflow.lite.examples.soundclassifier.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.soundclassifier.R

@Composable
fun StartScreen(
    onStart: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = scaffoldState.snackbarHostState

    val isAudioGranted = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    )
    val isFineLocationGranted =
        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    val isCoarseLocationGranted =
        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            val isAllGranted = map[Manifest.permission.RECORD_AUDIO] == true && (
                    map[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            map[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    )

            if (isAllGranted) {
                onStart.invoke()
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Location and audio permissions are required")
                }
            }
        }

    Layout(
        onStart = {
            if (!isAudioGranted || !isFineLocationGranted || !isCoarseLocationGranted) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        },
        scaffoldState = scaffoldState
    )
}

@Composable
private fun Layout(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    onStart: () -> Unit = {}
) {
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(it) }
    ) { paddings ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            Box(
                Modifier.rippleLoadingAnimationModifier(
                    isActive = true,
                    color = MaterialTheme.colors.primary,
                    expandFactor = 5f,
                )
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary, CircleShape)
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = onStart
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "Start record",
                        tint = MaterialTheme.colors.onPrimary,
                        modifier = Modifier
                            .size(64.dp)
                    )
                }
            }
            Text(
                text = "Sound ID",
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colors.onPrimary
            )

        }
    }
}

fun Modifier.rippleLoadingAnimationModifier(
    isActive: Boolean,
    color: Color,
    circles: Int = 3,
    expandFactor: Float = 5f,
    durationMillis: Int = 5000,
): Modifier {
    if (!isActive) return this

    return composed {
        val transition = rememberInfiniteTransition(label = "ripple")

        val translateAnimations = List(circles) { index ->
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * (durationMillis / circles))
                ),
                label = "rippleCircle$index"
            )
        }

        this.drawBehind {
            val maxRadius = (maxOf(size.height, size.width) / 2) * expandFactor
            translateAnimations.forEach { animatable ->
                val alpha = 1f - animatable.value
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = maxRadius * animatable.value,
                    center = size.center
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Layout()
}