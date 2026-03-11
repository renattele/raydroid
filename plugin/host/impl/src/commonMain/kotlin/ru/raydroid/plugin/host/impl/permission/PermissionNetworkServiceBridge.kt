package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.readable
import ru.raydroid.plugin.api.host.bridge.NetworkServiceBridge
import ru.raydroid.plugin.api.host.exception.PermissionDenied

internal class PermissionNetworkServiceBridge(
    private val networkServiceBridge: NetworkServiceBridge,
    private val manifest: Manifest
): NetworkServiceBridge {
    override suspend fun request(request: NetworkServiceBridge.RawNetworkRequest): NetworkServiceBridge.RawNetworkResponse {
        if (manifest.access.network?.permissions?.readable() == true) {
            if (manifest.access.network?.allowedUrls?.let { hasAccess(it, request.url) } == true) {
                return networkServiceBridge.request(request)
            }
        }
        throw PermissionDenied()
    }

    private fun hasAccess(allowedUrls: List<String>, url: String): Boolean {
        return allowedUrls.any { allowedUrl ->
            allowedUrl.toRegex().matches(url)
        }
    }
}