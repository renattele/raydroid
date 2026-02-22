package ru.raydroid.plugin.api.host

import app.cash.zipline.ZiplineService

interface HostBridge : ZiplineService {
   val cache: Cache
   val clipboard: Clipboard
   val environment: Environment
   val notification: Notification
   val preferences: Preferences
   val storage: Storage
   val system: System
}
