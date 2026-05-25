package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.application.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginDescriptor
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.repository.PluginRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadRuntimesUseCaseTest {
    @Test
    fun `load runtimes skips plugins excluded from current platform`() =
        runTest {
            val androidPluginId = PluginId("ru.test.android")
            val emptyPluginId = PluginId("ru.test.all")
            val repository =
                FakePluginRepository(
                    pluginIds = listOf(androidPluginId, emptyPluginId),
                    artifacts =
                        mapOf(
                            androidPluginId to PluginArtifact("android".encodeToByteArray()),
                            emptyPluginId to PluginArtifact("all".encodeToByteArray()),
                        ),
                )
            val loader =
                FakePluginLoader(
                    descriptors =
                        mapOf(
                            "android" to descriptor(androidPluginId, listOf(Platform.Android)),
                            "all" to descriptor(emptyPluginId, emptyList()),
                        ),
                )
            val registry = FakePluginRuntimeRegistry()

            LoadRuntimesUseCase(
                pluginRepository = repository,
                pluginRuntimeRegistry = registry,
                pluginLoader = loader,
                hostPlatform = Platform.IOS,
            )()

            assertEquals(listOf(emptyPluginId), registry.loadedPluginIds)
        }
}

private class FakePluginRepository(
    private val pluginIds: List<PluginId>,
    private val artifacts: Map<PluginId, PluginArtifact>,
) : PluginRepository {
    override suspend fun installPlugin(url: String) = Unit

    override suspend fun listInstalledPlugins(): List<PluginId> = pluginIds

    override suspend fun loadPlugin(pluginId: PluginId): PluginArtifact? = artifacts[pluginId]

    override suspend fun deletePlugin(pluginId: PluginId) = Unit
}

private class FakePluginLoader(
    private val descriptors: Map<String, PluginDescriptor>,
) : PluginLoader {
    override suspend fun loadPlugin(plugin: PluginArtifact): PluginRuntime? {
        val descriptor = loadPluginMetadata(plugin)
        return FakeLoadedPluginRuntime(descriptor.pluginId, descriptor.manifest)
    }

    override suspend fun loadPluginMetadata(plugin: PluginArtifact): PluginDescriptor =
        requireNotNull(descriptors[plugin.data.decodeToString()])

    override suspend fun join(plugins: StateFlow<List<PluginRuntime>>): PluginRuntimeCoordinator {
        error("Not used in tests")
    }
}

private class FakePluginRuntimeRegistry : PluginRuntimeRegistry {
    private val runtimes = MutableStateFlow<List<PluginRuntime>>(emptyList())
    val loadedPluginIds = mutableListOf<PluginId>()

    override suspend fun load(runtime: PluginRuntime) {
        loadedPluginIds += runtime.pluginId
        runtimes.value += runtime
    }

    override suspend fun unload(runtime: PluginRuntime) = Unit

    override fun get(): PluginRuntimeCoordinator =
        object : PluginRuntimeCoordinator {
            override fun cachedItems(): Flow<List<SearchIndexMutation>> = flowOf(emptyList())

            override fun runtimes(): StateFlow<List<PluginRuntime>> = runtimes

            override fun content(): StateFlow<List<PluginRuntimeCoordinator.ContentItem>> =
                MutableStateFlow<List<PluginRuntimeCoordinator.ContentItem>>(emptyList())

            override fun commands(): StateFlow<List<PluginRuntimeCoordinator.CommandItem>> =
                MutableStateFlow<List<PluginRuntimeCoordinator.CommandItem>>(emptyList())

            override suspend fun update(action: CommandActionBridge) = Unit
        }
}

private fun descriptor(
    pluginId: PluginId,
    platforms: List<Platform>,
): PluginDescriptor =
    object : PluginDescriptor {
        override val pluginId: PluginId = pluginId
        override val manifest: Manifest = manifest(pluginId, platforms)
        override val resources: FileSystem = FakeFileSystem()
    }

private fun manifest(
    pluginId: PluginId,
    platforms: List<Platform>,
): Manifest =
    Manifest(
        name = pluginId.id,
        title = UiText.Plain(pluginId.id),
        description = UiText.Plain("Test plugin"),
        author = UiText.Plain("Codex"),
        version = 1,
        platforms = platforms,
        categories = emptyList(),
        license = "MIT",
        commands =
            listOf(
                Command(
                    service = "command",
                    title = UiText.Plain("Command"),
                    description = UiText.Plain("Command"),
                    mode = Command.Mode.View,
                    match = null,
                    arguments = emptyList(),
                    preferences = emptyList(),
                ),
            ),
        resources = emptyMap(),
    )

private class FakeLoadedPluginRuntime(
    override val pluginId: PluginId,
    override val manifest: Manifest,
) : PluginRuntime {
    override val resources: FileSystem = FakeFileSystem()

    override fun cachedItems(chunkSize: Int): Flow<List<SearchIndexMutation>> = emptyFlow()

    override suspend fun cachedItems(
        commandName: String,
        requestedItems: List<CommandItemId>,
        chunkSize: Int,
    ): List<SearchIndexMutation> = emptyList()

    override fun content(): StateFlow<List<PluginRuntime.ContentItem>> =
        MutableStateFlow<List<PluginRuntime.ContentItem>>(emptyList())

    override fun fullscreen(commandName: String): StateFlow<PluginRuntime.FullscreenContent?> = MutableStateFlow(null)

    override suspend fun actions(
        commandName: String,
        itemId: CommandItemId,
    ): List<PluginCommandListAction> = emptyList()

    override suspend fun update(action: CommandActionBridge) = Unit

    override suspend fun update(
        commandName: String,
        action: CommandActionBridge,
    ) = Unit

    override suspend fun back(commandName: String): Boolean = false

    override suspend fun unload() = Unit
}
