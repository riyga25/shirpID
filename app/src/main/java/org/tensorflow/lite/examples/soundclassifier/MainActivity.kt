package org.tensorflow.lite.examples.soundclassifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_PERMISSIONS = 1337
    }

    private lateinit var soundClassifier: SoundClassifier
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()

        soundClassifier = SoundClassifier(
            context = this,
            externalScope = lifecycleScope
        )

//        binding.fab.setOnClickListener {
//            if (soundClassifier.isRecording.value) {
//                binding.fab.setImageDrawable(
//                    ContextCompat.getDrawable(
//                        this,
//                        R.drawable.ic_play_24dp
//                    )
//                )
//                soundClassifier.stop()
//            } else {
//                binding.fab.setImageDrawable(
//                    ContextCompat.getDrawable(
//                        this,
//                        R.drawable.ic_pause_24dp
//                    )
//                )
//                soundClassifier.start()
//            }
//        }
//        binding.bottomNavigationView.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.action_view -> {
//                    intent = Intent(this, ViewActivity::class.java)
//                    startActivity(intent)
//                }
//
//                R.id.action_about -> {
//                    startActivity(
//                        Intent(
//                            Intent.ACTION_VIEW,
//                            Uri.parse("https://github.com/woheller69/whobird")
//                        )
//                    )
//                }
//
//                R.id.action_settings -> {
//                    intent = Intent(this, SettingsActivity::class.java)
//                    startActivity(intent)
//                }
//            }
//            true
//        }

        binding.composeView.setContent {
            var birds by remember {
                mutableStateOf<Set<String>>(emptySet())
            }
            val isRecord by soundClassifier.isRecording.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                soundClassifier.birdEvents
                    .filter { it.second * 100 > 30 }
                    .collect { (bird, _) ->
                        birds = birds + bird
                    }
            }

            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (isRecord) {
                                soundClassifier.stop()
                            } else {
                                soundClassifier.start()
                            }
                        },
                        backgroundColor = Color.DarkGray,
                        contentColor = Color.White
                    ) {
                        val icon = if (isRecord) {
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
                        Column(Modifier) {
                            Row(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = bird.substringAfter("_"))
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            LocationHelper().requestLocation(this@MainActivity, soundClassifier)
        }

        if (!checkLocationPermission()) {
            Toast.makeText(
                this,
                this.resources.getString(R.string.error_location_permission),
                Toast.LENGTH_SHORT
            ).show()
        }
        if (checkMicrophonePermission()) {
            soundClassifier.start()
        } else {
            Toast.makeText(
                this,
                this.resources.getString(R.string.error_audio_permission),
                Toast.LENGTH_SHORT
            ).show()
        }
        keepScreenOn(true)
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            LocationHelper().stopLocation(this@MainActivity)
        }
    }

    private fun checkMicrophonePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            return false
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            return false
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (perms.isNotEmpty()) requestPermissions(perms.toTypedArray(), REQUEST_PERMISSIONS)
    }

    private fun keepScreenOn(enable: Boolean) =
        if (enable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
}
