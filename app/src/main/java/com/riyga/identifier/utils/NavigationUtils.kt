package com.riyga.identifier.utils

import android.os.Bundle
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import com.eygraber.uri.UriCodec
import kotlinx.serialization.json.Json
import kotlin.reflect.KType
import kotlin.reflect.typeOf

val LocalNavController = compositionLocalOf<NavController> {
    error("No NavController found!")
}

inline fun <reified T> customNavType(isNullableAllowed: Boolean = false): NavType<T> =
    object : NavType<T>(isNullableAllowed) {
        override fun get(
            bundle: Bundle,
            key: String
        ): T? {
            val json = bundle.getString(key) ?: return null
            return Json.decodeFromString(json)
        }

        override fun parseValue(value: String): T {
            return Json.decodeFromString(UriCodec.decode(value))
        }

        override fun put(
            bundle: Bundle,
            key: String,
            value: T
        ) {
            bundle.putString(key, Json.encodeToString(value))
        }

        override fun serializeAsValue(value: T): String {
            return UriCodec.encode(Json.encodeToString(value))
        }
    }

inline fun <reified T> navType(nullable: Boolean = false): Pair<KType, NavType<*>> =
    typeOf<T>() to customNavType<T>(nullable)

fun typeMapOf(vararg types: Pair<KType, NavType<*>>): Map<KType, NavType<*>> = mapOf(*types)

inline fun <reified T : Any> NavGraphBuilder.composableWithArgs(
    vararg types: Pair<KType, NavType<*>>,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(
        typeMap = typeMapOf(*types),
        content = content
    )
}

@Composable
inline fun <reified T> NavController.getResult(
    key: String = "navigation_result",
    initialValue: T
): State<T>? {
    return currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<T>(key, initialValue)
        ?.collectAsStateWithLifecycle()
}

fun <T> NavController.setResult(
    key: String = "navigation_result",
    value: T
) {
    previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
}

fun <T> NavController.clearResult(key: String) {
    currentBackStackEntry?.savedStateHandle?.remove<T>(key)
}