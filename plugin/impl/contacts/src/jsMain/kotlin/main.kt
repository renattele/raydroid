import ru.raydroid.plugin.api.runtime.plugin
import ru.raydroid.plugin.impl.contacts.ContactsCommand

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() = plugin {
    command(ContactsCommand())
}
