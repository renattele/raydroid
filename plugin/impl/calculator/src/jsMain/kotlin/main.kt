import ru.raydroid.plugin.api.core.Command
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.core.manifest
import ru.raydroid.plugin.impl.calculator.CalculatorCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() = manifest {
    name = "calculator"
    title = UiText.Plain("Calculator")
    description = UiText.Plain("Calculator extension")
    author = UiText.Plain("Raydroid")

    command(CalculatorCommand()) {
        title = UiText.Plain("Calculator")
        description = UiText.Plain("Calculator extension")
        mode = Command.Mode.Inline
        match = "[0-9].*".toRegex()
    }
}