package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService

interface HostServiceBridge : ZiplineService {
    val cacheBridge: CacheServiceBridge
    val clipboardBridge: ClipboardServiceBridge
    val contactsBridge: ContactsServiceBridge
    val environmentBridge: EnvironmentServiceBridge
    val filesystemBridge: FileSystemServiceBridge
    val networkBridge: NetworkServiceBridge
    val notificationBridge: NotificationServiceBridge
    val preferencesBridge: PreferencesServiceBridge
    val searchFieldBridge: SearchFieldServiceBridge
    val storageBridge: StorageServiceBridge
    val systemBridge: SystemServiceBridge
}
