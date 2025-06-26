package com.riyga.identifier.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.utils.LocalNavController
import com.riyga.identifier.utils.composableWithArgs
import com.riyga.identifier.utils.navType
import kotlinx.serialization.Serializable

@Composable
fun ComposeApp() {
    val navController = rememberNavController()

    MaterialTheme {
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            AppNavHost(
                navController = navController
            )
        }
    }
}

sealed interface AppDestination {
    @Serializable
    data object Start : AppDestination

    @Serializable
    data class Progress(val location: LocationData) : AppDestination
}

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = AppDestination.Start) {
        composable<AppDestination.Start> {
            StartScreen(
                onStart = { location ->
                    navController.navigate(
                        AppDestination.Progress(location)
                    )
                }
            )
        }

        composableWithArgs<AppDestination.Progress>(
            navType<LocationData>()
        ) { backStackEntry ->
            ProgressScreen(
                location = backStackEntry.toRoute<AppDestination.Progress>().location
            )
        }
    }
}
