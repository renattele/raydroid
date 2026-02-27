package ru.raydroid.desktop

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import ru.raydroid.App
import ru.raydroid.composeapp.appModule
import java.util.concurrent.Executors

fun main() {
    startKoin {
        modules(appModule)
    }
    val executorService = Executors.newSingleThreadExecutor {
        Thread(
            /* group = */ null,
            /* task = */ it,
            /* name = */ "Zipline",
            // Need increased stack size because otherwise we get stack overflow on really simple extensions
            /* stackSize = */ 512000
        )
    }
    application {
        val windowState = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
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
            App()
        }
    }
}