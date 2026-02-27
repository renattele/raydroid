package ru.raydroid.plugin.host.impl.services

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import ru.raydroid.plugin.api.host.bridge.CacheServiceBridge

internal class RuntimeCacheServiceImpl : CacheServiceBridge {
    private val cache = mutableMapOf<String, MutableStateFlow<String?>>().withDefault {
        MutableStateFlow(null)
    }
    private val lock = SynchronizedObject()
    override suspend fun get(key: String): String? = synchronized(lock) {
        cache[key]?.value
    }

    override suspend fun set(key: String, value: String) = synchronized(lock) {
        cache[key]?.value = value
    }

    override suspend fun clear() = synchronized(lock) {
        cache.clear()
    }

    override suspend fun flowOf(key: String): Flow<String?> = synchronized(lock) {
        cache[key] ?: emptyFlow()
    }
}