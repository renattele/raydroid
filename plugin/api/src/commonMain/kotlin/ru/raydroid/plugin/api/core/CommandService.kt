package ru.raydroid.plugin.api.core

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.ui.Ray

@Serializable
sealed class CommandAction {
    @Serializable
    data class Enter(val hoveredId: ItemId): CommandAction()

    @Serializable
    data class Type(val key: Char? = null): CommandAction()
}
interface CommandServiceBridge: ZiplineService {
    suspend fun cachedItems(): List<ListItem>
    fun content(): RayItems

    fun initialize(request: RenderRequest)

    suspend fun update(query: String, action: CommandAction)

    interface RenderRequest: ZiplineService {
        fun requestRender()
    }
}

abstract class CommandService {
    open suspend fun cachedItems(): List<ListItem> {
        return emptyList()
    }

    @Ray
    abstract fun RayListScope.content()

    abstract suspend fun execute(action: CommandAction)

    fun render() {
        val onRenderRequest = onRenderRequest
        if (onRenderRequest != null) {
            onRenderRequest()
        }
    }

    val query: String
        get() = _query

    suspend fun update(query: String, action: CommandAction) {
        _query = query
        execute(action)
    }

    internal var onRenderRequest: (() -> Unit)? = null

    private var _query: String = ""
}