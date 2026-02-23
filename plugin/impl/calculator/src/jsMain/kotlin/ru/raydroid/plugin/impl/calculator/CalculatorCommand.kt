package ru.raydroid.plugin.impl.calculator

import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandService
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.RayListScope
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand: CommandService() {
    private var result: String = ""
    override fun RayListScope.content() {
        item(ItemId.Static) {
            Text(result)
        }
    }

    override suspend fun execute(action: CommandAction) {
        result = query
        render()
    }
}