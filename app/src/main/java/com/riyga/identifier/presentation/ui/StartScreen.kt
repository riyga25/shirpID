package com.riyga.identifier.presentation.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.riyga.identifier.R
import com.riyga.identifier.utils.isPermissionGranted

@Composable
fun StartScreen(
    onStart: () -> Unit = {}
) {
    val context = LocalContext.current

    var isAudioGranted by remember {
        mutableStateOf(
            isPermissionGranted(
                context,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    var isFineLocationGranted by remember {
        mutableStateOf(
            isPermissionGranted(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Обновляем состояние соответствующего разрешения
                when {
                    !isAudioGranted -> isAudioGranted = true
                    !isFineLocationGranted -> isFineLocationGranted = true
                }
                showSettingsDialog = false
            } else {
                // Проверяем, может ли система снова показать диалог разрешения
                showSettingsDialog = when {
                    !isAudioGranted -> !ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        Manifest.permission.RECORD_AUDIO
                    )

                    !isFineLocationGranted -> !ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )

                    else -> false
                }
            }
        }

    val isAllPermissionsGranted = isAudioGranted && isFineLocationGranted
    val lifecycleOwner = LocalLifecycleOwner.current

    // Обновление состояния разрешений при изменении жизненного цикла
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Проверяем все разрешения, когда экран становится активным
                isAudioGranted = isPermissionGranted(
                    context,
                    Manifest.permission.RECORD_AUDIO
                )

                isFineLocationGranted = isPermissionGranted(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    Scaffold { paddings ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            if (!isAudioGranted) {
                OutlinedButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                ) {
                    Text(text = "Разрешить запись аудио")
                }
            }

            if (!isFineLocationGranted) {
                OutlinedButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                ) {
                    Text(text = "Разрешить доступ к местоположению")
                }
            }

            if (isAllPermissionsGranted) {
                Box(
                    Modifier.rippleLoadingAnimationModifier(
                        isActive = true,
                        color = MaterialTheme.colorScheme.primary,
                        expandFactor = 5f,
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
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
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(64.dp)
                        )
                    }
                }
                Text(
                    text = "Sound ID",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (showSettingsDialog) {
        Dialog(
            onDismissRequest = { showSettingsDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Открыть настройки приложения для управления разрешениями?")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    TextButton(
                        onClick = { showSettingsDialog = false }
                    ) {
                        Text("Отмена")
                    }
                    TextButton(
                        onClick = {
                            openAppSettings()
                            showSettingsDialog = false
                        }
                    ) {
                        Text("Перейти")
                    }
                }
            }
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
    StartScreen() {}
}