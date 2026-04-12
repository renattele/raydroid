package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.stringResource
import raydroid.plugin.host.impl.generated.resources.Res
import raydroid.plugin.host.impl.generated.resources.search_field_placeholder
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidTheme
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
    actionContent: (@Composable () -> Unit)? = null
) {
    val isEmpty = remember { derivedStateOf { state.fieldState.text.isEmpty() } }
    val spacing = RaydroidTheme.spacing
    val textStyle = TextStyle(
        color = PluginColor.OnSurface.toColor(),
        fontSize = PluginFontSize.Small.toTextUnit()
    )
    Column(modifier) {
        HorizontalDivider()
        BasicTextField(
            state.fieldState,
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
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

                            else -> false
                        }
                    } else {
                        false
                    }
                },
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
            cursorBrush = SolidColor(PluginColor.Primary.toColor()),
            decorator = { content ->
                val shape = if (state.isKeyboardPresent) {
                    RaydroidTheme.shapes.attachedBottom(RaydroidShapeToken.Large)
                } else {
                    RaydroidTheme.shapes.shape(RaydroidShapeToken.Large)
                }
                Box {
                    Box(
                        decoratorModifier
                            .padding(spacing.large)
                            .fillMaxWidth()
                    ) {
                        content()
                        if (isEmpty.value) {
                            Text(
                                stringResource(Res.string.search_field_placeholder),
                                style = textStyle,
                                color = PluginColor.OnSurface.toColor(),
                                modifier = Modifier.alpha(0.5f)
                            )
                        }
                    }
                    if (actionContent != null) {
                        Box(Modifier.align(Alignment.CenterEnd)) {
                            actionContent()
                        }
                    }
                }
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
