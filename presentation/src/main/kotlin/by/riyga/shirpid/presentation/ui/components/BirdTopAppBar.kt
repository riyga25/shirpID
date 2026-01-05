package by.riyga.shirpid.presentation.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdTopAppBar(
    title: String? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            if (title != null) {
                Text(title)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                BackButton(onClick = onBack)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}