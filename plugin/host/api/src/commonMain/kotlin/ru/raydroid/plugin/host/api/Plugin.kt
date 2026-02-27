package ru.raydroid.plugin.host.api

import kotlin.jvm.JvmInline

data class Plugin(
    val data: ByteArray,
    val signature: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Plugin

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

private val PLUGIN_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$".toRegex()

@JvmInline
value class PluginId(
    val id: String
) {
    init {
        require(id.matches(PLUGIN_REGEX)) {
            "Invalid plugin id: $id. It should match this schema: com.example.myplugin"
        }
    }
}