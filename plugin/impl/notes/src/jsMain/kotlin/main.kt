import ru.raydroid.plugin.api.runtime.plugin
import ru.raydroid.plugin.impl.notes.NotesCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() = plugin {
    command(NotesCommand())
}
