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
    val hotReloadEnabled = System.getProperty("compose.reload.isActive") == "true"
    application {
        val windowState = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(600.dp, 400.dp)
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            alwaysOnTop = true,
            resizable = false,
            undecorated = !hotReloadEnabled, // disabling on hot reload to properly move window so IDE properly visible
            transparent = !hotReloadEnabled,
            title = "KotlinTest",
        ) {
            App()
        }
    }
}