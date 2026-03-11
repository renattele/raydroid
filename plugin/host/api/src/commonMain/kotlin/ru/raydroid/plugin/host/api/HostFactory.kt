package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.host.bridge.HostServiceBridge

interface HostFactory {
    fun get(pluginId: PluginId, manifest: Manifest): HostServiceBridge
}