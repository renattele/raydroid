package ru.raydroid.gradle

import org.gradle.api.Project

private const val PLUGIN_IMPL_PATH_PREFIX = ":plugin:impl:"
private const val PLUGIN_IMPL_PACKAGE_PREFIX = "ru.raydroid.plugin.impl"

internal fun Project.pluginPackageName(): String {
    val suffix = if (path.startsWith(PLUGIN_IMPL_PATH_PREFIX)) {
        path.removePrefix(PLUGIN_IMPL_PATH_PREFIX).replace(':', '.')
    } else {
        name
    }
    val packageSuffix = suffix.replace('-', '_')
    return "$PLUGIN_IMPL_PACKAGE_PREFIX.$packageSuffix"
}
