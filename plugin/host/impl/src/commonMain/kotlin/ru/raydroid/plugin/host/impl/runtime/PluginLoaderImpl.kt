package ru.raydroid.plugin.host.impl.runtime

import app.cash.zipline.loader.DefaultFreshnessCheckerNotFresh
import app.cash.zipline.loader.LoadResult
import app.cash.zipline.loader.ManifestVerifier
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.openZip
import okio.use
import ru.raydroid.plugin.api.ZiplineServices
import ru.raydroid.plugin.api.runtime.CommandServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.host.api.domain.service.HostBridgeFactory
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.api.domain.model.PluginDescriptor
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime

internal val REXT_VIRTUAL_FS_PATH = "/plugin.rext".toPath()
internal val REXT_UNPACKED_VIRTUAL_FS_PATH = "/plugin".toPath()

// Need baseurl because otherwise loader won't accept url
internal const val NONEXISTENT_URL = "https://nonexistent.jkjk"

internal class PluginLoaderImpl(
    private val dispatcher: () -> CoroutineDispatcher,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
    private val coroutineScope: CoroutineScope,
    private val hostFactory: HostBridgeFactory
) : PluginLoader {
    override suspend fun loadPlugin(plugin: PluginArtifact): PluginRuntime {
        val dispatcher = dispatcher()
        val fs = pluginFs(plugin)
        val httpClient = RextZiplineHttpClient(fs)
        val loader = ZiplineLoader(
            dispatcher,
            manifestVerifier = ManifestVerifier.NO_SIGNATURE_CHECKS,
            httpClient = httpClient
        )
        return withContext(dispatcher) {
            val manifest = loadManifest(httpClient)
            val loadResult = loader.loadOnce(
                applicationName = "$NONEXISTENT_URL/manifest.zipline.json",
                freshnessChecker = DefaultFreshnessCheckerNotFresh,
                manifestUrl = "$NONEXISTENT_URL/manifest.zipline.json"
            )
            val pluginId = PluginId(manifest.name)

            when (loadResult) {
                is LoadResult.Failure -> throw loadResult.exception
                is LoadResult.Success -> {
                    val zipline = loadResult.zipline
                    val host = hostFactory.get(pluginId, manifest)
                    zipline.bind(ZiplineServices.Host.toString(), host)
                    val services = manifest.commands.map { command ->
                        zipline.take<CommandServiceBridge>(command.service)
                    }
                    return@withContext PluginRuntimeImpl(
                        manifest = manifest,
                        commandServices = services,
                        resources = fs,
                        zipline = zipline,
                        pluginRuntimeDispatcher = dispatcher,
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
    }

    override suspend fun loadPluginMetadata(plugin: PluginArtifact): PluginDescriptor {
        val pluginFs = pluginFs(plugin)
        val httpClient = RextZiplineHttpClient(pluginFs)
        val manifest = loadManifest(httpClient)
        return object : PluginDescriptor {
            override val pluginId: PluginId = PluginId(manifest.name)
            override val manifest: Manifest = manifest
            override val resources: FileSystem = pluginFs
        }
    }

    override suspend fun join(plugins: StateFlow<List<PluginRuntime>>): PluginRuntimeCoordinator {
        return PluginRuntimeCoordinatorImpl(
            coroutineScope = coroutineScope,
            pluginRuntimes = plugins
        )
    }

    private suspend fun loadManifest(
        httpClient: RextZiplineHttpClient
    ): Manifest {
        val data =
            httpClient.download("$NONEXISTENT_URL/resources/plugin-manifest.json", emptyList())
        return json.decodeFromString<Manifest>(data.toByteArray().decodeToString())
    }

    private fun pluginFs(
        plugin: PluginArtifact
    ): FileSystem {
        val fs = FakeFileSystem()
        loadPluginToFs(fs, plugin)
        return fs
    }

    private fun loadPluginToFs(
        fs: FileSystem,
        plugin: PluginArtifact
    ) {
        if (!fs.exists(REXT_VIRTUAL_FS_PATH)) {
            fs.write(REXT_VIRTUAL_FS_PATH) {
                write(plugin.data)
            }
            fs.unpackZip(REXT_VIRTUAL_FS_PATH, REXT_UNPACKED_VIRTUAL_FS_PATH)
        }
    }
}

internal fun FileSystem.unpackZip(zipFile: Path, destDir: Path) {
    fun Path.createParentDirectories() {
        this.parent?.let { parent ->
            createDirectories(parent)
        }
    }

    val zipFileSystem = openZip(zipFile)
    val paths = zipFileSystem.listRecursively("/".toPath())
        .filter { zipFileSystem.metadata(it).isRegularFile }
        .toList()

    paths.forEach { zipFilePath ->
        zipFileSystem.source(zipFilePath).buffer().use { source ->
            val relativeFilePath = zipFilePath.toString().trimStart('/')
            val fileToWrite = destDir.resolve(relativeFilePath)
            fileToWrite.createParentDirectories()
            sink(fileToWrite).buffer().use { sink ->
                sink.writeAll(source)
            }
        }
    }
}
