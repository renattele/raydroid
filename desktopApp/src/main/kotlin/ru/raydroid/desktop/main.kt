package ru.raydroid.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.cash.zipline.loader.ManifestVerifier
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.raydroid.App
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.PluginLoadResult
import ru.raydroid.plugin.host.PluginLoaderImpl
import ru.raydroid.plugin.host.ResourceZiplineHttpClient
import java.util.concurrent.Executors

fun main() {
    val executorService = Executors.newSingleThreadExecutor {
        Thread(
            /* group = */ null,
            /* task = */ it,
            /* name = */ "Zipline",
            // Need increased stack size because otherwise we get stack overflow on really simple extensions
            /* stackSize = */ 2048
        )
    }
    val dispatcher = executorService.asCoroutineDispatcher()
    val queryFlow = MutableStateFlow("")
    val nodeFlow = MutableStateFlow<RayItems>(mapOf())
    val loader = ZiplineLoader(
        dispatcher = dispatcher,
        manifestVerifier = ManifestVerifier.NO_SIGNATURE_CHECKS,
        httpClient = ResourceZiplineHttpClient("calculator.rext")
    )
    val pluginLoader = PluginLoaderImpl(dispatcher, loader)
    CoroutineScope(Dispatchers.IO).launch {
        val result = pluginLoader.loadPlugin("https://a.com/manifest.zipline.json")
        when (result) {
            is PluginLoadResult.Failure -> {
                result.exception.printStackTrace()
            }

            is PluginLoadResult.Success -> {
                result.commandServices.forEach { command ->
                    command.initialize(object : CommandServiceBridge.RenderRequest {
                        override fun requestRender() {
                            nodeFlow.value = command.content()
                        }
                    })
                }
                queryFlow.collect { query ->
                    result.commandServices.forEach { command ->
                        command.update(query, CommandAction.Type())
                    }
                }
            }
        }
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
            val query by queryFlow.collectAsState()
            val nodes by nodeFlow.collectAsState()
            App(query, nodes, onFieldUpdate = { queryFlow.value = it })
        }
    }
}