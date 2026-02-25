import ru.raydroid.plugin.api.core.plugin
import ru.raydroid.plugin.impl.calculator.CalculatorCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() = plugin {
    register(CalculatorCommand())
}