package ru.raydroid.plugin.host

import app.cash.zipline.loader.DefaultFreshnessCheckerNotFresh
import app.cash.zipline.loader.LoadResult
import app.cash.zipline.loader.ManifestVerifier
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.Manifest

class PluginLoaderImpl(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }
) : PluginLoader {
    override suspend fun loadPlugin(pluginBytes: ByteArray): PluginLoadResult {
        val httpClient = RextZiplineHttpClient(pluginBytes.toByteString())
        val loader = ZiplineLoader(
            dispatcher,
            manifestVerifier = ManifestVerifier.NO_SIGNATURE_CHECKS,
            httpClient = httpClient
        )
        return withContext(dispatcher) {
            // Need baseurl because otherwise loader won't accept url
            val baseUrl = "https://nonexistent.jkjk"
            val manifest = loadManifest(httpClient, baseUrl)
            val loadResult = loader.loadOnce(
                applicationName = "$baseUrl/manifest.zipline.json",
                freshnessChecker = DefaultFreshnessCheckerNotFresh,
                manifestUrl = "$baseUrl/manifest.zipline.json"
            )

            when (loadResult) {
                is LoadResult.Failure -> {
                    return@withContext PluginLoadResult.Failure(loadResult.exception)
                }

                is LoadResult.Success -> {
                    val zipline = loadResult.zipline
                    val services = manifest.commands.map { command ->
                        zipline.take<CommandServiceBridge>(command.service)
                    }
                    return@withContext PluginLoadResult.Success(
                        Plugin(
                            manifest = manifest,
                            commandServices = services,
                            resources = httpClient.fs
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadManifest(
        httpClient: RextZiplineHttpClient,
        baseUrl: String
    ): Manifest {
        val data = httpClient.download("$baseUrl/resources/plugin-manifest.json", emptyList())
        return json.decodeFromString<Manifest>(data.toByteArray().decodeToString())
    }
}