import ru.raydroid.plugin.api.runtime.plugin
import ru.raydroid.plugin.impl.shortcuts.ShortcutsCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() =
    plugin {
        command(ShortcutsCommand())
    }
