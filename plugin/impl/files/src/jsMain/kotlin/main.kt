import ru.raydroid.plugin.api.runtime.plugin
import ru.raydroid.plugin.impl.files.FilesCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() =
    plugin {
        command(FilesCommand())
    }
