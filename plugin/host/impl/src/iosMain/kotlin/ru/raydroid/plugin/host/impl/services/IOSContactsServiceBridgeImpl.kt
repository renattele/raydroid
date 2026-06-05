package ru.raydroid.plugin.host.impl.services

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNContact
import platform.Contacts.CNContactFamilyNameKey
import platform.Contacts.CNContactFetchRequest
import platform.Contacts.CNContactGivenNameKey
import platform.Contacts.CNContactIdentifierKey
import platform.Contacts.CNContactPhoneNumbersKey
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber
import platform.ContactsUI.CNContactViewController
import platform.ContactsUI.CNContactViewControllerDelegateProtocol
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UINavigationController
import platform.darwin.NSObject
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
internal class IOSContactsServiceBridgeImpl(
    private val contactStore: CNContactStore = CNContactStore(),
) : ContactsServiceBridge {
    override suspend fun hasContactsAccess(): Boolean =
        CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts) == CNAuthorizationStatusAuthorized

    override suspend fun requestContactsAccess() {
        suspendCancellableCoroutine { continuation ->
            contactStore.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
                continuation.resume(granted)
            }
        }
        Unit
    }

    override suspend fun openContactsSettings() {
        withContext(Dispatchers.Main) {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return@withContext
            UIApplication.sharedApplication.openURL(url)
        }
    }

    override suspend fun getContacts(): List<ContactsServiceBridge.RawContact> =
        withContext(Dispatchers.Default) {
            if (!hasContactsAccess()) return@withContext emptyList()
            val contacts = linkedMapOf<String, MutableContact>()
            val request =
                CNContactFetchRequest(
                    keysToFetch =
                        listOf(
                            CNContactIdentifierKey,
                            CNContactGivenNameKey,
                            CNContactFamilyNameKey,
                            CNContactPhoneNumbersKey,
                        ),
                )
            contactStore.enumerateContactsWithFetchRequest(request, error = null) { contact, _ ->
                val fetchedContact = contact ?: return@enumerateContactsWithFetchRequest
                val phones =
                    fetchedContact.phoneNumbers
                        .mapNotNull { item ->
                            ((item as? CNLabeledValue)?.value as? CNPhoneNumber)
                                ?.stringValue
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }
                        }.distinct()
                if (phones.isEmpty()) return@enumerateContactsWithFetchRequest
                val identifier = fetchedContact.identifier
                val name = fetchedContact.displayName().ifBlank { identifier }
                val mutableContact = contacts.getOrPut(identifier) { MutableContact(identifier, name) }
                phones.forEach { phone ->
                    if (phone !in mutableContact.phones) {
                        mutableContact.phones += phone
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
        if (!hasContactsAccess()) return
        val contact =
            contactStore.unifiedContactWithIdentifier(
                identifier = contactId,
                keysToFetch = listOf(CNContactViewController.descriptorForRequiredKeys()),
                error = null,
            ) ?: return
        withContext(Dispatchers.Main) {
            val presenter = topViewController() ?: return@withContext
            if (presenter is CNContactViewController && ContactPresentationState.activeContactId == contactId) {
                return@withContext
            }
            if (ContactPresentationState.activeContactId == contactId) {
                return@withContext
            }
            val viewController = CNContactViewController.viewControllerForContact(contact)
            viewController.contactStore = contactStore
            viewController.allowsEditing = false
            val delegate = ContactViewControllerDelegate()
            viewController.delegate = delegate
            PresentationRetainer.retain(delegate)
            ContactPresentationState.activeContactId = contactId
            val navigationController = UINavigationController(rootViewController = viewController)
            presenter.presentViewController(navigationController, animated = true, completion = null)
        }
    }

    override suspend fun dial(phoneNumber: String) {
        openUrl("tel:${phoneNumber.filterNot(Char::isWhitespace)}")
    }

    override suspend fun message(phoneNumber: String) {
        openUrl("sms:${phoneNumber.filterNot(Char::isWhitespace)}")
    }

    private suspend fun openUrl(value: String) {
        withContext(Dispatchers.Main) {
            val url = NSURL.URLWithString(value) ?: return@withContext
            UIApplication.sharedApplication.openURL(url)
        }
    }

    private data class MutableContact(
        val id: String,
        val name: String,
        val phones: MutableList<String> = mutableListOf(),
    )
}

private fun CNContact.displayName(): String =
    listOf(givenName, familyName)
        .filter { value -> value.isNotBlank() }
        .joinToString(" ")
        .ifBlank { organizationName.orEmpty() }

private class ContactViewControllerDelegate :
    NSObject(),
    CNContactViewControllerDelegateProtocol {
    override fun contactViewController(
        viewController: CNContactViewController,
        didCompleteWithContact: CNContact?,
    ) {
        ContactPresentationState.activeContactId = null
        viewController.presentingViewController?.dismissViewControllerAnimated(true, completion = null)
        PresentationRetainer.release(this)
    }
}

private object ContactPresentationState {
    var activeContactId: String? = null
}
