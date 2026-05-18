package ru.raydroid.plugin.api.manifest

import app.cash.zipline.ZiplineService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.model.UiText

@Serializable
enum class Platform {
    Android,
    IOS,
    Linux,
    Windows,
    MacOS
}

@Serializable
data class Manifest(
    val name: String,
    @Serializable(with = ManifestUiTextSerializer::class)
    val title: UiText,
    @Serializable(with = ManifestUiTextSerializer::class)
    val description: UiText,
    @Serializable(with = ManifestUiTextSerializer::class)
    val author: UiText,
    val version: Int,
    val platforms: List<Platform>,
    val categories: List<String>,
    val license: String,
    val commands: List<Command>,
    val resources: Resources,
    val access: Access = Access()
)

typealias Resources = Map<String, Map<String, String>>

@Serializable
data class Access(
    val clipboard: ClipboardAccess? = null,
    val cache: SizedAccess? = null,
    val contacts: ContactsAccess? = null,
    val environment: EnvironmentAccess? = null,
    val filesystem: FileSystemAccess? = null,
    val network: NetworkAccess? = null,
    val notification: NotificationAccess? = null,
    val preferences: SizedAccess? = null,
    @SerialName("search_field")
    val searchField: SearchFieldAccess? = null,
    val storage: SizedAccess? = null,
    val system: SystemAccess? = null,
    val runtime: RuntimeAccess? = null
)

@Serializable
data class ClipboardAccess(
    val permissions: List<Permission>? = null
)

@Serializable
data class ContactsAccess(
    val permissions: List<Permission>? = null
)

fun List<Permission>.readable(): Boolean =
    contains(Permission.Read) || contains(Permission.Manage)

fun List<Permission>.writable(): Boolean =
    contains(Permission.Write) || contains(Permission.Manage)

@Serializable
data class SizedAccess(
    val permissions: List<Permission>? = null,
    @SerialName("requested_size")
    val requestedSize: String? = null
)

@Serializable
data class EnvironmentAccess(
    val permissions: List<Permission>? = null,
    val access: List<String>? = null
)

@Serializable
data class FileSystemAccess(
    val permissions: List<FileSystemAccessPermission>? = null,
    @SerialName("allowed_paths")
    val allowedPaths: List<String>? = null
)

@Serializable
enum class FileSystemAccessPermission {
    @SerialName("read")
    Read,

    @SerialName("write")
    Write,

    @SerialName("manage")
    Manage,

    @SerialName("watch")
    Watch
}

@Serializable
data class NetworkAccess(
    val permissions: List<Permission>? = null,
    @SerialName("allowed_urls")
    val allowedUrls: List<String>? = null
)

@Serializable
data class SearchFieldAccess(
    val permissions: List<Permission>? = null
)

@Serializable
data class SystemAccess(
    val permissions: List<SystemAccessPermission>? = null,
    @SerialName("visible_apps")
    val visibleApps: List<String>? = null,
    @SerialName("allowed_urls")
    val allowedUrls: List<String>? = null
)

@Serializable
enum class SystemAccessPermission {
    @SerialName("get_apps")
    GetApps,
    @SerialName("open_resource")
    OpenResource,
    @SerialName("open_app")
    OpenApp
}

@Serializable
data class RuntimeAccess(
    @SerialName("requested_stack_size")
    val requestedStackSize: Int? = null
)

@Serializable
data class NotificationAccess(
    @SerialName("show_alerts") val showAlerts: Boolean? = null,
    @SerialName("show_toasts") val showToasts: Boolean? = null,
)

@Serializable
enum class Permission {
    @SerialName("read")
    Read,

    @SerialName("write")
    Write,

    @SerialName("manage")
    Manage
}

interface ManifestService: ZiplineService {
    fun getManifest(): Manifest
}
