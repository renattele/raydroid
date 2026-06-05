package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchAliasRepository

class ObserveSearchAliasesUseCase(
    private val searchAliasRepository: SearchAliasRepository,
) {
    operator fun invoke(): Flow<Map<SearchResultId, String>> = searchAliasRepository.observeAliases()
}
