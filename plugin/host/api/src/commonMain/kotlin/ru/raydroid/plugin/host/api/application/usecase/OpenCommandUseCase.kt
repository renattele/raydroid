package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository

class OpenCommandUseCase(
    private val commandActionDispatcher: CommandActionDispatcher,
    private val searchIndexRepository: SearchIndexRepository
) {
    suspend operator fun invoke(resultId: SearchResultId) {
        if (resultId.itemId != CommandItemId.CommandRoot) return
        commandActionDispatcher.dispatch(
            resultId = resultId,
            action = CommandActionBridge.Regular(CommandAction.OpenCommand())
        )
        searchIndexRepository.updateUsage(resultId)
    }
}
