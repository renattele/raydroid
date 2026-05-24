package ru.raydroid.plugin.impl.apps

import kotlinx.coroutines.flow.flow
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService

class AppsCommand : CommandService() {
    override suspend fun cachedItems(requestedItems: List<CommandItemId>?, chunkSize: Int) = flow {
        val requestedIds = requestedItems?.map { itemId -> itemId.value }?.toSet()
        Host.system.getApps()
            .asSequence()
            .filter { app -> requestedIds == null || app.id in requestedIds }
            .map { app ->
                CommandListItem(
                    id = CommandItemId(app.id),
                    icon = app.icon,
                    title = UiText.Plain(app.name ?: app.id),
                    description = UiText.Plain(app.id)
                )
            }
            .chunked(chunkSize)
            .forEach { chunk -> emit(chunk) }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) = Unit

    override suspend fun execute(action: CommandAction) {
        if (action is CommandAction.Enter && action.hoveredId != CommandItemId.CommandRoot) {
            Host.system.openApp(action.hoveredId.value)
        }
    }
}
