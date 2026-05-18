package ru.raydroid.search

import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.koinInject
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RAlertDialog
import ru.raydroid.core.designsystem.component.RButton
import ru.raydroid.core.designsystem.component.RIcon
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.core.designsystem.component.RTextButton
import ru.raydroid.feature.search.FocusedCommandAction
import ru.raydroid.feature.search.SearchFieldUiState
import ru.raydroid.feature.search.SearchScreenEvent
import ru.raydroid.feature.search.SearchScreenState
import ru.raydroid.feature.search.SearchViewModel
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginActionPanelHintMode
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.actionPanelHintMode
import ru.raydroid.plugin.host.api.ui.orUnknown
import ru.raydroid.plugin.host.api.ui.pluginFocusModel
import ru.raydroid.plugin.host.api.ui.suppressesHostActions
import ru.raydroid.plugin.host.api.ui.toPluginUiText
import ru.raydroid.plugin.host.impl.presentation.ActionPanel
import ru.raydroid.plugin.host.impl.presentation.ActionsPanelOverlay
import ru.raydroid.plugin.host.impl.presentation.CommandListItemView
import ru.raydroid.plugin.host.impl.presentation.ComposeRayRenderer
import ru.raydroid.plugin.host.impl.presentation.RayDecorator
import ru.raydroid.plugin.host.impl.presentation.RaydroidPreviewTheme
import ru.raydroid.plugin.host.impl.presentation.ResourceResolverProvider
import ru.raydroid.plugin.host.impl.presentation.SearchField
import ru.raydroid.plugin.host.impl.presentation.SearchFieldEvent
import ru.raydroid.plugin.host.impl.presentation.SearchListItem
import ru.raydroid.plugin.host.impl.presentation.ToastsOverlay
import ru.raydroid.plugin.host.impl.presentation.asText

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val viewModel = koinInject<SearchViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun SearchScreen(
    state: SearchScreenState,
    onEvent: (SearchScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    ResourceResolverProvider(state.plugins) {
        val spacing = RaydroidTheme.spacing
        val fullscreen = state.fullscreenContent
        val resultsContent = state.resultsContent
        val overlayState = state.overlayState
        val activeSearchFieldState = state.searchFieldState
        val latestSearchFieldState by rememberUpdatedState(activeSearchFieldState)
        val searchField = remember(fullscreen?.resultId) { TextFieldState() }
        Column(
            modifier
                .fillMaxSize()
                .background(RaydroidTheme.colorScheme.background),
        ) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focus.requestFocus()
            }
            LaunchedEffect(
                searchField,
                fullscreen?.resultId,
                activeSearchFieldState.query,
                activeSearchFieldState.selection
            ) {
                searchField.apply(activeSearchFieldState)
            }
            LaunchedEffect(searchField, fullscreen?.resultId) {
                snapshotFlow {
                    SearchFieldSnapshot(
                        query = searchField.text.toString(),
                        selection = searchField.selection.asSelection(searchField.text.length)
                    )
                }
                    .distinctUntilChanged()
                    .collectLatest { snapshot ->
                        if (snapshot != latestSearchFieldState.asSnapshot()) {
                            onEvent(
                                SearchScreenEvent.UpdateQuery(
                                    query = snapshot.query,
                                    selection = snapshot.selection
                                )
                            )
                        }
                    }
            }
            val listState = rememberLazyListState()
            val fullscreenFocusedActions = fullscreen
                ?.content
                ?.pluginFocusModel(
                    focusedItemId = fullscreen.focusedItemId,
                    query = fullscreen.searchFieldState.query
                )
                ?.focusedActions
                ?.takeIf { actions -> actions.isNotEmpty() }
                ?.map { action ->
                    FocusedCommandAction(
                        resultId = fullscreen.resultId,
                        action = action,
                        updateUsage = false
                    )
                }
            val suppressHostActions = fullscreen?.content?.suppressesHostActions() == true
            val actionPanelHintMode = fullscreen?.content?.actionPanelHintMode()
                ?: PluginActionPanelHintMode.Full
            val focusedCommandActions = if (suppressHostActions) {
                emptyList()
            } else {
                fullscreenFocusedActions ?: overlayState.focusedActions
                    .takeIf { actions ->
                        fullscreen == null || actions.all { action -> action.resultId == fullscreen.resultId }
                    }
                    .orEmpty()
            }
            val focusedActions = focusedCommandActions.map { focusedAction ->
                focusedAction.action
            }
            val contextActions = overlayState.contextActions.map { contextAction ->
                contextAction.action
            }
            val overlayActions = if (overlayState.showContextActions) {
                contextActions
            } else {
                focusedActions
            }
            val overlayFocusedActions = if (overlayState.showContextActions) {
                overlayState.contextActions
            } else {
                focusedCommandActions
            }
            val focusedItemIndex = resultsContent?.focusedItemIndex
            val searchResults = resultsContent?.searchResults
            LaunchedEffect(focusedItemIndex) {
                if (fullscreen == null && focusedItemIndex != null) {
                    listState.scrollToItem(focusedItemIndex)
                }
            }
            state.alerts.forEach { alert ->
                RAlertDialog(
                    onDismissRequest = {
                        if (alert.dismissAction != null) {
                            onEvent(SearchScreenEvent.DismissAlert(alert))
                        }
                    },
                    confirmButton = {
                        RButton(onClick = {
                            onEvent(SearchScreenEvent.ConfirmAlert(alert))
                        }) {
                            RText(alert.confirmAction.title.asText())
                        }
                    },
                    dismissButton = if (alert.dismissAction != null) {
                        {
                            RTextButton(onClick = {
                                onEvent(SearchScreenEvent.DismissAlert(alert))
                            }) {
                                RText(alert.dismissAction!!.title.asText())
                            }
                        }
                    } else {
                        null
                    },
                    title = {
                        RText(alert.title.asText())
                    },
                    text = {
                        RText(alert.message.asText())
                    }
                )
            }
            Box(
                Modifier
                    .weight(1f)
            ) {
                if (fullscreen != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.medium, vertical = spacing.small)
                    ) {
                        ComposeRayRenderer(
                            data = fullscreen.content,
                            query = fullscreen.searchFieldState.query,
                            focusedItemId = fullscreen.focusedItemId,
                            onClick = { callback ->
                                onEvent(
                                    SearchScreenEvent.EnterCallback(
                                        resultId = fullscreen.resultId,
                                        callback = callback,
                                        updateUsage = false
                                    )
                                )
                            },
                            onItemEnter = { itemId ->
                                onEvent(SearchScreenEvent.EnterPluginItem(itemId))
                            },
                            onFocus = { itemId ->
                                onEvent(SearchScreenEvent.FocusPluginItem(itemId))
                            },
                            onActions = { actions ->
                                onEvent(
                                    SearchScreenEvent.ShowContextActions(
                                        resultId = fullscreen.resultId,
                                        actions = actions
                                    )
                                )
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.medium,
                            end = spacing.medium,
                            top = spacing.extraSmall,
                            bottom = spacing.extraLarge * 2
                        ),
                        reverseLayout = true,
                        state = listState
                    ) {
                        if (searchResults != null) {
                            itemsIndexed(searchResults.results) { index, searchResult ->
                                if (searchResult is SearchResultSet.CachedSearchResult) {
                                    SearchListItem(searchResult, onClick = {
                                        onEvent(SearchScreenEvent.Submit(searchResult.resultId))
                                    }, focused = index == focusedItemIndex)
                                } else if (searchResult is SearchResultSet.CommandSearchResult) {
                                    CommandListItemView(
                                        listEntry = searchResult.listEntry,
                                        onClick = {
                                            onEvent(SearchScreenEvent.Submit(searchResult.resultId))
                                        },
                                        focused = index == focusedItemIndex
                                    )
                                } else if (
                                    searchResult is SearchResultSet.LiveSearchResult &&
                                    searchResult.presentation.content.isEmpty()
                                ) {
                                    CommandListItemView(
                                        listEntry = searchResult.listEntry,
                                        onClick = {
                                            onEvent(SearchScreenEvent.Submit(searchResult.resultId))
                                        },
                                        focused = index == focusedItemIndex
                                    )
                                } else if (searchResult is SearchResultSet.LiveSearchResult) {
                                    RayDecorator(
                                        listItem = searchResult.listEntry,
                                        title = searchResult.rayDecoratorTitle,
                                        commandName = remember(state.plugins) {
                                            searchResult.rayDecoratorCommandName(state.plugins)
                                        },
                                        pluginName = remember(state.plugins) {
                                            searchResult.rayDecoratorPluginName(state.plugins)
                                        },
                                        focused = index == focusedItemIndex,
                                        onClick = {
                                            onEvent(SearchScreenEvent.Submit(searchResult.resultId))
                                        }
                                    ) {
                                        ComposeRayRenderer(
                                            data = searchResult.presentation.content,
                                            onClick = { callback ->
                                                onEvent(
                                                    SearchScreenEvent.EnterCallback(
                                                        resultId = searchResult.resultId,
                                                        callback = callback,
                                                        updateUsage = false
                                                    )
                                                )
                                            },
                                            onItemEnter = {
                                                onEvent(SearchScreenEvent.Submit(searchResult.resultId))
                                            },
                                            onActions = { actions ->
                                                onEvent(
                                                    SearchScreenEvent.ShowContextActions(
                                                        resultId = searchResult.resultId,
                                                        actions = actions
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (state is SearchScreenState.Loading) {
                        RText(
                            text = "Searching...",
                            modifier = Modifier.align(Alignment.Center),
                            color = RaydroidTheme.colorScheme.onBackground
                        )
                    } else if (resultsContent?.searchResults?.results?.isEmpty() == true && resultsContent.isSearching == false) {
                        RText(
                            text = "No results",
                            modifier = Modifier.align(Alignment.Center),
                            color = RaydroidTheme.colorScheme.onBackground
                        )
                    }
                }
                LookaheadScope {
                    Column(
                        Modifier.padding(spacing.medium).align(Alignment.BottomEnd),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small)
                    ) {
                        ToastsOverlay(
                            toasts = state.toasts,
                            Modifier.animateBounds(
                                this@LookaheadScope,
                                animateMotionFrameOfReference = true
                            )
                        )
                        ActionsPanelOverlay(
                            actions = overlayActions,
                            visible = overlayState.showActions || overlayState.showContextActions,
                            onActionClick = { action ->
                                overlayFocusedActions
                                    .firstOrNull { focusedAction -> focusedAction.action == action }
                                    ?.let { focusedAction ->
                                        onEvent(SearchScreenEvent.EnterAction(focusedAction))
                                    }
                            }
                        )
                    }
                }
            }
            SearchField(
                activeSearchFieldState.toPresentationState(
                    fieldState = searchField,
                    canGoOnEnter = if (fullscreen != null) {
                        fullscreen.focusedItemId != null
                    } else {
                        focusedItemIndex != null
                    }
                ),
                onEvent = { event ->
                    when (event) {
                        SearchFieldEvent.Enter -> {
                            onEvent(SearchScreenEvent.Submit())
                        }

                        SearchFieldEvent.MoveFocusDown -> if (fullscreen != null) {
                            onEvent(SearchScreenEvent.MoveFocusNext)
                        } else {
                            onEvent(SearchScreenEvent.MoveFocusPrevious)
                        }

                        SearchFieldEvent.MoveFocusUp -> if (fullscreen != null) {
                            onEvent(SearchScreenEvent.MoveFocusPrevious)
                        } else {
                            onEvent(SearchScreenEvent.MoveFocusNext)
                        }

                        SearchFieldEvent.BackspaceOnEmpty -> onEvent(SearchScreenEvent.BackspaceOnEmpty)
                    }
                },
                Modifier.focusRequester(focus),
                contentPadding = if (fullscreen != null) {
                    PaddingValues(horizontal = spacing.small, vertical = spacing.large)
                } else {
                    PaddingValues(spacing.large)
                },
                placeholder = fullscreen?.placeholder,
                leadingContent = if (fullscreen != null) {
                    {
                        FullscreenBackButton(onClick = {
                            onEvent(SearchScreenEvent.CloseFullscreen)
                        }, exitBackspaceCount = fullscreen.exitBackspaceCount)
                    }
                } else {
                    null
                }
            ) {
                if (actionPanelHintMode != PluginActionPanelHintMode.Hidden) {
                    ActionPanel(
                        actions = focusedActions,
                        showActions = overlayState.showActions,
                        showPrimaryHint = actionPanelHintMode == PluginActionPanelHintMode.Full,
                        onPrimaryAction = {
                            onEvent(SearchScreenEvent.Submit())
                        },
                        onToggleActions = {
                            onEvent(SearchScreenEvent.ToggleActions)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenBackButton(
    exitBackspaceCount: Int,
    onClick: () -> Unit
) {
    val motion = RaydroidTheme.motionScheme.fast
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }
    val slotWidth by animateDpAsState(
        targetValue = if (!entered) {
            0.dp
        } else {
            when (exitBackspaceCount) {
                0 -> 48.dp
                1 -> 40.dp
                else -> 0.dp
            }
        },
        animationSpec = motion.dpSpec(),
        label = "FullscreenBackButtonSlotWidth"
    )
    val buttonWidth by animateDpAsState(
        targetValue = if (!entered) {
            0.dp
        } else {
            when (exitBackspaceCount) {
                0 -> 40.dp
                1 -> 32.dp
                else -> 0.dp
            }
        },
        animationSpec = motion.dpSpec(),
        label = "FullscreenBackButtonWidth"
    )
    val iconSize by animateDpAsState(
        targetValue = if (!entered) {
            0.dp
        } else {
            when (exitBackspaceCount) {
                0 -> 22.dp
                1 -> 18.dp
                else -> 0.dp
            }
        },
        animationSpec = motion.dpSpec(),
        label = "FullscreenBackButtonIconSize"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (!entered || exitBackspaceCount >= 2) 0f else 1f,
        animationSpec = motion.floatSpec(),
        label = "FullscreenBackButtonIconAlpha"
    )
    Box(
        modifier = Modifier
            .width(slotWidth)
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(buttonWidth)
                .height(40.dp)
                .clip(RaydroidTheme.shapes.small)
                .background(RaydroidTheme.colorScheme.primaryContainer)
                .clickable(enabled = exitBackspaceCount < 2, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            RIcon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(iconSize)
                    .alpha(iconAlpha),
                tint = RaydroidTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Preview
@Composable
fun SearchScreenPreview() {
    RaydroidPreviewTheme {
        val state = remember {
            SearchScreenState.Results(content = ru.raydroid.feature.search.SearchResultsContentState())
        }
        SearchScreen(state = state, onEvent = {})
    }
}

private val SearchResultSet.LiveSearchResult.rayDecoratorTitle: PluginUiText?
    get() = listEntry.title

private fun SearchResultSet.LiveSearchResult.rayDecoratorCommandName(
    plugins: Map<PluginId, PluginRuntime>
): PluginUiText =
    plugins[resultId.pluginId]
        ?.manifest
        ?.commands
        ?.firstOrNull { command -> command.service == resultId.commandName }
        ?.title
        ?.toPluginUiText(resultId.pluginId)
        .orUnknown()

private fun SearchResultSet.LiveSearchResult.rayDecoratorPluginName(
    plugins: Map<PluginId, PluginRuntime>
): PluginUiText =
    plugins[resultId.pluginId]
        ?.manifest
        ?.title
        ?.toPluginUiText(resultId.pluginId)
        .orUnknown()

private fun SearchFieldUiState.toPresentationState(
    fieldState: TextFieldState,
    canGoOnEnter: Boolean
) =
    ru.raydroid.plugin.host.impl.presentation.SearchFieldState(
        fieldState = fieldState,
        canGoOnEnter = canGoOnEnter
    )

private data class SearchFieldSnapshot(
    val query: String,
    val selection: SearchFieldSelection
)

private fun SearchFieldUiState.asSnapshot(): SearchFieldSnapshot =
    SearchFieldSnapshot(query = query, selection = selection)

private fun TextRange.asSelection(textLength: Int): SearchFieldSelection =
    when {
        textLength == 0 -> SearchFieldSelection.CursorAtEnd
        start == 0 && end == 0 -> SearchFieldSelection.CursorAtStart
        start == 0 && end == textLength -> SearchFieldSelection.SelectAll
        else -> SearchFieldSelection.CursorAtEnd
    }

private fun TextFieldState.apply(state: SearchFieldUiState) {
    edit {
        replace(0, length, state.query)
        selection = when (state.selection) {
            SearchFieldSelection.CursorAtStart -> TextRange(0)
            SearchFieldSelection.CursorAtEnd -> TextRange(state.query.length)
            SearchFieldSelection.SelectAll -> TextRange(0, state.query.length)
        }
    }
}
