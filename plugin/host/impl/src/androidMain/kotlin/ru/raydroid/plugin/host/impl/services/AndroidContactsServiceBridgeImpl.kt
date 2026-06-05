package ru.raydroid.plugin.host.impl.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge

internal class AndroidContactsServiceBridgeImpl(
    private val context: Context,
) : ContactsServiceBridge {
    override suspend fun hasContactsAccess(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    override suspend fun requestContactsAccess() {
        AndroidRuntimePermissionGateway.requestReadContacts(context)
        Unit
    }

    override suspend fun openContactsSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override suspend fun getContacts(): List<ContactsServiceBridge.RawContact> =
        withContext(Dispatchers.IO) {
            if (!hasContactsAccess()) return@withContext emptyList()
            val contacts = linkedMapOf<String, MutableContact>()
            val projection =
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                )
            context.contentResolver
                .query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                    val phoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex).toString()
                        val name =
                            cursor
                                .getString(nameIndex)
                                ?.trim()
                                .orEmpty()
                                .ifBlank { id }
                        val phone = cursor.getString(phoneIndex)?.trim().orEmpty()
                        if (phone.isBlank()) continue
                        val contact = contacts.getOrPut(id) { MutableContact(id = id, name = name) }
                        if (phone !in contact.phones) {
                            contact.phones += phone
                        }
                    }
                }
            contacts.values.map { contact ->
                ContactsServiceBridge.RawContact(
                    id = contact.id,
                    name = contact.name,
                    phones = contact.phones,
                )
            }
        }

    override suspend fun openContact(contactId: String) {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override suspend fun dial(phoneNumber: String) {
        AndroidRuntimePermissionGateway.requestCallPhone(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val intent =
            Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override suspend fun message(phoneNumber: String) {
        val intent =
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private data class MutableContact(
        val id: String,
        val name: String,
        val phones: MutableList<String> = mutableListOf(),
    )
}
