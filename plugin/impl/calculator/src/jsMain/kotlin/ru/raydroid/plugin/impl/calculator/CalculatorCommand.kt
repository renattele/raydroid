package ru.raydroid.plugin.impl.calculator

import kotlinx.coroutines.flow.flow
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandService
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayListScope
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.host.service.NotificationService
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand : CommandService() {
    private var result: String = ""

    override suspend fun cachedItems(requestedItems: List<ItemId>?, chunkSize: Int) = flow {
        val apps = Host.system.getApps()
        apps.map { app ->
            ListItem(
                ItemId(app.id),
                icon = app.icon,
                title = UiText.Plain(app.name ?: "Unknown"),
                description = UiText.Plain(app.id)
            )
        }.chunked(chunkSize).forEach { chunk ->
            emit(chunk)
        }
    }

    override fun RayListScope.content() {
        if (result.isNotEmpty()) {
            item(ItemId.Static) {
                Column {
                    Text(UiText.Plain(result))
                }
            }
        }
    }

    override suspend fun execute(action: CommandAction) {
        if (action is CommandAction.Enter) {
            result = action.hoveredId.value
            Host.system.openApp(action.hoveredId.value)
            render()
        }
        if (query.any { it.isDigit() }) {
            Host.notification.alert(
                title = UiText.Plain("Calculator"),
                message = UiText.Plain(query),
                actions = listOf(
                    NotificationService.AlertAction(
                        title = UiText.Plain("Hi"),
                        style = NotificationService.AlertAction.Style.Cancel
                    )
                ),
                primaryAction =
                    NotificationService.AlertAction(
                        title = UiText.Plain("Hi"),
                        style = NotificationService.AlertAction.Style.Cancel
                    )
            )
        } else {
            result = ""
        }
    }
}
