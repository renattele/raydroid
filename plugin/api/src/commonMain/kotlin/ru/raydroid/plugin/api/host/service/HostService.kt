package ru.raydroid.plugin.api.host.service

interface HostService {
    val cache: CacheService
    val clipboard: ClipboardService
    val environment: EnvironmentService
    val filesystem: FileSystemService
    val network: NetworkService
    val notification: NotificationService
    val preferences: PreferencesService
    val searchField: SearchFieldService
    val storage: StorageService
    val system: SystemService
}
