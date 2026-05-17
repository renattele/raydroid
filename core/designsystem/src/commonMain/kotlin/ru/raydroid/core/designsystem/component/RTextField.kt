package ru.raydroid.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import ru.raydroid.core.designsystem.RaydroidTheme

@Composable
fun RTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(
        color = RaydroidTheme.colorScheme.onSurface,
        fontSize = RaydroidTheme.typographyScale.small
    ),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: () -> Unit = {},
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    outputTransformation: OutputTransformation? = null,
    scrollState: ScrollState = rememberScrollState(),
    cursorColor: Color = RaydroidTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(RaydroidTheme.spacing.large),
    contentModifier: Modifier = Modifier,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.then(
            if (onPreviewKeyEvent != null) {
                Modifier.onPreviewKeyEvent(onPreviewKeyEvent)
            } else {
                Modifier
            }
        ),
    ) {
        if (leadingContent != null) {
            Box(Modifier.align(Alignment.CenterVertically)) {
                leadingContent()
            }
        }
        Box(
            contentModifier
                .padding(contentPadding)
                .weight(1f)
        ) {
            if (state.text.isEmpty() && placeholder != null) {
                placeholder()
            }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                textStyle = textStyle,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = {
                    onKeyboardAction()
                },
                lineLimits = lineLimits,
                outputTransformation = outputTransformation,
                scrollState = scrollState,
                cursorBrush = SolidColor(cursorColor)
            )
        }
        if (trailingContent != null) {
            Box(Modifier.align(Alignment.CenterVertically)) {
                trailingContent()
            }
        }
    }
}
