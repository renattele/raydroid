package ru.raydroid.search

import ru.raydroid.feature.search.DesktopSearchCommand
import ru.raydroid.feature.search.SearchFieldUiState
import ru.raydroid.feature.search.SearchFullscreenContentState
import ru.raydroid.feature.search.SearchOverlayState
import ru.raydroid.feature.search.SearchResultsContentState
import ru.raydroid.feature.search.SearchScreenEvent
import ru.raydroid.feature.search.SearchScreenState
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchScreenDesktopCommandTest {
    @Test
    fun `escape hides visible actions before anything else`() {
        val state =
            SearchScreenState.Results(
                content = SearchResultsContentState(),
                overlayState = SearchOverlayState(showActions = true),
            )

        val event = resolveDesktopSearchCommand(DesktopSearchCommand.HandleEscape, state)

        assertEquals(SearchScreenEvent.HideActions, event)
    }

    @Test
    fun `escape closes fullscreen when actions are hidden`() {
        val state =
            SearchScreenState.Fullscreen(
                content =
                    SearchFullscreenContentState(
                        resultId = resultId(),
                        title = null,
                        placeholder = null,
                        searchFieldState = SearchFieldUiState(),
                        exitBackspaceCount = 0,
                        content = emptyList(),
                        focusedItemId = null,
                    ),
            )

        val event = resolveDesktopSearchCommand(DesktopSearchCommand.HandleEscape, state)

        assertEquals(SearchScreenEvent.CloseFullscreen, event)
    }

    @Test
    fun `close fullscreen command is ignored when results are showing`() {
        val state = SearchScreenState.Results(content = SearchResultsContentState())

        val event = resolveDesktopSearchCommand(DesktopSearchCommand.CloseFullscreen, state)

        assertNull(event)
    }

    private fun resultId(): SearchResultId =
        SearchResultId(
            pluginId = PluginId("ru.raydroid.test"),
            commandName = "apps",
            itemId = CommandItemId("item-1"),
        )
}
