package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import ru.raydroid.core.designsystem.RaydroidMotionToken
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText

@Composable
fun SearchListItem(
    result: SearchResultSet.CachedSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false
) {
    val spacing = RaydroidTheme.spacing
    val shape = RaydroidTheme.shapes.shape(RaydroidShapeToken.Medium)
    Row(
        modifier
            .interactable(focused = focused) { onClick() }
            .fillMaxWidth()
            .clip(shape)
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        val icon = result.listEntry.icon
        if (icon != null) {
            IconRenderer(
                data = PluginIconData(
                    icon = icon
                )
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            val highlightStyle = SpanStyle(
                color = PluginColor.Primary.toColor(),
                fontWeight = FontWeight.SemiBold
            )
            val title = result.listEntry.title
            if (title != null) {
                Text(
                    text = title.asText().highlight(result.titleMatches, highlightStyle),
                    fontSize = PluginFontSize.Large.toTextUnit()
                )
            }
            val description = result.listEntry.description
            if (description != null) {
                Text(
                    text = description.asText().highlight(result.descriptionMatches, highlightStyle),
                    fontSize = PluginFontSize.Small.toTextUnit(),
                    color = PluginColor.OutlineVariant.toColor()
                )
            }
        }
    }
}

@Composable
fun CommandListItemView(
    listEntry: PluginCommandListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false
) {
    val spacing = RaydroidTheme.spacing
    val shape = RaydroidTheme.shapes.shape(RaydroidShapeToken.Medium)
    Row(
        modifier
            .interactable(focused = focused) { onClick() }
            .fillMaxWidth()
            .clip(shape)
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        val icon = listEntry.icon
        if (icon != null) {
            IconRenderer(
                data = PluginIconData(
                    icon = icon
                )
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            val title = listEntry.title
            if (title != null) {
                TextRenderer(
                    data = PluginTextData(
                        text = title,
                        fontSize = PluginFontSize.Large
                    )
                )
            }
            val description = listEntry.description
            if (description != null) {
                TextRenderer(
                    data = PluginTextData(
                        text = description,
                        fontSize = PluginFontSize.Small,
                        color = PluginColor.OutlineVariant
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
            listEntry = PluginCommandListItem(
                id = CommandItemId("1"),
                icon = PluginIcon.Url("https://i.imgur.com/UVpA9a0.jpeg"),
                title = PluginUiText.Plain("123"),
                description = PluginUiText.Resource(PluginId.Invalid, "456")
            ),
            onClick = {}
        )
    }
}

private fun String.highlight(
    ranges: List<IntRange>,
    style: SpanStyle
): AnnotatedString {
    if (ranges.isEmpty()) return AnnotatedString(this)
    return AnnotatedString.Builder(this).apply {
        ranges.forEach { range ->
            val start = range.first.coerceIn(0, length)
            val endExclusive = (range.last + 1).coerceIn(0, length)
            if (start < endExclusive) {
                addStyle(style, start, endExclusive)
            }
        }
    }.toAnnotatedString()
}
