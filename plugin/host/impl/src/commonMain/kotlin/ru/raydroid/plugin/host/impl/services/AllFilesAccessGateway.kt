package ru.raydroid.plugin.host.impl.services

internal interface AllFilesAccessGateway {
    suspend fun hasAllFilesAccess(): Boolean

    suspend fun requestAllFilesAccess()
}

internal class UnsupportedAllFilesAccessGateway : AllFilesAccessGateway {
    override suspend fun hasAllFilesAccess(): Boolean = false

    override suspend fun requestAllFilesAccess() = Unit
}
