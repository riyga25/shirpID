package org.tensorflow.lite.examples.soundclassifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.filter

class MainActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private lateinit var soundClassifier: SoundClassifier
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
//                showToast(R.string.permissions_granted)
                soundClassifier.start()
            } else {
                handleDeniedPermissions(permissions)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionsIfNeeded()

        soundClassifier = SoundClassifier(
            context = this,
            externalScope = lifecycleScope
        )

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            LocationHelper().requestLocation(this@MainActivity, soundClassifier)
        }

        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            showToast(R.string.error_location_permission)
        }

        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            soundClassifier.start()
        } else {
            showToast(R.string.error_audio_permission)
        }

        keepScreenOn(true)
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            LocationHelper().stopLocation(this@MainActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundClassifier.stop()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissionsIfNeeded() {
        val missingPermissions = REQUIRED_PERMISSIONS.filterNot { hasPermission(it) }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun handleDeniedPermissions(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filterValues { !it }.keys
        if (deniedPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
            showToast(R.string.error_audio_permission)
        }
        if (deniedPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            showToast(R.string.error_location_permission)
        }
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
    }

    private fun keepScreenOn(enable: Boolean) {
        if (enable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setupUI() {
        binding.composeView.setContent {
            var birds by remember { mutableStateOf<Set<String>>(emptySet()) }
            val isRecording by soundClassifier.isRecording.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                soundClassifier.birdEvents
                    .filter { it.second * 100 > 30 }
                    .collect { (bird, _) -> birds = birds + bird }
            }

            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (isRecording) {
                                soundClassifier.stop()
                            } else {
                                soundClassifier.start()
                            }
                        },
                        backgroundColor = Color.DarkGray,
                        contentColor = Color.White
                    ) {
                        val icon = if (isRecording) {
                            R.drawable.ic_pause_24dp
                        } else {
                            R.drawable.ic_play_24dp
                        }
                        Icon(painterResource(icon), "play/pause")
                    }
                }
            ) { paddings ->
                LazyColumn(
                    contentPadding = PaddingValues(bottom = paddings.calculateBottomPadding()),
                    modifier = Modifier.statusBarsPadding()
                ) {
                    items(birds.toList()) { bird ->
                        BirdRow(bird = bird)
                    }
                }
            }
        }
    }
}

@Composable
fun BirdRow(bird: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = bird.substringAfter("_"))
        }
        Divider()
    }
}
