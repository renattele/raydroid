package ru.raydroid.search

import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.core.context.GlobalContext
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RAlertDialog
import ru.raydroid.core.designsystem.component.RButton
import ru.raydroid.core.designsystem.component.RIcon
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.core.designsystem.component.RTextButton
import ru.raydroid.feature.search.ActionUiModel
import ru.raydroid.feature.search.FullscreenUiState
import ru.raydroid.feature.search.SearchQueryState
import ru.raydroid.feature.search.SearchResultUiModel
import ru.raydroid.feature.search.SearchStore
import ru.raydroid.feature.search.SearchUiState
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.orUnknown
import ru.raydroid.plugin.host.api.ui.pluginFocusModel
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
import ru.raydroid.plugin.host.impl.presentation.SearchFieldState as ComposeSearchFieldState
import ru.raydroid.plugin.host.impl.presentation.ToastsOverlay
import ru.raydroid.plugin.host.impl.presentation.asText

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val store = remember { GlobalContext.get().get<SearchStore>() }
    DisposableEffect(store) {
        store.start()
        onDispose {
            store.stop()
        }
    }
    val state by store.state.collectAsState()
    SearchScreen(state = state, store = store, modifier = modifier)
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    store: SearchStore,
    modifier: Modifier = Modifier
) {
    ResourceResolverProvider(state.plugins) {
        val spacing = RaydroidTheme.spacing
        val fullscreen = state.fullscreen
        val rootFieldState = rememberComposeTextFieldState(state.searchFieldState, key = "root")
        val fullscreenFieldState = fullscreen?.let { fullscreenState ->
            rememberComposeTextFieldState(fullscreenState.searchFieldState, key = fullscreenState.resultId)
        }

        LaunchedEffect(rootFieldState) {
            snapshotFlow { rootFieldState.text.toString() }
                .distinctUntilChanged()
                .collect { query ->
                    store.updateRootQuery(query)
                }
        }
        if (fullscreen != null && fullscreenFieldState != null) {
            LaunchedEffect(fullscreen.resultId, fullscreenFieldState) {
                snapshotFlow { fullscreenFieldState.text.toString() }
                    .distinctUntilChanged()
                    .collect { query ->
                        store.updateFullscreenQuery(query)
                    }
            }
        }

        Column(
            modifier
                .fillMaxSize()
                .background(RaydroidTheme.colorScheme.background),
        ) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focus.requestFocus()
            }
            val listState = rememberLazyListState()
            val fullscreenFocusedActions = fullscreen
                ?.content
                ?.pluginFocusModel(
                    focusedItemId = fullscreen.focusedItemId,
                    query = fullscreen.searchFieldState.text
                )
                ?.focusedActions
                ?.takeIf { actions -> actions.isNotEmpty() }
                ?.map { action ->
                    ActionUiModel(
                        resultId = fullscreen.resultId,
                        action = action,
                        updateUsage = false
                    )
                }
            val focusedCommandActions = fullscreenFocusedActions ?: state.focusedActions
            val focusedActions = focusedCommandActions.map { focusedAction ->
                focusedAction.action
            }
            val contextActions = state.contextActions.map { contextAction ->
                contextAction.action
            }
            val overlayActions = if (state.showContextActions) {
                contextActions
            } else {
                focusedActions
            }
            val overlayFocusedActions = if (state.showContextActions) {
                state.contextActions
            } else {
                focusedCommandActions
            }
            LaunchedEffect(state.focusedItemIndex) {
                val focusedItemIndex = state.focusedItemIndex
                if (fullscreen == null && focusedItemIndex != null) {
                    listState.scrollToItem(focusedItemIndex)
                }
            }
            state.alerts.forEach { alert ->
                RAlertDialog(
                    onDismissRequest = {
                        if (alert.dismissAction != null) {
                            store.dismissAlert(alert)
                        }
                    },
                    confirmButton = {
                        RButton(onClick = {
                            store.confirmAlert(alert)
                        }) {
                            RText(alert.confirmAction.title.asText())
                        }
                    },
                    dismissButton = if (alert.dismissAction != null) {
                        {
                            RTextButton(onClick = {
                                store.dismissAlert(alert)
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
                Modifier.weight(1f)
            ) {
                if (fullscreen != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.medium, vertical = spacing.small)
                    ) {
                        ComposeRayRenderer(
                            data = fullscreen.content,
                            query = fullscreen.searchFieldState.text,
                            focusedItemId = fullscreen.focusedItemId,
                            onClick = { callback ->
                                store.enterCallback(
                                    resultId = fullscreen.resultId,
                                    callback = callback,
                                    updateUsage = false
                                )
                            },
                            onItemEnter = { itemId ->
                                store.enterPluginItem(itemId)
                            },
                            onFocus = { itemId ->
                                store.focusPluginItem(itemId)
                            },
                            onActions = { actions ->
                                store.showContextActions(
                                    resultId = fullscreen.resultId,
                                    actions = actions
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
                        val searchResults = state.searchResults
                        if (searchResults != null) {
                            itemsIndexed(searchResults) { index, searchResult ->
                                when (searchResult) {
                                    is SearchResultUiModel.Cached -> {
                                        SearchListItem(
                                            result = searchResult.toCachedSearchResult(),
                                            onClick = {
                                                store.enter(searchResult.resultId)
                                            },
                                            focused = index == state.focusedItemIndex
                                        )
                                    }

                                    is SearchResultUiModel.Command -> {
                                        CommandListItemView(
                                            listEntry = searchResult.listEntry,
                                            onClick = {
                                                store.enter(searchResult.resultId)
                                            },
                                            focused = index == state.focusedItemIndex
                                        )
                                    }

                                    is SearchResultUiModel.Live -> {
                                        RayDecorator(
                                            listItem = searchResult.listEntry,
                                            title = searchResult.rayDecoratorTitle,
                                            commandName = remember(state.plugins) {
                                                searchResult.rayDecoratorCommandName(state.plugins)
                                            },
                                            pluginName = remember(state.plugins) {
                                                searchResult.rayDecoratorPluginName(state.plugins)
                                            },
                                            focused = index == state.focusedItemIndex,
                                            onClick = {
                                                store.enter(searchResult.resultId)
                                            }
                                        ) {
                                            ComposeRayRenderer(
                                                data = searchResult.presentation.content,
                                                onClick = { callback ->
                                                    store.enterCallback(
                                                        resultId = searchResult.resultId,
                                                        callback = callback,
                                                        updateUsage = false
                                                    )
                                                },
                                                onItemEnter = {
                                                    store.enter(searchResult.resultId)
                                                },
                                                onActions = { actions ->
                                                    store.showContextActions(
                                                        resultId = searchResult.resultId,
                                                        actions = actions
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (state.searchResults == null && state.isSearching) {
                        RText(
                            text = "Searching...",
                            modifier = Modifier.align(Alignment.Center),
                            color = RaydroidTheme.colorScheme.onBackground
                        )
                    } else if (state.searchResults?.isEmpty() == true && !state.isSearching) {
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
                            visible = state.showActions || state.showContextActions,
                            onActionClick = { action ->
                                overlayFocusedActions
                                    .firstOrNull { focusedAction -> focusedAction.action == action }
                                    ?.let { focusedAction ->
                                        store.enterAction(focusedAction)
                                    }
                            }
                        )
                    }
                }
            }
            val activeFieldState = fullscreenFieldState ?: rootFieldState
            SearchField(
                state = ComposeSearchFieldState(
                    fieldState = activeFieldState,
                    canGoOnEnter = if (fullscreen != null) {
                        fullscreen.focusedItemId != null
                    } else {
                        state.focusedItemIndex != null
                    }
                ),
                onEvent = { event ->
                    when (event) {
                        SearchFieldEvent.Enter -> store.enter()
                        SearchFieldEvent.MoveFocusDown -> if (fullscreen != null) {
                            store.moveFocusNext()
                        } else {
                            store.moveFocusPrevious()
                        }

                        SearchFieldEvent.MoveFocusUp -> if (fullscreen != null) {
                            store.moveFocusPrevious()
                        } else {
                            store.moveFocusNext()
                        }

                        SearchFieldEvent.BackspaceOnEmpty -> store.backspaceOnEmpty()
                    }
                },
                modifier = Modifier.focusRequester(focus),
                contentPadding = if (fullscreen != null) {
                    PaddingValues(horizontal = spacing.small, vertical = spacing.large)
                } else {
                    PaddingValues(spacing.large)
                },
                placeholder = fullscreen?.placeholder,
                leadingContent = if (fullscreen != null) {
                    {
                        FullscreenBackButton(
                            onClick = store::closeFullscreen,
                            exitBackspaceCount = fullscreen.exitBackspaceCount
                        )
                    }
                } else {
                    null
                }
            ) {
                ActionPanel(
                    actions = focusedActions,
                    showActions = state.showActions,
                    onToggleActions = store::toggleActions
                )
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

@Composable
private fun rememberComposeTextFieldState(
    queryState: SearchQueryState,
    key: Any?
): TextFieldState {
    val fieldState = remember(key) { TextFieldState(queryState.text) }
    LaunchedEffect(queryState.text, queryState.selection, key) {
        fieldState.edit {
            replace(0, length, queryState.text)
            selection = when (queryState.selection) {
                SearchFieldSelection.CursorAtStart -> TextRange(0)
                SearchFieldSelection.CursorAtEnd -> TextRange(queryState.text.length)
                SearchFieldSelection.SelectAll -> TextRange(0, queryState.text.length)
            }
        }
    }
    return fieldState
}

@Preview
@Composable
fun SearchScreenPreview() {
    RaydroidPreviewTheme {
        SearchScreen(
            state = SearchUiState(),
            store = remember { GlobalContext.getOrNull()?.get() ?: error("Koin not initialized") }
        )
    }
}

private val SearchResultUiModel.Live.rayDecoratorTitle: PluginUiText?
    get() = listEntry.title

private fun SearchResultUiModel.Live.rayDecoratorCommandName(
    plugins: Map<PluginId, PluginRuntime>
): PluginUiText =
    plugins[resultId.pluginId]
        ?.manifest
        ?.commands
        ?.firstOrNull { command -> command.service == resultId.commandName }
        ?.title
        ?.toPluginUiText(resultId.pluginId)
        .orUnknown()

private fun SearchResultUiModel.Live.rayDecoratorPluginName(
    plugins: Map<PluginId, PluginRuntime>
): PluginUiText =
    plugins[resultId.pluginId]
        ?.manifest
        ?.title
        ?.toPluginUiText(resultId.pluginId)
        .orUnknown()

private fun SearchResultUiModel.Cached.toCachedSearchResult(): ru.raydroid.plugin.host.api.domain.model.SearchResultSet.CachedSearchResult {
    return ru.raydroid.plugin.host.api.domain.model.SearchResultSet.CachedSearchResult(
        resultId = resultId,
        listEntry = listEntry,
        titleMatches = titleMatches,
        descriptionMatches = descriptionMatches
    )
}
