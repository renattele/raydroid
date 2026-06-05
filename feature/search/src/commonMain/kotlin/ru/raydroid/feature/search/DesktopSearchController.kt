package ru.raydroid.feature.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DesktopSearchController {
    private val commandFlow = MutableSharedFlow<DesktopSearchCommand>(extraBufferCapacity = 8)
    private val stateFlow = MutableStateFlow(DesktopSearchState())

    val commands: Flow<DesktopSearchCommand> = commandFlow
    val state: StateFlow<DesktopSearchState> = stateFlow

    fun requestFocus() {
        commandFlow.tryEmit(DesktopSearchCommand.RequestFocus)
    }

    fun handleEscape() {
        commandFlow.tryEmit(DesktopSearchCommand.HandleEscape)
    }

    fun closeFullscreen() {
        commandFlow.tryEmit(DesktopSearchCommand.CloseFullscreen)
    }

    fun update(state: SearchScreenState) {
        stateFlow.value = DesktopSearchState(hasFullscreen = state.fullscreenContent != null)
    }
}

sealed interface DesktopSearchCommand {
    data object RequestFocus : DesktopSearchCommand

    data object HandleEscape : DesktopSearchCommand

    data object CloseFullscreen : DesktopSearchCommand
}

data class DesktopSearchState(
    val hasFullscreen: Boolean = false,
)
