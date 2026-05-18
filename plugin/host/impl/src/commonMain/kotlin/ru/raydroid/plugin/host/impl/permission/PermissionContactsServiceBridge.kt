package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.readable

internal class PermissionContactsServiceBridge(
    private val contactsServiceBridge: ContactsServiceBridge,
    private val manifest: Manifest
) : ContactsServiceBridge {
    override suspend fun hasContactsAccess(): Boolean {
        requireReadAccess()
        return contactsServiceBridge.hasContactsAccess()
    }

    override suspend fun requestContactsAccess() {
        requireReadAccess()
        contactsServiceBridge.requestContactsAccess()
    }

    override suspend fun openContactsSettings() {
        requireReadAccess()
        contactsServiceBridge.openContactsSettings()
    }

    override suspend fun getContacts(): List<ContactsServiceBridge.RawContact> {
        requireReadAccess()
        return contactsServiceBridge.getContacts()
    }

    override suspend fun openContact(contactId: String) {
        requireReadAccess()
        contactsServiceBridge.openContact(contactId)
    }

    override suspend fun dial(phoneNumber: String) {
        requireReadAccess()
        contactsServiceBridge.dial(phoneNumber)
    }

    override suspend fun message(phoneNumber: String) {
        requireReadAccess()
        contactsServiceBridge.message(phoneNumber)
    }

    private fun requireReadAccess() {
        if (manifest.access.contacts?.permissions?.readable() != true) {
            throw PermissionDenied()
        }
    }
}
