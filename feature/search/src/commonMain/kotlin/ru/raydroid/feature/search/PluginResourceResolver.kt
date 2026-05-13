package ru.raydroid.feature.search

import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.manifest.Resources
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginUiText

class PluginResourceResolver(
    private val plugins: Map<PluginId, PluginRuntime>,
    private val language: String
) {
    fun resolveText(text: PluginUiText): String = when (text) {
        is PluginUiText.Plain -> text.text
        is PluginUiText.Resource -> {
            val runtime = plugins[text.pluginId]
            runtime?.let {
                resolveLocalizedString(it.manifest.resources, text.key, language)
            } ?: text.key
        }
    }

    fun resolveIcon(icon: PluginIcon): ResolvedPluginAsset? = when (icon) {
        is PluginIcon.Url -> ResolvedPluginAsset.RemoteUrl(icon.url)
        is PluginIcon.Base64 -> ResolvedPluginAsset.Base64Data(icon.base64)
        is PluginIcon.Resource -> plugins[icon.pluginId]
            ?.let { runtime -> readBinaryResource(runtime.resources, icon.key) }
            ?.let(ResolvedPluginAsset::BinaryData)
        is PluginIcon.Builtin -> ResolvedPluginAsset.BuiltinName(icon.name)
    }

    fun resolveImage(image: PluginImage): ResolvedPluginAsset? = when (image) {
        is PluginImage.Url -> ResolvedPluginAsset.RemoteUrl(image.url)
        is PluginImage.Resource -> plugins[image.pluginId]
            ?.let { runtime -> readBinaryResource(runtime.resources, image.key) }
            ?.let(ResolvedPluginAsset::BinaryData)
    }
}

sealed interface ResolvedPluginAsset {
    data class RemoteUrl(val url: String) : ResolvedPluginAsset
    data class Base64Data(val base64: String) : ResolvedPluginAsset
    data class BinaryData(val bytes: ByteArray) : ResolvedPluginAsset
    data class BuiltinName(val name: String) : ResolvedPluginAsset
}

private fun resolveLocalizedString(
    resources: Resources,
    key: String,
    language: String
): String? {
    val localizedBucket = resources["strings-$language"]
    val defaultBucket = resources[DEFAULT_STRINGS_BUCKET]
    return localizedBucket?.get(key) ?: defaultBucket?.get(key)
}

private fun readBinaryResource(
    resources: FileSystem,
    key: String
): ByteArray? {
    return try {
        resources.read("plugin/resources/$key".toPath()) {
            readByteArray()
        }
    } catch (_: IOException) {
        null
    }
}

private const val DEFAULT_STRINGS_BUCKET = "strings"
