package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.api.host.transport.SearchFieldServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.writable

internal class PermissionSearchFieldServiceBridge(
    private val searchFieldServiceBridge: SearchFieldServiceBridge,
    private val manifest: Manifest
) : SearchFieldServiceBridge {
    override suspend fun setState(state: SearchFieldState) {
        if (manifest.access.searchField?.permissions?.writable() != true) {
            throw PermissionDenied()
        }
        searchFieldServiceBridge.setState(state)
    }
}
