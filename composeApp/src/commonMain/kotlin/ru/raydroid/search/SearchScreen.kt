package ru.raydroid.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import raydroid.composeapp.generated.resources.Res
import ru.raydroid.plugin.host.impl.ui.ComposeRayRenderer
import ru.raydroid.plugin.host.impl.ui.SearchField
import ru.raydroid.plugin.host.impl.ui.SearchFieldEvent

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
        Column(Modifier.verticalScroll(rememberScrollState())) {
            state.content.forEach { pluginContent ->
                ComposeRayRenderer(
                    resources = pluginContent.resources,
                    manifest = pluginContent.manifest,
                    data = pluginContent.data
                )
            }
        }
        SearchField(state.searchFieldState, onEvent = { event ->
            when (event) {
                is SearchFieldEvent.QueryChanged -> state.eventSink(
                    SearchScreenEvent.QueryChanged(
                        event.newQuery
                    )
                )
            }
        }, Modifier.focusRequester(focus))
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