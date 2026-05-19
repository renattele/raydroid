package ru.raydroid.feature.search

import okio.fakefilesystem.FakeFileSystem
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginUiText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

class PluginResourceResolverTest {
    @Test
    fun `resolver returns localized text and resource bytes`() {
        val pluginId = PluginId("ru.raydroid.calculator")
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories("plugin/resources".toPath())
        val iconBytes = byteArrayOf(1, 2, 3)
        val imageBytes = byteArrayOf(4, 5, 6)
        fileSystem.write("plugin/resources/icon.bin".toPath()) {
            write(iconBytes)
        }
        fileSystem.write("plugin/resources/image.bin".toPath()) {
            write(imageBytes)
        }

        val runtime = ResolverTestRuntime(
            pluginId = pluginId,
            manifest = Manifest(
                name = pluginId.id,
                title = UiText.Plain("Calculator"),
                description = UiText.Plain("Calculator extension"),
                author = UiText.Plain("Raydroid"),
                version = 1,
                platforms = listOf(Platform.IOS),
                categories = emptyList(),
                license = "MIT",
                commands = listOf(
                    Command(
                        service = "calculator",
                        title = UiText.Plain("Calculator"),
                        description = UiText.Plain("Calculator extension"),
                        placeholder = null,
                        mode = Command.Mode.View,
                        match = null,
                        searchable = true,
                        arguments = emptyList(),
                        preferences = emptyList()
                    )
                ),
                resources = mapOf(
                    "strings" to mapOf("title" to "Calculator"),
                    "strings-ru" to mapOf("title" to "Kalkulyator")
                )
            ),
            resources = fileSystem
        )

        val resolver = PluginResourceResolver(
            plugins = mapOf(pluginId to runtime),
            language = "ru"
        )

        assertEquals(
            "Kalkulyator",
            resolver.resolveText(PluginUiText.Resource(pluginId, "title"))
        )
        assertEquals(
            "plain",
            resolver.resolveText(PluginUiText.Plain("plain"))
        )

        val icon = resolver.resolveIcon(PluginIcon.Resource(pluginId, "icon.bin"))
        val image = resolver.resolveImage(PluginImage.Resource(pluginId, "image.bin"))

        assertIs<ResolvedPluginAsset.BinaryData>(icon)
        assertContentEquals(iconBytes, icon.bytes)
        assertIs<ResolvedPluginAsset.BinaryData>(image)
        assertContentEquals(imageBytes, image.bytes)
        assertEquals(
            ResolvedPluginAsset.BuiltinName("Help"),
            resolver.resolveIcon(PluginIcon.Builtin("Help"))
        )
    }
}

private class ResolverTestRuntime(
    override val pluginId: PluginId,
    override val manifest: Manifest,
    override val resources: FakeFileSystem
) : PluginRuntime {
    override fun cachedItems(chunkSize: Int): Flow<List<SearchIndexMutation>> = emptyFlow()
    override suspend fun cachedItems(
        commandName: String,
        requestedItems: List<CommandItemId>,
        chunkSize: Int
    ): List<SearchIndexMutation> = emptyList()

    override fun content(): StateFlow<List<PluginRuntime.ContentItem>> = MutableStateFlow(emptyList())

    override fun fullscreen(commandName: String): StateFlow<PluginRuntime.FullscreenContent?> =
        MutableStateFlow(null)

    override suspend fun actions(
        commandName: String,
        itemId: CommandItemId
    ): List<PluginCommandListAction> = emptyList()

    override suspend fun update(action: CommandActionBridge) = Unit

    override suspend fun update(commandName: String, action: CommandActionBridge) = Unit

    override suspend fun back(commandName: String): Boolean = false

    override suspend fun unload() = Unit
}
