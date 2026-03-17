package ru.raydroid.plugin.host.impl.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
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
import org.jetbrains.compose.resources.stringResource
import raydroid.plugin.host.impl.generated.resources.Res
import raydroid.plugin.host.impl.generated.resources.search_field_placeholder
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.FontSize
import ru.raydroid.plugin.api.ui.Spacing

@Composable
fun SearchField(
    state: SearchFieldState,
    onEvent: (event: SearchFieldEvent) -> Unit,
    modifier: Modifier = Modifier,
    decoratorModifier: Modifier = Modifier
) {
    val themeResolver = LocalThemeResolver.current
    val isEmpty = remember { derivedStateOf { state.fieldState.text.isEmpty() } }
    val textStyle = TextStyle(
        color = themeResolver.color(Color.OnSurface),
        fontSize = themeResolver.fontSize(FontSize.Small)
    )
    BasicTextField(
        state.fieldState,
        modifier = modifier.fillMaxWidth().onPreviewKeyEvent { event ->
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
            } else false
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
        cursorBrush = SolidColor(themeResolver.color(Color.Primary)),
        decorator = { content ->
            val shape = if (state.isKeyboardPresent) {
                RoundedCornerShape(
                    bottomStart = themeResolver.spacing(Spacing.Large),
                    bottomEnd = themeResolver.spacing(Spacing.Large)
                )
            } else {
                RoundedCornerShape(
                    themeResolver.spacing(Spacing.Large)
                )
            }
            val shadowSpread = themeResolver.spacing(Spacing.ExtraSmall)
            val shadowOffset = themeResolver.spacing(Spacing.Small)
            Box(
                decoratorModifier
                    .border(
                        BorderStroke(
                            themeResolver.spacing(Spacing.ExtraSmall) / 2,
                            color = themeResolver.color(Color.Outline)
                        ),
                        shape
                    )
                    .dropShadow(shape = shape) {
                        color = androidx.compose.ui.graphics.Color.Black
                        spread = shadowSpread.toPx()
                        alpha = 0.4f
                        radius = shadowOffset.toPx()
                        offset = Offset(
                            x = 0f,
                            y = 2.dp.toPx()
                        )
                    }
                    .background(themeResolver.color(Color.SurfaceContainer), shape)
                    .padding(themeResolver.spacing(Spacing.Large),)
                    .fillMaxWidth()
            ) {
                content()
                if (isEmpty.value) {
                    Text(
                        stringResource(Res.string.search_field_placeholder),
                        style = textStyle,
                        color = themeResolver.color(Color.OnSurface),
                        modifier = Modifier.alpha(0.5f)
                    )
                }
            }
        }
    )
}

data class SearchFieldState(
    val fieldState: TextFieldState,
    val canGoOnEnter: Boolean = false,
    val isKeyboardPresent: Boolean = false,
)

sealed class SearchFieldEvent {
    data object Enter : SearchFieldEvent()
    data object MoveFocusUp: SearchFieldEvent()
    data object MoveFocusDown: SearchFieldEvent()
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
        )
    }
}