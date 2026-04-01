package ru.raydroid.plugin.impl.calculator

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandService
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayListScope
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.host.service.NotificationService
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand : CommandService() {
    private var result: String = ""

    private val toast = NotificationService.Toast(
        message = UiText.Plain("Loading..."),
        style = NotificationService.Toast.Style.Animated
    )

    override suspend fun cachedItems(requestedItems: List<ItemId>?, chunkSize: Int) = flow {
        Host.notification.showToast(toast)
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
    }.onCompletion {
        Host.notification.hideToast(toast)
    }

    override fun RayListScope.content() {
        if (result.isNotEmpty()) {
            item(ItemId.Static, actions = {
                action("123", title = UiText.Plain("Copy"))
            }) {
                Column {
                    Text(UiText.Plain(result))
                }
            }
        }
    }

    override suspend fun execute(action: CommandAction) {
        if (action is CommandAction.Enter) {
            result = action.hoveredId.value
            // Host.system.openApp(action.hoveredId.value)
            render()
        }
        result = "123"
        render()
        if (query.any { it.isDigit() }) {
            val toast = NotificationService.Toast(
                message = UiText.Plain("Hello"),
                style = NotificationService.Toast.Style.Animated
            )
            Host.notification.showToast(toast)
            delay(1000)
            Host.notification.hideToast(toast)
        } else {
            result = ""
        }
    }
}
