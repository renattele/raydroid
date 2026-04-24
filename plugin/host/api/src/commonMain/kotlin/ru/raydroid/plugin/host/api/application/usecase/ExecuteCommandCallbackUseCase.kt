package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback

class ExecuteCommandCallbackUseCase(
    private val searchIndexRepository: SearchIndexRepository
) {
    suspend operator fun invoke(
        resultId: SearchResultId,
        callback: PluginCommandCallback,
        updateUsage: Boolean = true
    ) {
        callback()
        if (updateUsage) {
            searchIndexRepository.updateUsage(resultId)
        }
    }
}
