package ru.raydroid.plugin.api.host.service

import kotlinx.serialization.Serializable

interface ContactsService {
    suspend fun hasContactsAccess(): Boolean
    suspend fun requestContactsAccess()
    suspend fun openContactsSettings()
    suspend fun getContacts(): List<Contact>
    suspend fun openContact(contactId: String)
    suspend fun dial(phoneNumber: String)
    suspend fun message(phoneNumber: String)

    @Serializable
    data class Contact(
        val id: String,
        val name: String,
        val phones: List<String>
    )
}
