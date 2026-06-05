import ru.raydroid.plugin.api.runtime.plugin
import ru.raydroid.plugin.impl.apps.AppsCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() =
    plugin {
        command(AppsCommand())
    }
