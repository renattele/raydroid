package ru.raydroid

import app.cash.zipline.loader.DefaultFreshnessCheckerNotFresh
import app.cash.zipline.loader.LoadResult
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.ZiplineServices
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.ManifestService
import ru.raydroid.plugin.api.ui.RayNodeData

fun startPlugin(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    loader: ZiplineLoader,
    manifestUrl: String,
    query: Flow<String>,
    nodes: MutableStateFlow<List<RayNodeData>>
) {
    scope.launch(dispatcher + SupervisorJob()) {
        val loadResultFlow = loader.load(
            applicationName = "Raydroid",
            freshnessChecker = DefaultFreshnessCheckerNotFresh,
            manifestUrlFlow = flowOf(manifestUrl),
        )

        var previousJob: Job? = null
        loadResultFlow.collect { result ->
            previousJob?.cancel()
            if (result is LoadResult.Failure) {
                result.exception.printStackTrace()
            }

            if (result is LoadResult.Success) {
                val zipline = result.zipline
                val manifestService = zipline.take<ManifestService>(ZiplineServices.Manifest.toString())

                previousJob = launch {
                    val manifest = manifestService.getManifest()
                    manifest.commands.forEach { command ->
                        val service = zipline.take<CommandServiceBridge>(command.service)
                        service.initialize(object : CommandServiceBridge.RenderRequest {
                            override fun requestRender() {
                                nodes.value = service.content()
                            }
                        })
                    }
                    query.collect { query ->
                        manifest.commands.forEach { command ->
                            val service = zipline.take<CommandServiceBridge>(command.service)
                            service.update(query)
                        }
                    }
                }

            }
        }
    }
}