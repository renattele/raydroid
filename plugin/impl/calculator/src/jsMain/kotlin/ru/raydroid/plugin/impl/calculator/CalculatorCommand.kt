package ru.raydroid.plugin.impl.calculator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandService
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayListScope
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand : CommandService() {
    private var result: String = ""
    private var label: UiText = UiText.Resource("app.main")
    private var previous: Job? = null
    private var apps = emptyList<String>()
    override suspend fun cachedItems(requestedItems: List<ItemId>?, chunkSize: Int) = flow {
        val apps = Host.system.getApps()
        apps.map { app ->
            ListItem(
                ItemId(app.id),
                Icon.Url("https://i.imgur.com/UVpA9a0.jpeg"),
                title = UiText.Plain(app.name),
                description = UiText.Plain(app.id)
            )
        }.chunked(chunkSize).forEach { chunk ->
            emit(chunk)
        }
        emit(listOf(ListItem(
            ItemId("12333456787"),
            Icon.Url("https://i.imgur.com/UVpA9a0.jpeg"),
            title = UiText.Resource("app.description"),
            description = UiText.Resource("app.description")
        )))
    }

    override fun RayListScope.content() {
        item(ItemId.Static) {

            Column {
                apps.forEach { app ->
                    Text(UiText.Plain(app))
                }
                Text(label)
                Text(UiText.Plain(result))
            }
        }
    }

    override suspend fun execute(action: CommandAction) {
        if (query.any { it.isDigit() }) {
            previous?.cancel()
            previous = CoroutineScope(Dispatchers.Unconfined).launch {
                repeat(12) { dotsCount ->
                    label = UiText.Plain("Calculating" + ".".repeat(dotsCount % 4))
                    render()
                    delay(300)
                }
                result = query
                label = UiText.Resource("app.label")
                render()
            }
        }
    }
}
