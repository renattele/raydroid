package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.presentation.CommandActionId
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository

class OpenItemUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
    private val searchIndexRepository: SearchIndexRepository
) {
    suspend operator fun invoke(query: String, resultId: SearchResultId) {
        dispatch(
            query = query,
            resultId = resultId,
            action = CommandAction.Enter(resultId.itemId)
        )
        searchIndexRepository.updateUsage(resultId)
    }

    suspend operator fun invoke(
        query: String,
        resultId: SearchResultId,
        actionId: CommandActionId
    ) {
        dispatch(
            query = query,
            resultId = resultId,
            action = CommandAction.ExecuteAction(
                itemId = resultId.itemId,
                actionId = actionId
            )
        )
        searchIndexRepository.updateUsage(resultId)
    }

    private suspend fun dispatch(
        query: String,
        resultId: SearchResultId,
        action: CommandAction
    ) {
        val runtimes = pluginRuntimeRegistry.get().runtimes().value
        runtimes
            .filter {
                it.pluginId == resultId.pluginId
            }.forEach { runtime ->
                runtime.update(query, action)
            }
    }
}
