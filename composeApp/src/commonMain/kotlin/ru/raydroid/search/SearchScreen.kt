package ru.raydroid.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.raydroid.plugin.host.impl.ui.ComposeRayRenderer

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<SearchViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state, modifier)
}

@Composable
fun SearchScreen(state: SearchScreenState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focus.requestFocus()
        }
        state.content.forEach { pluginContent ->
            ComposeRayRenderer(
                resources = pluginContent.resources,
                manifest = pluginContent.manifest,
                data = pluginContent.data
            )
        }
        TextField(
            value = state.query,
            onValueChange = { newQuery ->
                state.eventSink(SearchScreenEvent.QueryChanged(newQuery))
            },
            modifier = Modifier.focusRequester(focus).fillMaxWidth()
        )
    }
}

@Preview
@Composable
fun SearchScreenPreview() {
    MaterialTheme {
        val state = remember {
            SearchScreenState {

            }
        }
        SearchScreen(state)
    }
}