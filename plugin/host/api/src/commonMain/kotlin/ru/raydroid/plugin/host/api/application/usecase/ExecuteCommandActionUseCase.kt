package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.presentation.CommandActionId
import ru.raydroid.plugin.api.presentation.CommandInternalActionId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.api.runtime.InternalCommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository

class ExecuteCommandActionUseCase(
    private val commandActionDispatcher: CommandActionDispatcher,
    private val searchIndexRepository: SearchIndexRepository
) {
    suspend operator fun invoke(
        resultId: SearchResultId,
        actionId: CommandActionId
    ) {
        commandActionDispatcher.dispatch(
            resultId = resultId,
            action = CommandActionBridge.Internal(
                InternalCommandActionBridge.Click(CommandInternalActionId(actionId.value))
            )
        )
        searchIndexRepository.updateUsage(resultId)
    }
}
