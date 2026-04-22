package ru.raydroid.plugin.api.runtime

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionId
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandInternalActionId
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.api.presentation.CommandPresentationMap
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.buildRayNodes
import ru.raydroid.plugin.api.zipline

@DslMarker
annotation class PluginMarker

interface PluginScope {
    @PluginMarker
    fun command(commandService: CommandService)
}

@PluginMarker
fun plugin(content: PluginScope.() -> Unit) {
    val scope = object : PluginScope {
        override fun command(commandService: CommandService) {
            val serviceName = commandService::class.simpleName!!
            zipline.bind(serviceName, commandService.toBridge(serviceName))
        }
    }
    scope.content()
}

internal fun CommandService.toBridge(serviceName: String): CommandServiceBridge =
    object : CommandServiceBridge {
        private val entryIdPrefix = "__ray_entry:"
        private val actionIdPrefix = "__ray_action:"

        override fun getServiceName() = serviceName

        override suspend fun cachedItems(
            requestedItems: List<CommandItemId>?,
            chunkSize: Int
        ): Flow<List<CommandListItem>> {
            return this@toBridge.cachedItems(requestedItems, chunkSize)
        }

        override fun content(): CommandPresentationMap {
            val presentations = mutableMapOf<CommandItemId, CommandPresentation>()
            clickActions.keys.removeAll { id -> id.value.startsWith(entryIdPrefix) }
            var entryIndex = 0
            val scope = object : CommandListScope {
                override fun entry(
                    title: UiText?,
                    description: UiText?,
                    icon: Icon?,
                    onClick: suspend () -> Unit,
                    content: RayScope.() -> Unit
                ) {
                    val clickId = CommandInternalActionId("$entryIdPrefix${entryIndex++}")
                    clickActions[clickId] = onClick
                    val id = CommandItemId(clickId.value)
                    val listEntry = CommandListItem(
                        id = id,
                        title = title,
                        description = description,
                        icon = icon,
                    )
                    presentations[id] = CommandPresentation(
                        listEntry = listEntry,
                        content = buildRayNodes(content)
                    )
                }
            }
            scope.content()
            return presentations
        }

        override fun actions(target: CommandActionTarget): List<CommandListAction> {
            val targetActionIdPrefix = "$actionIdPrefix${target.toActionIdKey()}:"
            clickActions.keys.removeAll { id -> id.value.startsWith(targetActionIdPrefix) }
            return buildActions(
                target = target,
                group = null,
                groupPath = emptyList()
            ) {
                runActions(this, target)
            }
        }

        override fun fullscreen() = buildRayNodes fullscreenContent@{
            with(this@toBridge) {
                this@fullscreenContent.fullscreen()
            }
        }

        override fun initialize(
            request: CommandServiceBridge.RenderRequest,
            fullscreenRenderRequest: CommandServiceBridge.FullscreenRenderRequest,
            invalidateCacheRequest: CommandServiceBridge.InvalidateCacheRequest
        ) {
            onRenderRequest = request::requestRender
            onFullscreenRenderRequest = fullscreenRenderRequest::requestFullscreenRender
            onInvalidateCacheRequest = invalidateCacheRequest::requestInvalidation
        }

        override suspend fun update(action: CommandActionBridge) {
            this@toBridge.update(action)
        }

        private fun buildActions(
            target: CommandActionTarget,
            group: UiText?,
            groupPath: List<Int>,
            content: CommandActionScope.() -> Unit
        ): List<CommandListAction> {
            val actions = mutableListOf<CommandListAction>()
            var groupIndex = 0
            var actionIndex = 0
            val scope = object : CommandActionScope {
                override fun group(
                    title: UiText,
                    content: CommandActionScope.() -> Unit
                ) {
                    actions += buildActions(
                        target = target,
                        group = title,
                        groupPath = groupPath + groupIndex++,
                        content = content
                    )
                }

                override fun action(
                    title: UiText,
                    icon: Icon?,
                    description: UiText?,
                    style: CommandListAction.Style,
                    primary: Boolean,
                    onClick: suspend () -> Unit
                ) {
                    val clickId = CommandInternalActionId(
                        "$actionIdPrefix${target.toActionIdKey()}:${groupPath.joinToString(".")}:${actionIndex++}"
                    )
                    clickActions[clickId] = onClick
                    val id = CommandActionId(clickId.value)
                    actions += CommandListAction(
                        id = id,
                        title = title,
                        icon = icon,
                        description = description,
                        group = group,
                        style = style,
                        primary = primary
                    )
                }
            }
            scope.content()
            return actions
        }

        private fun CommandActionTarget.toActionIdKey() = when (this) {
            CommandActionTarget.CommandRoot -> "command_root"
            CommandActionTarget.Fullscreen -> "fullscreen"
            is CommandActionTarget.Item -> "item:${itemId.value}"
        }

        private fun runActions(scope: CommandActionScope, target: CommandActionTarget) {
            with(this@toBridge) {
                scope.actions(target)
            }
        }
    }
