package ru.raydroid.plugin.host.impl.resource

import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.manifest.Resources

internal fun resolveLocalizedString(
    resources: Resources,
    key: String,
    language: String,
): String? {
    val localizedBucket = resources["strings-$language"]
    val defaultBucket = resources[DEFAULT_STRINGS_BUCKET]
    return localizedBucket?.get(key) ?: defaultBucket?.get(key)
}

internal fun resolveStringVariants(
    resources: Resources,
    key: String,
): Map<String, String> {
    val stringBuckets =
        resources.filterKeys { bucketName ->
            bucketName.startsWith(STRINGS_BUCKET_PREFIX)
        }
    val defaultBucket = stringBuckets[DEFAULT_STRINGS_BUCKET]
    val resolved = linkedMapOf<String, String>()

    stringBuckets.forEach { (bucketName, bucketResources) ->
        val value = bucketResources[key] ?: defaultBucket?.get(key)
        if (value != null) {
            resolved[bucketName] = value
        }
    }

    if (resolved.isEmpty()) {
        resolved[DEFAULT_STRINGS_BUCKET] = key
    } else if (defaultBucket != null && defaultBucket[key] != null && DEFAULT_STRINGS_BUCKET !in resolved) {
        resolved[DEFAULT_STRINGS_BUCKET] = defaultBucket.getValue(key)
    }

    return resolved
}

internal fun readBinaryResource(
    resources: FileSystem,
    key: String,
): ByteArray? =
    try {
        resources.read("plugin/resources/$key".toPath()) {
            readByteArray()
        }
    } catch (_: IOException) {
        null
    }

private const val STRINGS_BUCKET_PREFIX = "strings"
private const val DEFAULT_STRINGS_BUCKET = "strings"
