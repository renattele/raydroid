package ru.raydroid.plugin.host

import app.cash.zipline.loader.DefaultFreshnessCheckerNotFresh
import app.cash.zipline.loader.LoadResult
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.ZiplineServices
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.ManifestService

class PluginLoaderImpl(
    private val dispatcher: CoroutineDispatcher,
    private val loader: ZiplineLoader,
) : PluginLoader {
    override suspend fun loadPlugin(url: String): PluginLoadResult {
        return withContext(dispatcher) {
            val loadResultFlow = loader.load(
                applicationName = url,
                freshnessChecker = DefaultFreshnessCheckerNotFresh,
                manifestUrlFlow = flowOf(url),
            )

            when (val result = loadResultFlow.first()) {
                is LoadResult.Failure -> {
                    return@withContext PluginLoadResult.Failure(result.exception)
                }
                is LoadResult.Success -> {
                    val zipline = result.zipline
                    val manifestService =
                        zipline.take<ManifestService>(ZiplineServices.Manifest.toString())

                    val manifest = manifestService.getManifest()
                    val services = manifest.commands.map { command ->
                        zipline.take<CommandServiceBridge>(command.service)
                    }
                    return@withContext PluginLoadResult.Success(manifest, services)
                }
            }
        }
    }
}