package ru.raydroid.plugin.host.impl.data.plugin

import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.host.api.domain.model.PluginId

data class RemotePlugin(
    val data: ByteArray,
    val signature: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RemotePlugin

        if (!data.contentEquals(other.data)) return false
        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + (signature?.hashCode() ?: 0)
        return result
    }
}