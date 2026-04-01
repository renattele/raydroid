package ru.raydroid.plugin.host.api.domain.service

import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.host.transport.HostServiceBridge
import ru.raydroid.plugin.host.api.domain.model.PluginId

interface HostBridgeFactory {
    fun get(pluginId: PluginId, manifest: Manifest): HostServiceBridge
}
