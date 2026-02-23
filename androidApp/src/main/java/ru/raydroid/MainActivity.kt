package ru.raydroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import app.cash.zipline.loader.ManifestVerifier
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.host.PluginLoadResult
import ru.raydroid.plugin.host.PluginLoaderImpl
import ru.raydroid.plugin.host.startPlugin
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val executorService = Executors.newSingleThreadExecutor { Thread(it, "Zipline") }
        val dispatcher = executorService.asCoroutineDispatcher()
        val queryFlow = MutableStateFlow("")
        val nodeFlow = MutableStateFlow<RayItems>(mapOf())
        val loader = ZiplineLoader(
            dispatcher = dispatcher,
            manifestVerifier = ManifestVerifier.NO_SIGNATURE_CHECKS,
            httpClient = OkHttpClient()
        )
        val pluginLoader = PluginLoaderImpl(dispatcher, loader)
        CoroutineScope(Dispatchers.IO).launch {
            val result = pluginLoader.loadPlugin("http://192.168.0.149:8080/manifest.zipline.json")
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
        setContent {
            val query by queryFlow.collectAsState()
            val nodes = nodeFlow.collectAsState()
            App(query, nodes, onFieldUpdate = { queryFlow.value = it })
        }
    }
}