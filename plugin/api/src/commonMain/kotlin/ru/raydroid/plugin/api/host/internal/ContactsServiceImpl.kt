package ru.raydroid.plugin.api.host.internal

import ru.raydroid.plugin.api.host.service.ContactsService
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge

internal class ContactsServiceImpl(
    private val bridge: ContactsServiceBridge,
) : ContactsService {
    override suspend fun hasContactsAccess(): Boolean = bridge.hasContactsAccess()

    override suspend fun requestContactsAccess() {
        bridge.requestContactsAccess()
    }

    override suspend fun openContactsSettings() {
        bridge.openContactsSettings()
    }

    override suspend fun getContacts(): List<ContactsService.Contact> = bridge.getContacts().map { contact -> contact.toServiceContact() }

    override suspend fun openContact(contactId: String) {
        bridge.openContact(contactId)
    }

    override suspend fun dial(phoneNumber: String) {
        bridge.dial(phoneNumber)
    }

    override suspend fun message(phoneNumber: String) {
        bridge.message(phoneNumber)
    }

    private fun ContactsServiceBridge.RawContact.toServiceContact() =
        ContactsService.Contact(
            id = id,
            name = name,
            phones = phones,
        )
}
