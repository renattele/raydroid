package ru.raydroid.plugin.impl.calculator

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.host.service.NotificationService
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand : CommandService() {
    private var result: String = ""

    private val toast = NotificationService.Toast(
        message = UiText.Plain("Loading..."),
        style = NotificationService.Toast.Style.Animated
    )

    override suspend fun cachedItems(requestedItems: List<CommandItemId>?, chunkSize: Int) = flow {
        Host.notification.showToast(toast)
        val apps = Host.system.getApps()
        apps.map { app ->
            CommandListItem(
                CommandItemId(app.id),
                icon = app.icon,
                title = UiText.Plain(app.name ?: "Unknown"),
                description = UiText.Plain(app.id)
            )
        }.chunked(chunkSize).forEach { chunk ->
            emit(chunk)
        }
    }.onCompletion {
        //   Host.notification.hideToast(toast)
    }

    override fun CommandListScope.content() {
        entry(CommandItemId.Static) {
            Column {
                Text(UiText.Plain(result))
            }
        }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        when (target) {
            CommandActionTarget.CommandRoot,
            CommandActionTarget.Fullscreen,
            is CommandActionTarget.Item -> {
                action("123", title = UiText.Resource("app.label"))
                group(UiText.Plain("111")) {
                    action(
                        "456",
                        title = UiText.Resource("app.description"),
                        style = CommandListAction.Style.Destructive,
                        icon = Icon.Builtin("Clear")
                    )
                    action(
                        "789",
                        title = UiText.Resource("app.description"),
                        style = CommandListAction.Style.Destructive
                    )
                }
            }
        }
    }

    override fun RayScope.fullscreen() {
        Column(spacing = Spacing.Medium) {
            Text(UiText.Resource("app.main"))
            Text(UiText.Plain(result))
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
        renderFullscreen()
        if (action is CommandAction.OpenCommand) {
            Host.searchField.setState(SearchFieldState("Text", SearchFieldSelection.SelectAll))
        }
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
