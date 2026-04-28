package ru.raydroid.plugin.api.runtime

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandCallbackId
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
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
    data class Type(val query: String) : CommandAction()
}

@Serializable
sealed interface CommandActionBridge {
    @Serializable
    data class Regular(val action: CommandAction) : CommandActionBridge

    @Serializable
    data class Internal(val action: InternalCommandActionBridge) : CommandActionBridge
}

@Serializable
sealed interface InternalCommandActionBridge {
    @Serializable
    data class Click(val callback: CommandCallbackRef) : InternalCommandActionBridge
}

interface CommandServiceBridge : ZiplineService {
    fun getServiceName(): String
    suspend fun cachedItems(
        requestedItems: List<CommandItemId>? = null,
        chunkSize: Int = 100
    ): Flow<List<CommandListItem>>

    fun content(): CommandPresentationMap

    fun actions(target: CommandActionTarget): List<CommandListAction>

    fun fullscreen(): List<RayNodeData>

    fun initialize(
        request: RenderRequest,
        fullscreenRenderRequest: FullscreenRenderRequest,
        invalidateCacheRequest: InvalidateCacheRequest
    )

    suspend fun update(action: CommandActionBridge)

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

    suspend fun update(action: CommandActionBridge) {
        when (action) {
            is CommandActionBridge.Regular -> {
                if (action.action is CommandAction.Type) {
                    _query = action.action.query
                }
                execute(action.action)
            }
            is CommandActionBridge.Internal -> when (val internalAction = action.action) {
                is InternalCommandActionBridge.Click -> {
                    contentCallbacks.invoke(internalAction.callback) ||
                        fullscreenCallbacks.invoke(internalAction.callback)
                }
            }
        }
    }

    internal var onRenderRequest: (() -> Unit)? = null

    internal var onFullscreenRenderRequest: (() -> Unit)? = null

    internal var onInvalidateCacheRequest: ((List<CommandItemId>?) -> Unit)? = null

    internal val contentCallbacks = CallbackRegistry()

    internal val fullscreenCallbacks = CallbackRegistry()

    private var _query: String = ""
}

internal class CallbackRegistry {
    private var nextGeneration = 0L
    private val callbacksByGeneration = linkedMapOf<Long, MutableMap<CommandCallbackId, suspend () -> Unit>>()

    fun beginFrame(): CallbackFrame {
        val generation = ++nextGeneration
        callbacksByGeneration[generation] = mutableMapOf()
        while (callbacksByGeneration.size > RETAINED_GENERATIONS) {
            val oldestGeneration = callbacksByGeneration.keys.first()
            callbacksByGeneration.remove(oldestGeneration)
        }
        return CallbackFrame(generation)
    }

    suspend fun invoke(callback: CommandCallbackRef): Boolean {
        val callbacks = callbacksByGeneration[callback.generation] ?: return false
        val action = callbacks[callback.id] ?: return false
        val pluginContext = currentCoroutineContext()
        withContext(pluginContext) {
            action()
        }
        return true
    }

    inner class CallbackFrame internal constructor(
        val generation: Long
    ) {
        fun register(path: String, callback: suspend () -> Unit): CommandCallbackRef {
            val id = CommandCallbackId("__ray_callback:$generation:$path")
            callbacksByGeneration.getValue(generation)[id] = callback
            return CommandCallbackRef(id = id, generation = generation)
        }
    }

    private companion object {
        const val RETAINED_GENERATIONS = 2
    }
}
