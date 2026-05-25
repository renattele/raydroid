package ru.raydroid.plugin.host.impl.services.mac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge

internal class MacContactsServiceBridgeImpl : ContactsServiceBridge {
    override suspend fun hasContactsAccess(): Boolean =
        runAppleScript(
            """
            tell application "Contacts"
                count people
            end tell
            """.trimIndent(),
        ) != null

    override suspend fun requestContactsAccess() {
        hasContactsAccess()
        Unit
    }

    override suspend fun openContactsSettings() {
        openTarget("x-apple.systempreferences:com.apple.preference.security?Privacy_Contacts")
    }

    override suspend fun getContacts(): List<ContactsServiceBridge.RawContact> {
        val output =
            runAppleScript(
                """
                set AppleScript's text item delimiters to linefeed
                tell application "Contacts"
                    set contactLines to {}
                    repeat with currentPerson in people
                        set phoneValues to value of phones of currentPerson
                        if (count of phoneValues) > 0 then
                            set AppleScript's text item delimiters to "|||"
                            set phoneValueText to phoneValues as string
                            set AppleScript's text item delimiters to tab
                            set end of contactLines to ((id of currentPerson) as string) & tab & ((name of currentPerson) as string) & tab & phoneValueText
                            set AppleScript's text item delimiters to linefeed
                        end if
                    end repeat
                    return contactLines as string
                end tell
                """.trimIndent(),
            ) ?: return emptyList()

        return output
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 3) return@mapNotNull null
                val phones = parts[2].split("|||").map(String::trim).filter(String::isNotBlank).distinct()
                if (phones.isEmpty()) return@mapNotNull null
                ContactsServiceBridge.RawContact(
                    id = parts[0],
                    name = parts[1].ifBlank { parts[0] },
                    phones = phones,
                )
            }.toList()
    }

    override suspend fun openContact(contactId: String) {
        runAppleScript(
            """
            tell application "Contacts"
                activate
                set targetPerson to first person whose id is "${contactId.escapeAppleScript()}"
                show targetPerson
            end tell
            """.trimIndent(),
        )
    }

    override suspend fun dial(phoneNumber: String) {
        openTarget("tel:${phoneNumber.filterNot(Char::isWhitespace)}")
    }

    override suspend fun message(phoneNumber: String) {
        openTarget("sms:${phoneNumber.filterNot(Char::isWhitespace)}")
    }

    private suspend fun openTarget(target: String) {
        withContext(Dispatchers.IO) {
            runCommand(listOf("open", target))
        }
    }

    private suspend fun runAppleScript(script: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val process =
                    ProcessBuilder("osascript", "-e", script)
                        .redirectErrorStream(true)
                        .start()
                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                if (process.waitFor() == 0) {
                    output
                } else {
                    null
                }
            }.getOrNull()
        }
}

private fun String.escapeAppleScript(): String = replace("\\", "\\\\").replace("\"", "\\\"")
