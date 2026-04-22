package ru.raydroid.plugin.api.runtime

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.presentation.CommandActionId
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.presentation.CommandPresentationMap
import ru.raydroid.plugin.api.ui.Ray
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.RayScope

@Serializable
sealed class CommandAction {
    @Serializable
    class OpenCommand : CommandAction()

    @Serializable
    class CloseCommand : CommandAction()

    @Serializable
    data class Enter(val hoveredId: CommandItemId) : CommandAction()

    @Serializable
    data class ExecuteAction(
        val itemId: CommandItemId,
        val actionId: CommandActionId
    ) : CommandAction()

    @Serializable
    class Type : CommandAction()
}

interface CommandServiceBridge : ZiplineService {
    fun getServiceName(): String
    suspend fun cachedItems(
        requestedItems: List<CommandItemId>? = null,
        chunkSize: Int = 100
    ): Flow<List<CommandListItem>>

    fun content(): CommandPresentationMap

    fun fullscreen(): List<RayNodeData>

    fun actions(target: CommandActionTarget): List<CommandListAction>

    fun initialize(
        request: RenderRequest,
        fullscreenRenderRequest: FullscreenRenderRequest,
        invalidateCacheRequest: InvalidateCacheRequest
    )

    suspend fun update(query: String, action: CommandAction)

    interface RenderRequest : ZiplineService {
        fun requestRender()
    }

    interface InvalidateCacheRequest : ZiplineService {
        fun requestInvalidation(invalidatedIds: List<CommandItemId>? = null)
    }

    interface FullscreenRenderRequest : ZiplineService {
        fun requestFullscreenRender()
    }
}

abstract class CommandService {
    open suspend fun cachedItems(
        requestedItems: List<CommandItemId>? = null,
        chunkSize: Int = 100
    ): Flow<List<CommandListItem>> =
        emptyFlow()

    @Ray
    abstract fun CommandListScope.content()

    @Ray
    open fun RayScope.fullscreen() = Unit

    @Ray
    open fun CommandActionScope.actions(target: CommandActionTarget) = Unit

    abstract suspend fun execute(action: CommandAction)

    fun render() {
        val onRenderRequest = onRenderRequest
        if (onRenderRequest != null) {
            onRenderRequest()
        }
    }

    fun renderFullscreen() {
        val onFullscreenRenderRequest = onFullscreenRenderRequest
        if (onFullscreenRenderRequest != null) {
            onFullscreenRenderRequest()
        }
    }

    fun invalidateCache(invalidatedIds: List<CommandItemId>? = null) {
        onInvalidateCacheRequest?.invoke(invalidatedIds)
    }

    val query: String
        get() = _query

    suspend fun update(query: String, action: CommandAction) {
        _query = query
        execute(action)
    }

    internal var onRenderRequest: (() -> Unit)? = null

    internal var onFullscreenRenderRequest: (() -> Unit)? = null

    internal var onInvalidateCacheRequest: ((List<CommandItemId>?) -> Unit)? = null

    private var _query: String = ""
}
