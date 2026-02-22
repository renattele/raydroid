package ru.raydroid.plugin.impl.calculator

import ru.raydroid.plugin.api.core.CommandService
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Text

class CalculatorCommand: CommandService() {
    private var result: String = ""
    override fun RayScope.content() {
        Text(result)
    }

    override suspend fun execute() {
        result = query
        render()
    }
}