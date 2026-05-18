package ru.raydroid.plugin.impl.contacts

import kotlinx.coroutines.flow.flow
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.host.service.ContactsService
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListQuickAction
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.ui.Icon

class ContactsCommand : CommandService() {
    private var contacts = emptyList<ContactsService.Contact>()
    private var contactsLoaded = false

    override suspend fun cachedItems(
        requestedItems: List<CommandItemId>?,
        chunkSize: Int
    ) = flow {
        if (!Host.contacts.hasContactsAccess()) {
            contacts = emptyList()
            contactsLoaded = false
            return@flow
        }
        val requestedIds = requestedItems?.map { itemId -> itemId.value }?.toSet()
        val loadedContacts = Host.contacts.getContacts()
            .filter { contact -> requestedIds == null || contact.id in requestedIds }
        if (requestedIds == null) {
            contacts = loadedContacts
            contactsLoaded = true
        }
        loadedContacts
            .map { contact -> contact.toCommandListItem() }
            .chunked(chunkSize)
            .forEach { chunk -> emit(chunk) }
    }

    override fun CommandListScope.content() {
        val filter = query.trim()
        if (filter.isBlank()) return
        contacts
            .asSequence()
            .filter { contact -> contact.matches(filter) }
            .take(LiveResultLimit)
            .forEach { contact ->
                entry(
                    id = CommandItemId(contact.id),
                    title = UiText.Plain(contact.name),
                    description = UiText.Plain(contact.phones.joinToString()),
                    icon = ContactIcon,
                    quickAction = CallQuickAction
                )
            }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        if (target.itemId == CommandItemId.CommandRoot) return
        val contact = contacts.firstOrNull { contact -> contact.id == target.itemId.value } ?: return
        val phone = contact.primaryPhone() ?: return
        action(
            title = UiText.Resource("contacts.action.call"),
            icon = Icon.Builtin("Call"),
            primary = true
        ) {
            Host.contacts.dial(phone)
        }
        action(
            title = UiText.Resource("contacts.action.message"),
            icon = Icon.Builtin("Message")
        ) {
            Host.contacts.message(phone)
        }
        action(
            title = UiText.Resource("contacts.action.open"),
            icon = Icon.Builtin("Contacts")
        ) {
            Host.contacts.openContact(contact.id)
        }
    }

    override suspend fun execute(action: CommandAction) {
        when (action) {
            is CommandAction.Enter -> {
                if (action.hoveredId == CommandItemId.CommandRoot) {
                    if (Host.contacts.hasContactsAccess()) {
                        Host.contacts.openContactsSettings()
                    } else {
                        Host.contacts.requestContactsAccess()
                    }
                    if (Host.contacts.hasContactsAccess()) {
                        loadContacts()
                        invalidateCache(null)
                        render()
                    }
                    return
                }
                val contact = contact(action.hoveredId.value) ?: return
                Host.contacts.openContact(contact.id)
            }

            is CommandAction.OpenCommand -> {
                if (Host.contacts.hasContactsAccess()) {
                    Host.contacts.openContactsSettings()
                } else {
                    Host.contacts.requestContactsAccess()
                }
                if (Host.contacts.hasContactsAccess()) {
                    loadContacts()
                    invalidateCache(null)
                    render()
                }
            }

            is CommandAction.Type -> {
                if (Host.contacts.hasContactsAccess() && !contactsLoaded) {
                    loadContacts()
                }
                render()
            }

            is CommandAction.Focus,
            is CommandAction.CloseCommand -> Unit
        }
    }

    private suspend fun contact(id: String): ContactsService.Contact? {
        contacts.firstOrNull { contact -> contact.id == id }?.let { contact -> return contact }
        loadContacts()
        return contacts.firstOrNull { contact -> contact.id == id }
    }

    private suspend fun loadContacts() {
        contacts = Host.contacts.getContacts()
        contactsLoaded = true
    }

    private fun ContactsService.Contact.toCommandListItem(): CommandListItem =
        CommandListItem(
            id = CommandItemId(id),
            title = UiText.Plain(name),
            description = UiText.Plain(phones.joinToString()),
            icon = ContactIcon,
            iconColor = null,
            quickAction = CallQuickAction
        )

    private fun ContactsService.Contact.primaryPhone(): String? =
        phones.firstOrNull { phone -> phone.isNotBlank() }

    private fun ContactsService.Contact.matches(filter: String): Boolean {
        val normalizedFilter = filter.lowercase()
        return name.lowercase().contains(normalizedFilter) ||
            phones.any { phone -> phone.contains(filter) }
    }

    private companion object {
        const val LiveResultLimit = 8
        val ContactIcon = Icon.Resource("icons/contact.png")
        val CallQuickAction = CommandListQuickAction(
            title = UiText.Resource("contacts.action.call"),
            icon = Icon.Builtin("Call")
        )
    }
}
