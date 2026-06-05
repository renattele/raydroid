package ru.raydroid.plugin.host.api.ui

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.host.api.domain.model.PluginId

fun UiText.toPluginUiText(pluginId: PluginId): PluginUiText =
    when (type) {
        UiText.Type.Plain -> PluginUiText.Plain(text)
        UiText.Type.Resource -> PluginUiText.Resource(pluginId = pluginId, key = text)
    }

fun PluginUiText?.orUnknown(): PluginUiText = this ?: PluginUiText.Plain("ERROR UNKNOWN")
