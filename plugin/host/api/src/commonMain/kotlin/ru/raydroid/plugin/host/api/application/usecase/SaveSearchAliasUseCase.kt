package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.host.api.domain.model.SearchAliasSaveResult
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchAliasRepository

class SaveSearchAliasUseCase(
    private val searchAliasRepository: SearchAliasRepository
) {
    suspend operator fun invoke(
        resultId: SearchResultId,
        alias: String
    ): SearchAliasSaveResult = searchAliasRepository.saveAlias(resultId, alias)
}
