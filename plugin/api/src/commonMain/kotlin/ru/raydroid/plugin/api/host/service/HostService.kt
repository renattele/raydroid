package ru.raydroid.plugin.api.host.service

interface HostService {
    val cache: CacheService
    val clipboard: ClipboardService
    val environment: EnvironmentService
    val network: NetworkService
    val notification: NotificationService
    val preferences: PreferencesService
    val storage: StorageService
    val system: SystemService
}