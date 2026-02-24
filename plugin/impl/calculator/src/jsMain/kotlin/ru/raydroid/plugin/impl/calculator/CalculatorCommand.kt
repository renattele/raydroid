package ru.raydroid.plugin.impl.calculator

import kotlinx.coroutines.delay
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandService
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.RayListScope
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand : CommandService() {
    private var result: String = ""
    private var label: String? = null
    override fun RayListScope.content() {
        item(ItemId.Static) {
            Column {
                label?.let { Text(it) }
                Text(result)
            }
        }
    }

    override suspend fun execute(action: CommandAction) {
        if (query.any { it.isDigit() }) {
            repeat(12) { dotsCount ->
                label = "Calculating" + ".".repeat(dotsCount % 4)
                render()
                delay(300)
            }
            result = query
            label = "Your results:"
            render()
        }
    }
}