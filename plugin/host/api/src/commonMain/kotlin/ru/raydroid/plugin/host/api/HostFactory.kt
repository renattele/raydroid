package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.host.bridge.HostServiceBridge

interface HostFactory {
    fun get(pluginId: PluginId): HostServiceBridge
}