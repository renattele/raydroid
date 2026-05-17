package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import ru.raydroid.plugin.api.presentation.CommandCallbackId
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
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
    val fieldShape = RaydroidTheme.shapes.medium
    Column(modifier) {
        RDivider(color = RaydroidTheme.colorScheme.outlineVariant)
        RTextField(
            state = state.fieldState,
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .background(RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(
                    width = RaydroidTheme.spacing.border,
                    color = RaydroidTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
                    shape = fieldShape
                ),
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                showKeyboardOnFocus = true,
                imeAction = ImeAction.Go
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
                    color = PluginColor.OnSurfaceVariant.toColor(),
                    modifier = Modifier.alpha(0.9f)
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
            ActionPanel(
                actions = listOf(
                    PluginCommandListAction(
                        callback = PluginCommandCallback(
                            ref = CommandCallbackRef(CommandCallbackId("copy"), generation = 0),
                            dispatch = {}
                        ),
                        title = PluginUiText.Plain("Copy"),
                        description = PluginUiText.Plain("Description"),
                        icon = null,
                    )
                ),
                showActions = false,
                onToggleActions = {}
            )
        }
    }
}
