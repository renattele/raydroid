package ru.raydroid.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.raydroid.App

fun main() = application {
    val field = remember { mutableStateOf("") }
    val windowState = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(600.dp, 200.dp)
    )
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        alwaysOnTop = true,
        resizable = false,
        undecorated = true,
        transparent = true,
        title = "KotlinTest",
    ) {
        App(field.value, mutableStateOf(mapOf()), onFieldUpdate = { field.value = it })
    }
}