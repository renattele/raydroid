package ru.raydroid.plugin.api.core

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.ui.Ray

@Serializable
sealed class CommandAction {
    @Serializable
    data class Enter(val hoveredId: ItemId) : CommandAction()

    @Serializable
    class Type : CommandAction()
}

interface CommandServiceBridge : ZiplineService {
    fun getServiceName(): String
    suspend fun cachedItems(
        requestedItems: List<ItemId>? = null,
        chunkSize: Int = 100
    ): Flow<List<ListItem>>

    fun content(): RayItems

    fun initialize(request: RenderRequest, invalidateCacheRequest: InvalidateCacheRequest)

    suspend fun update(query: String, action: CommandAction)

    interface RenderRequest : ZiplineService {
        fun requestRender()
    }

    interface InvalidateCacheRequest : ZiplineService {
        fun requestInvalidation(invalidatedIds: List<ItemId>? = null)
    }
}

abstract class CommandService {
    open suspend fun cachedItems(
        requestedItems: List<ItemId>? = null,
        chunkSize: Int = 100
    ): Flow<List<ListItem>> =
        emptyFlow()

    @Ray
    abstract fun RayListScope.content()

    abstract suspend fun execute(action: CommandAction)

    fun render() {
        val onRenderRequest = onRenderRequest
        if (onRenderRequest != null) {
            onRenderRequest()
        }
    }

    fun invalidateCache(invalidatedIds: List<ItemId>? = null) {
        onInvalidateCacheRequest?.invoke(invalidatedIds)
    }

    val query: String
        get() = _query

    suspend fun update(query: String, action: CommandAction) {
        _query = query
        execute(action)
    }

    internal var onRenderRequest: (() -> Unit)? = null

    internal var onInvalidateCacheRequest: ((List<ItemId>?) -> Unit)? = null

    private var _query: String = ""
}
