package ru.raydroid.desktop

import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.raydroid.App

fun main() = application {
    val field = remember { mutableStateOf("") }
    Window(onCloseRequest = {
        exitApplication()
    }) {
        App(field.value, mutableStateOf(mapOf()), onFieldUpdate = { field.value = it })
    }
}