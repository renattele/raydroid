package ru.raydroid

import androidx.compose.ui.window.ComposeUIViewController
import ru.raydroid.composeapp.initKoin

fun MainViewController() = ComposeUIViewController { App() }

fun InitKoin() {
    initKoin()
}
