package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import raydroid.plugin.host.impl.generated.resources.Res
import raydroid.plugin.host.impl.generated.resources.live_results_reference
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.core.designsystem.component.rInteractable
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText

@Composable
fun RayDecorator(
    listItem: PluginCommandListItem,
    commandName: PluginUiText,
    pluginName: PluginUiText,
    focused: Boolean,
    modifier: Modifier = Modifier,
    title: PluginUiText? = listItem.title,
    onClick: () -> Unit = {},
    contextMenuSourceId: String? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier
            .rInteractable(
                focused = focused,
                contextMenuSourceId = contextMenuSourceId,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .background(
                if (focused) {
                    RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                } else {
                    RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                },
                shape = RaydroidTheme.shapes.medium
            )
            .padding(RaydroidTheme.spacing.small)
    ) {
        Box(Modifier.fillMaxWidth()) {
            content()
        }
        Row {
            title?.let { title ->
                TextRenderer(
                    PluginTextData(
                        text = title,
                        fontSize = PluginFontSize.ExtraSmall,
                        color = PluginColor.Outline
                    )
                )
            }
            TextRenderer(
                PluginTextData(
                    text = commandName,
                    fontSize = PluginFontSize.ExtraSmall,
                    color = PluginColor.OnSurfaceVariant
                )
            )
            Spacer(Modifier.weight(1f))
            RText(
                stringResource(Res.string.live_results_reference, pluginName.asText()),
                color = PluginColor.OnSurfaceVariant.toColor()
            )
        }
    }
}

@Preview
@Composable
private fun RayDecoratorPreview() {
    RaydroidPreviewTheme {
        RayDecorator(
            listItem = PluginCommandListItem(
                CommandItemId.Static,
                icon = null,
                title = PluginUiText.Plain("Hello"),
                description = null
            ), commandName = PluginUiText.Plain("Command"),
            pluginName = PluginUiText.Plain("Plugin"),
            focused = false,
            Modifier.fillMaxWidth()
        ) {
            RText("Hello")
        }
    }
}
