package by.riyga.shirpid.presentation.ui.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.riyga.shirpid.presentation.R
import by.riyga.shirpid.presentation.ui.components.BackButton
import by.riyga.shirpid.presentation.ui.components.BirdScaffold
import by.riyga.shirpid.presentation.ui.components.BirdTopAppBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LocationScreen() {
    val viewModel: LocationViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Layout()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Layout(
    onBack: () -> Unit = {},
) {
    BirdScaffold(
        topBar = {
            BirdTopAppBar(
                title = "Задать местоположение",
                onBack = onBack
            )
        }
    ) { paddings ->
        Column(
            modifier = Modifier.padding(top = paddings.calculateTopPadding())
        ) {

        }
    }
}