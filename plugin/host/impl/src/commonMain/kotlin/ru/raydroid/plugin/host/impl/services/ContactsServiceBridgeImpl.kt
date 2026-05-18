package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge

internal class UnsupportedContactsServiceBridge : ContactsServiceBridge {
    override suspend fun hasContactsAccess(): Boolean = false

    override suspend fun requestContactsAccess() = Unit

    override suspend fun openContactsSettings() = Unit

    override suspend fun getContacts(): List<ContactsServiceBridge.RawContact> = emptyList()

    override suspend fun openContact(contactId: String) = Unit

    override suspend fun dial(phoneNumber: String) = Unit

    override suspend fun message(phoneNumber: String) = Unit
}
