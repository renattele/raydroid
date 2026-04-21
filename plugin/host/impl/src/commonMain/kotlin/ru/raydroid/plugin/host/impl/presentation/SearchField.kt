package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import raydroid.plugin.host.impl.generated.resources.Res
import raydroid.plugin.host.impl.generated.resources.search_field_placeholder
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RDivider
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.core.designsystem.component.RTextField
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginUiText

@Composable
fun SearchField(
    state: SearchFieldState,
    onEvent: (event: SearchFieldEvent) -> Unit,
    modifier: Modifier = Modifier,
    decoratorModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(RaydroidTheme.spacing.large),
    placeholder: PluginUiText? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    actionContent: (@Composable () -> Unit)? = null
) {
    val textStyle = TextStyle(
        color = PluginColor.OnSurface.toColor(),
        fontSize = PluginFontSize.Small.toTextUnit()
    )
    Column(modifier) {
        RDivider()
        RTextField(
            state = state.fieldState,
            modifier = Modifier
                .fillMaxWidth(),
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                showKeyboardOnFocus = true,
                imeAction = if (state.canGoOnEnter) ImeAction.Go else ImeAction.None
            ),
            onKeyboardAction = {
                onEvent(SearchFieldEvent.Enter)
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorColor = PluginColor.Primary.toColor(),
            contentModifier = decoratorModifier,
            contentPadding = contentPadding,
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            onEvent(SearchFieldEvent.MoveFocusUp)
                            true
                        }

                        Key.DirectionDown -> {
                            onEvent(SearchFieldEvent.MoveFocusDown)
                            true
                        }

                        Key.Backspace -> {
                            if (state.fieldState.text.isEmpty()) {
                                onEvent(SearchFieldEvent.BackspaceOnEmpty)
                            }
                            false
                        }

                        else -> false
                    }
                } else {
                    false
                }
            },
            placeholder = {
                val placeholderText = placeholder?.asText()
                    ?: stringResource(Res.string.search_field_placeholder)
                RText(
                    placeholderText,
                    style = textStyle,
                    color = PluginColor.OnSurface.toColor(),
                    modifier = Modifier.alpha(0.5f)
                )
            },
            leadingContent = if (leadingContent != null) {
                {
                    leadingContent()
                }
            } else {
                null
            },
            trailingContent = if (actionContent != null) {
                {
                    actionContent()
                }
            } else {
                null
            }
        )
    }
}

data class SearchFieldState(
    val fieldState: TextFieldState,
    val canGoOnEnter: Boolean = false,
    val isKeyboardPresent: Boolean = false,
)

sealed class SearchFieldEvent {
    data object Enter : SearchFieldEvent()
    data object MoveFocusUp : SearchFieldEvent()
    data object MoveFocusDown : SearchFieldEvent()
    data object BackspaceOnEmpty : SearchFieldEvent()
}

@Preview
@Composable
private fun SearchFieldPreview() {
    RaydroidPreviewTheme {
        SearchField(
            state = SearchFieldState(
                fieldState = TextFieldState()
            ),
            onEvent = {}
        ) {
            ActionPanel(PluginCommandListItem(
                id = CommandItemId.Static,
                icon = null,
                title = PluginUiText.Plain("Copy"),
                description = PluginUiText.Plain("Description")
            ), false, {})
        }
    }
}
