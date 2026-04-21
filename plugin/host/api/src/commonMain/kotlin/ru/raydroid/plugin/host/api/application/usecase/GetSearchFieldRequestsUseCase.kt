package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.host.api.event.SearchFieldGateway
import ru.raydroid.plugin.host.api.event.SearchFieldRequest

class GetSearchFieldRequestsUseCase(
    private val searchFieldGateway: SearchFieldGateway
) {
    operator fun invoke(): Flow<SearchFieldRequest> = searchFieldGateway.get()
}
