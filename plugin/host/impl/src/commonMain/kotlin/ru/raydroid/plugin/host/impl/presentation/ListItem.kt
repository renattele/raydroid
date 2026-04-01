package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import raydroid.plugin.host.impl.generated.resources.Res
import raydroid.plugin.host.impl.generated.resources.search_field_placeholder
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.FontSize
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.IconData
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.TextData
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet

@Composable
fun SearchListItem(
    result: SearchResultSet.CachedSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false
) {
    CommandListItemView(result.listEntry, onClick, modifier, focused)
}

@Composable
fun CommandListItemView(
    listEntry: CommandListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false
) {
    val themeResolver = LocalThemeResolver.current
    val backgroundColor = if (focused) {
        themeResolver.color(Color.SurfaceBright)
    } else {
        themeResolver.color(Color.Surface)
    }
    Row(
        modifier
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(themeResolver.spacing(Spacing.Medium))
    ) {
        val icon = listEntry.icon
        if (icon != null) {
            IconRenderer(
                data = IconData(
                    icon = icon
                )
            )
        }
        Column {
            val title = listEntry.title
            if (title != null) {
                TextRenderer(
                    data = TextData(
                        text = title,
                        fontSize = FontSize.Large
                    )
                )
            }
            val description = listEntry.description
            if (description != null) {
                TextRenderer(
                    data = TextData(
                        text = description,
                        fontSize = FontSize.Small,
                        color = Color.OutlineVariant
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun ListItemPreview() {
    RaydroidPreviewTheme {
        CommandListItemView(
            listEntry = CommandListItem(
                id = CommandItemId("1"),
                icon = Icon.Url("https://i.imgur.com/UVpA9a0.jpeg"),
                title = UiText.Plain("123"),
                description = UiText.Resource("456")
            ),
            onClick = {

            }
        )
    }
}
