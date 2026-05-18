package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchAliasRepository

class RemoveSearchAliasUseCase(
    private val searchAliasRepository: SearchAliasRepository
) {
    suspend operator fun invoke(resultId: SearchResultId) {
        searchAliasRepository.removeAlias(resultId)
    }
}
