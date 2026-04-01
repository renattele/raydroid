package ru.raydroid.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.impl.presentation.ActionPanel
import ru.raydroid.plugin.host.impl.presentation.ComposeRayRenderer
import ru.raydroid.plugin.host.impl.presentation.RaydroidPreviewTheme
import ru.raydroid.plugin.host.impl.presentation.ResourceResolverProvider
import ru.raydroid.plugin.host.impl.presentation.SearchField
import ru.raydroid.plugin.host.impl.presentation.SearchFieldEvent
import ru.raydroid.plugin.host.impl.presentation.SearchListItem
import ru.raydroid.plugin.host.impl.presentation.asText

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<SearchViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(state: SearchScreenState, modifier: Modifier = Modifier) {
    ResourceResolverProvider(state.plugins) {
        Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focus.requestFocus()
            }
            val listState = rememberLazyListState()
            LaunchedEffect(state.focusedItemIndex) {
                if (state.focusedItemIndex != null) {
                    listState.scrollToItem(state.focusedItemIndex)
                }
            }
            state.alerts.forEach { alert ->
                AlertDialog(
                    onDismissRequest = {
                        if (alert.dismissAction != null) {
                            state.eventSink(SearchScreenEvent.DismissAlert(alert))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            state.eventSink(SearchScreenEvent.ConfirmAlert(alert))
                        }) {
                            Text(alert.confirmAction.title.asText())
                        }
                    },
                    dismissButton = if (alert.dismissAction != null) {
                        {
                            TextButton(onClick = {
                                state.eventSink(SearchScreenEvent.DismissAlert(alert))
                            }) {
                                Text(alert.dismissAction!!.title.asText())
                            }
                        }
                    } else {
                        null
                    },
                    title = {
                        Text(alert.title.asText())
                    },
                    text = {
                        Text(alert.message.asText())
                    }
                )
            }
            LazyColumn(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .weight(1f),
                reverseLayout = true,
                state = listState
            ) {
                if (state.searchResults != null) {
                    itemsIndexed(state.searchResults.results) { index, searchResult ->
                        if (searchResult is SearchResultSet.CachedSearchResult) {
                            SearchListItem(searchResult, onClick = {
                                state.eventSink(SearchScreenEvent.Enter(searchResult.resultId))
                            }, focused = index == state.focusedItemIndex)
                        } else if (searchResult is SearchResultSet.LiveSearchResult) {
                            ComposeRayRenderer(
                                searchResult.presentation.content
                            )
                        }
                    }
                }
            }
            SearchField(
                state.searchFieldState.copy(canGoOnEnter = state.focusedItemIndex != null),
                onEvent = { event ->
                    when (event) {
                        SearchFieldEvent.Enter -> state.eventSink(SearchScreenEvent.Enter())
                        SearchFieldEvent.MoveFocusDown -> state.eventSink(SearchScreenEvent.MoveFocusPrevious)
                        SearchFieldEvent.MoveFocusUp -> state.eventSink(SearchScreenEvent.MoveFocusNext)
                    }
                },
                Modifier.focusRequester(focus)
            )
            ActionPanel(
                toasts = state.toasts,
                focusedItem = state.focusedItemIndex?.let { index ->
                    state.searchResults?.results?.getOrNull(index)?.listEntry
                }
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
