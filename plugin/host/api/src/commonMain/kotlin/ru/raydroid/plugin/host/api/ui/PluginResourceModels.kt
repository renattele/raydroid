package ru.raydroid.plugin.host.api.ui

import ru.raydroid.plugin.host.api.domain.model.PluginId

sealed interface PluginUiText {
    data class Plain(
        val text: String,
    ) : PluginUiText

    data class Resource(
        val pluginId: PluginId,
        val key: String,
    ) : PluginUiText
}

sealed interface PluginIcon {
    data class Url(
        val url: String,
    ) : PluginIcon

    data class Resource(
        val pluginId: PluginId,
        val key: String,
    ) : PluginIcon

    data class Base64(
        val base64: String,
    ) : PluginIcon

    data class Builtin(
        val name: String,
    ) : PluginIcon
}

sealed interface PluginImage {
    data class Url(
        val url: String,
    ) : PluginImage

    data class Resource(
        val pluginId: PluginId,
        val key: String,
    ) : PluginImage
}
