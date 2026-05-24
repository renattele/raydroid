import ru.raydroid.plugin.api.runtime.plugin
import ru.raydroid.plugin.impl.weather.WeatherCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() =
    plugin {
        command(WeatherCommand())
    }
