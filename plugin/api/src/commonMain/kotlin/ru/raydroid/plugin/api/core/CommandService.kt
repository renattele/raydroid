package ru.raydroid.plugin.api.core

import app.cash.zipline.ZiplineService
import ru.raydroid.plugin.api.ui.Ray
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.RayScope

interface CommandServiceBridge: ZiplineService {
    @Ray
    fun content(): List<RayNodeData>

    suspend fun execute()

    fun initialize(request: RenderRequest)
    suspend fun update(query: String)

    interface RenderRequest: ZiplineService {
        fun requestRender()
    }
}

abstract class CommandService {
    @Ray
    abstract fun RayScope.content()

    abstract suspend fun execute()

    fun render() {
        val onRenderRequest = _onRenderRequest
        if (onRenderRequest != null) {
            onRenderRequest()
        }
    }

    val query: String
        get() = _query

    suspend fun update(query: String) {
        _query = query
        execute()
    }

    internal var _onRenderRequest: (() -> Unit)? = null

    private var _query: String = ""
}