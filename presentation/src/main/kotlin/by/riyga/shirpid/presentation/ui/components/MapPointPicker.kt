package by.riyga.shirpid.presentation.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import by.riyga.shirpid.presentation.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

@Composable
fun MapPointPicker(
    initLatitude: Double? = null,
    initLongitude: Double? = null,
    onSelected: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var mapView: MapView? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName;
    }

    LaunchedEffect(initLatitude, initLongitude) {
        if (initLatitude != null && initLongitude != null) {
            mapView?.controller?.setCenter(GeoPoint(initLatitude, initLongitude))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }

    BackHandler {
        onDismiss()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    this.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    mapView = this

                    if(isDarkTheme) {
                        val matrix = floatArrayOf(
                            -0.4f, -0.9f, -0.05f, 0f, 320f,
                            -0.25f, -1.0f, -0.08f, 0f, 320f,
                            -0.35f, -0.8f, -0.15f, 0f, 320f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        val colorMatrix = ColorMatrix(matrix)
                        val colorFilter = ColorMatrixColorFilter(colorMatrix)
                        overlayManager.tilesOverlay.setColorFilter(colorFilter)
                    }
                }
            },
            update = { map ->
                // Обновление центра при необходимости (наводка на локацию)
            }
        )
        Icon(
            painterResource(R.drawable.ic_location_pin),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
                .offset(y = (-18).dp),
            tint = MaterialTheme.colorScheme.onBackground
        )

        BackButton(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            onDismiss()
        }

        if (initLatitude != null && initLongitude != null) {
            IconButton(
                onClick = {
                    mapView?.controller?.apply {
                        setCenter(GeoPoint(initLatitude, initLongitude))
                        setZoom(15.0)
                    }

                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        MaterialTheme.colorScheme.background,
                        CircleShape
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Icon(
                    painterResource(R.drawable.ic_my_location),
                    contentDescription = "to my location",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Button(
            onClick = {
                val center = mapView!!.mapCenter
                onSelected(center.latitude, center.longitude)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Text("Готово", modifier = Modifier.padding(8.dp))
        }
    }
}