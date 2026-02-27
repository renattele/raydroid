package ru.raydroid.plugin.impl.calculator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandService
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.RayListScope
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand : CommandService() {
    private var result: String = ""
    private var label: UiText = UiText.Resource("app.main")
    private var previous: Job? = null
    private var apps = emptyList<String>()
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
            apps = Host.system.getApps().map { it.id }
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