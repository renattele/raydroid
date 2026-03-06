package ru.raydroid.plugin.host.impl

import okio.ByteString.Companion.toByteString
import okio.IOException
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.Plugin
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.PluginLoader
import ru.raydroid.plugin.host.api.PluginMetadata
import ru.raydroid.plugin.host.impl.datasource.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.datasource.ResourcePluginDataSource

internal interface SearchResourceResolver {
    fun session(): Session

    interface Session {
        suspend fun resolveContent(
            pluginId: PluginId,
            title: UiText,
            description: UiText
        ): List<ResolvedContent>

        suspend fun resolveIcon(pluginId: PluginId, icon: Icon): Icon
    }

    data class ResolvedContent(
        val title: String,
        val description: String
    )
}

internal class SearchResourceResolverImpl(
    private val resourcePluginDataSource: ResourcePluginDataSource,
    private val localPluginDataSource: LocalPluginDataSource,
    private val pluginLoader: PluginLoader
) : SearchResourceResolver {
    override fun session(): SearchResourceResolver.Session {
        val metadataCache = mutableMapOf<PluginId, PluginMetadata?>()
        return object : SearchResourceResolver.Session {
            override suspend fun resolveContent(
                pluginId: PluginId,
                title: UiText,
                description: UiText
            ): List<SearchResourceResolver.ResolvedContent> {
                val metadata = metadataCache.getOrPutMetadata(pluginId)

                val titleVariants = resolveTextVariants(title, metadata)
                val descriptionVariants = resolveTextVariants(description, metadata)
                val bucketNames = linkedSetOf<String>().apply {
                    addAll(titleVariants.keys)
                    addAll(descriptionVariants.keys)
                }
                val defaultTitle = titleVariants[DEFAULT_STRINGS_BUCKET]
                    ?: titleVariants.values.firstOrNull()
                    ?: title.text
                val defaultDescription = descriptionVariants[DEFAULT_STRINGS_BUCKET]
                    ?: descriptionVariants.values.firstOrNull()
                    ?: description.text

                return bucketNames.map { bucketName ->
                    SearchResourceResolver.ResolvedContent(
                        title = titleVariants[bucketName] ?: defaultTitle,
                        description = descriptionVariants[bucketName] ?: defaultDescription
                    )
                }.distinct()
            }

            override suspend fun resolveIcon(pluginId: PluginId, icon: Icon): Icon {
                if (icon.type != Icon.Type.Resource) return icon
                val metadata = metadataCache.getOrPutMetadata(pluginId) ?: return icon

                return try {
                    val bytes = metadata.resource.read("plugin/resources/${icon.value}".toPath()) {
                        readByteArray()
                    }
                    Icon.Base64(bytes.toByteString().base64())
                } catch (_: IOException) {
                    icon
                }
            }
        }
    }

    private suspend fun loadMetadata(pluginId: PluginId): PluginMetadata? {
        val pluginData = resourcePluginDataSource.load(pluginId)
            ?: localPluginDataSource.load(pluginId)
            ?: return null
        return pluginLoader.loadPluginMetadata(Plugin(pluginData))
    }

    private suspend fun MutableMap<PluginId, PluginMetadata?>.getOrPutMetadata(
        pluginId: PluginId
    ): PluginMetadata? {
        if (containsKey(pluginId)) return get(pluginId)
        return loadMetadata(pluginId).also { put(pluginId, it) }
    }

    private fun resolveTextVariants(
        text: UiText,
        metadata: PluginMetadata?
    ): Map<String, String> {
        if (text.type == UiText.Type.Plain || metadata == null) {
            return linkedMapOf(DEFAULT_STRINGS_BUCKET to text.text)
        }

        val stringBuckets = metadata.manifest.resources
            .filterKeys { key -> key.startsWith(STRINGS_BUCKET_PREFIX) }
        val defaultBucket = stringBuckets[DEFAULT_STRINGS_BUCKET]
        val resolved = linkedMapOf<String, String>()

        stringBuckets.forEach { (bucketName, resources) ->
            val value = resources[text.text] ?: defaultBucket?.get(text.text)
            if (value != null) {
                resolved[bucketName] = value
            }
        }

        if (resolved.isEmpty()) {
            resolved[DEFAULT_STRINGS_BUCKET] = text.text
        } else if (defaultBucket != null && defaultBucket[text.text] != null) {
            if (!resolved.containsKey(DEFAULT_STRINGS_BUCKET)) {
                resolved[DEFAULT_STRINGS_BUCKET] = defaultBucket.getValue(text.text)
            }
        }

        return resolved
    }

    private companion object {
        const val STRINGS_BUCKET_PREFIX = "strings"
        const val DEFAULT_STRINGS_BUCKET = "strings"
    }
}
