package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.presentation.CommandInternalActionId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.api.runtime.InternalCommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.SearchResultId

class OpenLiveEntryUseCase(
    private val commandActionDispatcher: CommandActionDispatcher
) {
    suspend operator fun invoke(resultId: SearchResultId) {
        commandActionDispatcher.dispatch(
            resultId = resultId,
            action = CommandActionBridge.Internal(
                InternalCommandActionBridge.Click(CommandInternalActionId(resultId.itemId.value))
            )
        )
    }
}
