package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.SystemAccessPermission
import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge
import ru.raydroid.plugin.api.host.exception.PermissionDenied

internal class PermissionSystemServiceBridge(
    private val systemServiceBridge: SystemServiceBridge,
    private val manifest: Manifest
) : SystemServiceBridge {
    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> {
        if (manifest.access.system?.permissions?.contains(SystemAccessPermission.GetApps) != true) {
            throw PermissionDenied()
        }
        if (manifest.access.system?.visibleApps?.isEmpty() == true) {
            return emptyList()
        }
        val visibleApps = manifest.access.system?.visibleApps?.let {
            visibleApps(
                it,
                systemServiceBridge.getApps()
            )
        }
        if (visibleApps != null) return visibleApps
        return emptyList()
    }

    private fun visibleApps(
        allowedApps: List<String>,
        apps: List<SystemServiceBridge.RawApplication>
    ): List<SystemServiceBridge.RawApplication> {
        val allowedAppsRegexes = allowedApps.map { it.toRegex() }
        return apps.filter { app ->
            allowedAppsRegexes.any { it.matches(app.id) }
        }
    }

    override suspend fun open(
        app: SystemServiceBridge.RawApplication,
        options: SystemServiceBridge.OpenOptions
    ) {
        if (manifest.access.system?.permissions?.contains(SystemAccessPermission.OpenApp) != true) {
            throw PermissionDenied()
        }
        val allowedApps = manifest.access.system?.visibleApps ?: emptyList()
        val allowedAppsRegex = allowedApps.map { it.toRegex() }
        if (allowedAppsRegex.any { it.matches(app.id) }) {
            systemServiceBridge.open(app, options)
        } else {
            throw PermissionDenied()
        }
    }

    private fun hasAccessToUrl(allowedUrls: List<String>, url: String): Boolean {
        return allowedUrls.any { allowedUrl ->
            allowedUrl.toRegex().matches(url)
        }
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions
    ) {
        if (manifest.access.system?.permissions?.contains(SystemAccessPermission.OpenResource) != true) {
            throw PermissionDenied()
        }
        if (manifest.access.system?.allowedUrls?.let { hasAccessToUrl(it, target) } == true) {
            systemServiceBridge.open(target, options)
        } else {
            throw PermissionDenied()
        }
    }

}