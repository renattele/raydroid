package ru.raydroid.plugin.impl.calculator

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.host.service.ClipboardService
import ru.raydroid.plugin.api.host.service.NotificationService
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.Text
import ru.raydroid.plugin.api.ui.actions
import ru.raydroid.plugin.api.ui.Detail
import ru.raydroid.plugin.api.ui.Form
import ru.raydroid.plugin.api.ui.FormFieldData
import ru.raydroid.plugin.api.ui.FormValue
import ru.raydroid.plugin.api.ui.Grid
import ru.raydroid.plugin.api.ui.LazyGrid
import ru.raydroid.plugin.api.ui.LazyList
import ru.raydroid.plugin.api.ui.List
import ru.raydroid.plugin.api.ui.Row

class CalculatorCommand : CommandService() {
    private var result: String = ""

    override suspend fun cachedItems(requestedItems: List<CommandItemId>?, chunkSize: Int) = flow {
        Host.notification.showLoadingToast("Loading...") {
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
        }
    }

    override fun CommandListScope.content() {
        entry(
            id = ResultItemId
        ) {
            Column {
                Text(UiText.Plain(result))
            }
        }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        action(
            title = UiText.Resource("app.label"),
            primary = true
        ) {
            result = "entry"
            render()
        }
        group(UiText.Plain("111")) {
            action(
                title = UiText.Resource("app.description"),
                style = CommandListAction.Style.Destructive,
                icon = Icon.Builtin("Clear")
            ) {
                result = ""
                render()
            }
            action(
                title = UiText.Resource("app.description"),
                style = CommandListAction.Style.Destructive
            ) {
                result = "789"
                render()
            }
        }
    }

    override fun RayScope.fullscreen() {
        Column(spacing = Spacing.Medium) {
            Text(UiText.Plain("Focused: ${focusedItemId?.value ?: "none"}"))
            Row(spacing = Spacing.Medium) {
                List(searchBarPlaceholder = UiText.Plain("Filter list")) {
                    item(
                        id = CommandItemId("calculator.list.one"),
                        title = UiText.Plain("List item"),
                        subtitle = UiText.Plain(result.ifBlank { "No result" }),
                        icon = Icon.Builtin("Calculate"),
                        keywords = listOf("calculator", "result"),
                        modifier = Modifier.actions {
                            action(UiText.Plain("Set list result"), primary = true) {
                                result = "list"
                                renderFullscreen()
                            }
                        }
                    )
                    item(
                        id = CommandItemId("calculator.list.two"),
                        title = UiText.Plain("Second list item"),
                        subtitle = UiText.Plain("Detail follows focus"),
                        icon = Icon.Builtin("ArrowRight"),
                        keywords = listOf("second", "focus"),
                        modifier = Modifier.actions {
                            action(UiText.Plain("Set second result"), primary = true) {
                                result = "second"
                                renderFullscreen()
                            }
                        }
                    )
                    emptyView(UiText.Plain("No list results"))
                }
                if (focusedItemId?.value?.startsWith("calculator.list") == true) {
                    Detail(
                        markdown = when (focusedItemId) {
                            CommandItemId("calculator.list.one") -> "## List item\n\nFocus moves here with arrow keys."
                            CommandItemId("calculator.list.two") -> "## Second item\n\nThis detail is rendered by custom Row layout."
                            else -> "## No selection"
                        }
                    )
                }
            }
        }
    }

    override suspend fun execute(action: CommandAction) {
        if (action is CommandAction.Focus) {
            renderFullscreen()
            return
        }
        if (action is CommandAction.Enter) {
            result = action.hoveredId.value
            // Host.system.openApp(action.hoveredId.value)
            render()
        }
        if (action is CommandAction.Type) {
            result = action.query
            render()
        }
        result = "123"
        render()
        renderFullscreen()
        if (action is CommandAction.OpenCommand) {
            Host.searchField.setState(SearchFieldState("Text", SearchFieldSelection.SelectAll))
        }
        if (query.any { it.isDigit() }) {
            Host.notification.showToast(
                message = "Hello",
                style = NotificationService.Toast.Style.Animated
            ) {
                delay(1000)
            }
        }
    }

    private companion object {
        val ResultItemId = CommandItemId("calculator.result")
    }
}
