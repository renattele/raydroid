package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.SearchResultId

class CloseCommandUseCase(
    private val commandActionDispatcher: CommandActionDispatcher
) {
    suspend operator fun invoke(resultId: SearchResultId) {
        if (resultId.itemId != CommandItemId.CommandRoot) return
        commandActionDispatcher.dispatch(
            resultId = resultId,
            action = CommandActionBridge.Regular(CommandAction.CloseCommand())
        )
    }
}
