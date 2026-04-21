package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService

interface HostServiceBridge : ZiplineService {
   val cacheBridge: CacheServiceBridge
   val clipboardBridge: ClipboardServiceBridge
   val environmentBridge: EnvironmentServiceBridge
   val networkBridge: NetworkServiceBridge
   val notificationBridge: NotificationServiceBridge
   val preferencesBridge: PreferencesServiceBridge
   val searchFieldBridge: SearchFieldServiceBridge
   val storageBridge: StorageServiceBridge
   val systemBridge: SystemServiceBridge
}
