package ru.raydroid.search

import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RAlertDialog
import ru.raydroid.core.designsystem.component.RButton
import ru.raydroid.core.designsystem.component.RIcon
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.core.designsystem.component.RTextButton
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.orUnknown
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
    val viewModel = koinViewModel<SearchViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state, modifier)
}

@Composable
fun SearchScreen(state: SearchScreenState, modifier: Modifier = Modifier) {
    ResourceResolverProvider(state.plugins) {
        val spacing = RaydroidTheme.spacing
        val fullscreen = state.fullscreen
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
            val focusedActions = state.focusedActions.map { focusedAction ->
                focusedAction.action
            }
            LaunchedEffect(state.focusedItemIndex) {
                if (fullscreen == null && state.focusedItemIndex != null) {
                    listState.scrollToItem(state.focusedItemIndex)
                }
            }
            state.alerts.forEach { alert ->
                RAlertDialog(
                    onDismissRequest = {
                        if (alert.dismissAction != null) {
                            state.eventSink(SearchScreenEvent.DismissAlert(alert))
                        }
                    },
                    confirmButton = {
                        RButton(onClick = {
                            state.eventSink(SearchScreenEvent.ConfirmAlert(alert))
                        }) {
                            RText(alert.confirmAction.title.asText())
                        }
                    },
                    dismissButton = if (alert.dismissAction != null) {
                        {
                            RTextButton(onClick = {
                                state.eventSink(SearchScreenEvent.DismissAlert(alert))
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
                        ComposeRayRenderer(fullscreen.content)
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
                        if (state.searchResults != null) {
                            itemsIndexed(state.searchResults.results) { index, searchResult ->
                                if (searchResult is SearchResultSet.CachedSearchResult) {
                                    SearchListItem(searchResult, onClick = {
                                        state.eventSink(SearchScreenEvent.Enter(searchResult.resultId))
                                    }, focused = index == state.focusedItemIndex)
                                } else if (searchResult is SearchResultSet.CommandSearchResult) {
                                    CommandListItemView(
                                        listEntry = searchResult.listEntry,
                                        onClick = {
                                            state.eventSink(SearchScreenEvent.Enter(searchResult.resultId))
                                        },
                                        focused = index == state.focusedItemIndex
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
                                        focused = index == state.focusedItemIndex
                                    ) {
                                        ComposeRayRenderer(
                                            searchResult.presentation.content
                                        )
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
                    } else if (state.searchResults?.results?.isEmpty() == true && !state.isSearching) {
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
                            actions = focusedActions,
                            visible = state.showActions,
                            onActionClick = { action ->
                                state.focusedActions
                                    .firstOrNull { focusedAction -> focusedAction.action == action }
                                    ?.let { focusedAction ->
                                        state.eventSink(SearchScreenEvent.EnterAction(focusedAction))
                                    }
                            }
                        )
                    }
                }
            }
            SearchField(
                (fullscreen?.searchFieldState ?: state.searchFieldState)
                    .copy(canGoOnEnter = fullscreen == null && state.focusedItemIndex != null),
                onEvent = { event ->
                    when (event) {
                        SearchFieldEvent.Enter -> if (fullscreen == null) {
                            state.eventSink(SearchScreenEvent.Enter())
                        }

                        SearchFieldEvent.MoveFocusDown -> if (fullscreen == null) {
                            state.eventSink(SearchScreenEvent.MoveFocusPrevious)
                        }

                        SearchFieldEvent.MoveFocusUp -> if (fullscreen == null) {
                            state.eventSink(SearchScreenEvent.MoveFocusNext)
                        }

                        SearchFieldEvent.BackspaceOnEmpty -> state.eventSink(SearchScreenEvent.BackspaceOnEmpty)
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
                            state.eventSink(SearchScreenEvent.CloseFullscreen)
                        }, exitBackspaceCount = fullscreen.exitBackspaceCount)
                    }
                } else {
                    null
                }
            ) {
                ActionPanel(
                    actions = focusedActions,
                    showActions = state.showActions,
                    onToggleActions = {
                        state.eventSink(SearchScreenEvent.ToggleActions)
                    }
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

@Preview
@Composable
fun SearchScreenPreview() {
    RaydroidPreviewTheme {
        val state = remember {
            SearchScreenState {

            }
        }
        SearchScreen(state)
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
