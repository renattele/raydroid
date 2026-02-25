package ru.raydroid.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import raydroid.plugin.host.generated.resources.Res
import ru.raydroid.App
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.Plugin
import ru.raydroid.plugin.host.PluginLoadResult
import ru.raydroid.plugin.host.PluginLoaderImpl
import java.util.concurrent.Executors

fun main() {
    val executorService = Executors.newSingleThreadExecutor {
        Thread(
            /* group = */ null,
            /* task = */ it,
            /* name = */ "Zipline",
            // Need increased stack size because otherwise we get stack overflow on really simple extensions
            /* stackSize = */ 512000
        )
    }
    val dispatcher = executorService.asCoroutineDispatcher()
    val queryFlow = MutableStateFlow("")
    val nodeFlow = MutableStateFlow<RayItems>(mapOf())
    val pluginFlow  = MutableStateFlow<Plugin?>(null)
    val pluginLoader = PluginLoaderImpl(dispatcher)
    CoroutineScope(Dispatchers.IO).launch {
        val resource = Res.readBytes("files/calculator.rext")
        when (val result = pluginLoader.loadPlugin(resource)) {
            is PluginLoadResult.Failure -> {
                result.exception.printStackTrace()
            }

            is PluginLoadResult.Success -> {
                val plugin = result.plugin
                pluginFlow.value = plugin
                plugin.commandServices.forEach { command ->
                    command.initialize(object : CommandServiceBridge.RenderRequest {
                        override fun requestRender() {
                            nodeFlow.value = command.content()
                        }
                    })
                }
                queryFlow.collect { query ->
                    plugin.commandServices.forEach { command ->
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
            val plugin by pluginFlow.collectAsState()
            plugin?.let { plugin ->
                App(
                    field = query,
                    data = nodes,
                    plugin = plugin,
                    onFieldUpdate = { queryFlow.value = it })
            }
        }
    }
}