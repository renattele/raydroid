package ru.raydroid.plugin.host.impl.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.raydroid.plugin.host.api.event.SearchFieldGateway
import ru.raydroid.plugin.host.api.event.SearchFieldRequest

internal class SearchFieldGatewayImpl : SearchFieldGateway {
    private val requests = MutableSharedFlow<SearchFieldRequest>(
        extraBufferCapacity = 64
    )

    override fun get(): Flow<SearchFieldRequest> = requests

    override suspend fun emit(request: SearchFieldRequest) {
        requests.emit(request)
    }
}
