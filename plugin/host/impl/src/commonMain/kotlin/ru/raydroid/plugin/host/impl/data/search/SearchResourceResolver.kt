package ru.raydroid.plugin.host.impl.data.search

import okio.ByteString.Companion.toByteString
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginDescriptor
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.impl.data.plugin.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.ResourcePluginDataSource
import ru.raydroid.plugin.host.impl.resource.readBinaryResource
import ru.raydroid.plugin.host.impl.resource.resolveStringVariants

internal interface SearchResourceResolver {
    fun session(): Session

    interface Session {
        suspend fun resolveContent(
            pluginId: PluginId,
            title: UiText?,
            description: UiText?,
        ): List<ResolvedContent>

        suspend fun resolveIcon(
            pluginId: PluginId,
            icon: Icon?,
        ): Icon?
    }

    data class ResolvedContent(
        val title: String?,
        val description: String?,
    )
}

internal class SearchResourceResolverImpl(
    private val resourcePluginDataSource: ResourcePluginDataSource,
    private val localPluginDataSource: LocalPluginDataSource,
    private val pluginLoader: PluginLoader,
) : SearchResourceResolver {
    override fun session(): SearchResourceResolver.Session {
        val metadataCache = mutableMapOf<PluginId, PluginDescriptor?>()
        return object : SearchResourceResolver.Session {
            override suspend fun resolveContent(
                pluginId: PluginId,
                title: UiText?,
                description: UiText?,
            ): List<SearchResourceResolver.ResolvedContent> {
                val metadata = metadataCache.getOrPutMetadata(pluginId)

                val titleVariants = resolveTextVariants(title, metadata)
                val descriptionVariants = resolveTextVariants(description, metadata)
                val bucketNames =
                    linkedSetOf<String>().apply {
                        addAll(titleVariants.keys)
                        addAll(descriptionVariants.keys)
                    }
                if (bucketNames.isEmpty()) {
                    return listOf(SearchResourceResolver.ResolvedContent(title = null, description = null))
                }

                val defaultTitle =
                    titleVariants[DEFAULT_STRINGS_BUCKET]
                        ?: titleVariants.values.firstOrNull()
                val defaultDescription =
                    descriptionVariants[DEFAULT_STRINGS_BUCKET]
                        ?: descriptionVariants.values.firstOrNull()

                return bucketNames
                    .map { bucketName ->
                        SearchResourceResolver.ResolvedContent(
                            title = titleVariants[bucketName] ?: defaultTitle,
                            description = descriptionVariants[bucketName] ?: defaultDescription,
                        )
                    }.distinct()
            }

            override suspend fun resolveIcon(
                pluginId: PluginId,
                icon: Icon?,
            ): Icon? {
                if (icon == null || icon.type != Icon.Type.Resource) return icon
                val metadata = metadataCache.getOrPutMetadata(pluginId) ?: return icon

                val bytes = readBinaryResource(metadata.resources, icon.value) ?: return icon
                return Icon.Url("data:image/png;base64,${bytes.toByteString().base64()}")
            }
        }
    }

    private suspend fun loadMetadata(pluginId: PluginId): PluginDescriptor? {
        val pluginData =
            resourcePluginDataSource.load(pluginId)
                ?: localPluginDataSource.load(pluginId)
                ?: return null
        return pluginLoader.loadPluginMetadata(PluginArtifact(pluginData))
    }

    private suspend fun MutableMap<PluginId, PluginDescriptor?>.getOrPutMetadata(pluginId: PluginId): PluginDescriptor? {
        if (containsKey(pluginId)) return get(pluginId)
        return loadMetadata(pluginId).also { put(pluginId, it) }
    }

    private fun resolveTextVariants(
        text: UiText?,
        metadata: PluginDescriptor?,
    ): Map<String, String> {
        if (text == null) {
            return emptyMap()
        }

        if (text.type == UiText.Type.Plain || metadata == null) {
            return linkedMapOf(DEFAULT_STRINGS_BUCKET to text.text)
        }

        return resolveStringVariants(metadata.manifest.resources, text.text)
    }

    private companion object {
        const val DEFAULT_STRINGS_BUCKET = "strings"
    }
}
