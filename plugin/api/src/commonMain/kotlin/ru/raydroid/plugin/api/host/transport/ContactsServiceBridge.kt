package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

interface ContactsServiceBridge : ZiplineService {
    suspend fun hasContactsAccess(): Boolean

    suspend fun requestContactsAccess()

    suspend fun openContactsSettings()

    suspend fun getContacts(): List<RawContact>

    suspend fun openContact(contactId: String)

    suspend fun dial(phoneNumber: String)

    suspend fun message(phoneNumber: String)

    @Serializable
    data class RawContact(
        val id: String,
        val name: String,
        val phones: List<String>,
    )
}
