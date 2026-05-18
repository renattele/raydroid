package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.core.designsystem.component.rInteractable
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandListQuickAction
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText

@Composable
fun SearchListItem(
    result: SearchResultSet.CachedSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    contextMenuSourceId: String? = null,
    onLongClick: (() -> Unit)? = null,
    onQuickAction: (() -> Unit)? = null
) {
    val spacing = RaydroidTheme.spacing
    Row(
        modifier
            .rInteractable(
                focused = focused,
                contextMenuSourceId = contextMenuSourceId,
                onLongClick = onLongClick
            ) { onClick() }
            .fillMaxWidth()
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            val highlightStyle = SpanStyle(
                color = PluginColor.Primary.toColor(),
                fontWeight = FontWeight.SemiBold
            )
            val title = result.listEntry.title
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RText(
                        text = title.asText().highlight(result.titleMatches, highlightStyle),
                        fontSize = PluginFontSize.Large.toTextUnit(),
                        color = PluginColor.OnSurface.toColor(),
                        modifier = Modifier.weight(1f)
                    )
                    result.listEntry.alias?.let { alias ->
                        AliasBadge(alias)
                    }
                }
            }
            val description = result.listEntry.description
            if (description != null) {
                RText(
                    text = description.asText().highlight(result.descriptionMatches, highlightStyle),
                    fontSize = PluginFontSize.Small.toTextUnit(),
                    color = PluginColor.OnSurfaceVariant.toColor()
                )
            }
        }
        result.listEntry.quickAction?.let { quickAction ->
            if (onQuickAction != null) {
                QuickActionButton(quickAction, onClick = onQuickAction)
            }
        }
    }
}

@Composable
fun CommandListItemView(
    listEntry: PluginCommandListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    contextMenuSourceId: String? = null,
    onLongClick: (() -> Unit)? = null,
    onQuickAction: (() -> Unit)? = null
) {
    val spacing = RaydroidTheme.spacing
    val shape = RaydroidTheme.shapes.shape(RaydroidShapeToken.Medium)
    val hasInlineResult = listEntry.trailingText != null
    Row(
        modifier
            .rInteractable(
                focused = focused,
                contextMenuSourceId = contextMenuSourceId,
                onLongClick = onLongClick
            ) { onClick() }
            .fillMaxWidth()
            .background(
                if (hasInlineResult) {
                    RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
                shape = shape
            )
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        val icon = listEntry.icon
        if (icon != null) {
            IconRenderer(
                data = PluginIconData(
                    icon = icon,
                    color = listEntry.iconColor
                )
            )
        }
        val title = listEntry.title
        val trailingText = listEntry.trailingText
        val titleText = title?.asText()
        val trailingValue = trailingText?.asText()
        val placeTrailingBelow = shouldPlaceTrailingBelow(titleText.orEmpty(), trailingValue)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = if (placeTrailingBelow) Alignment.Top else Alignment.CenterVertically
                ) {
                    if (trailingValue == null || titleText == null) {
                        TextRenderer(
                            data = PluginTextData(
                                text = title,
                                fontSize = PluginFontSize.Large
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val expressionModifier = if (placeTrailingBelow) {
                            Modifier
                                .heightIn(max = CalculationExpressionMaxHeight)
                        } else {
                            Modifier
                        }
                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier.weight(1f).then(expressionModifier),
                            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                            verticalAlignment = Alignment.Top
                        ) {
                            RText(
                                text = titleText.withCalculationBreaks(),
                                fontSize = titleText.trailingTitleFontSize(placeTrailingBelow).toTextUnit(),
                                color = PluginColor.OnSurface.toColor(),
                                maxLines = if (placeTrailingBelow) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Clip,
                                modifier = if (placeTrailingBelow) {
                                    Modifier
                                        .weight(1f)
                                        .verticalScroll(scrollState)
                                } else {
                                    Modifier.weight(1f)
                                }
                            )
                            if (placeTrailingBelow) {
                                CalculationScrollHandle(scrollState)
                            }
                        }
                    }
                    listEntry.alias?.let { alias ->
                        AliasBadge(alias)
                    }
                }
            }
            val description = listEntry.description
            if (description != null) {
                TextRenderer(
                    data = PluginTextData(
                        text = description,
                        fontSize = PluginFontSize.Small,
                        color = PluginColor.OnSurfaceVariant
                    )
                )
            }
            if (placeTrailingBelow) {
                RText(
                    text = trailingValue.orEmpty(),
                    fontSize = PluginFontSize.Large.toTextUnit(),
                    color = PluginColor.OnSurface.toColor(),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        if (!placeTrailingBelow && trailingValue != null) {
            RText(
                text = trailingValue,
                fontSize = PluginFontSize.Large.toTextUnit(),
                color = PluginColor.OnSurface.toColor(),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier.widthIn(min = 40.dp)
            )
        }
        listEntry.quickAction?.let { quickAction ->
            if (onQuickAction != null) {
                QuickActionButton(quickAction, onClick = onQuickAction)
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    quickAction: PluginCommandListQuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(QuickActionButtonSize)
            .clip(RaydroidTheme.shapes.full)
            .background(PluginColor.TertiaryContainer.toColor())
            .rInteractable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        IconRenderer(
            data = PluginIconData(
                icon = quickAction.icon,
                contentDescription = quickAction.title.asText(),
                size = PluginIconSize.Small,
                color = PluginColor.OnTertiaryContainer
            )
        )
    }
}

@Composable
private fun AliasBadge(alias: String, modifier: Modifier = Modifier) {
    RText(
        text = alias,
        fontSize = PluginFontSize.ExtraSmall.toTextUnit(),
        color = PluginColor.OnSecondaryContainer.toColor(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RaydroidTheme.shapes.small)
            .background(PluginColor.SecondaryContainer.toColor().copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun CalculationScrollHandle(scrollState: ScrollState) {
    val shouldShow by remember {
        derivedStateOf { scrollState.maxValue > 0 }
    }
    if (!shouldShow) return

    val progress by remember {
        derivedStateOf {
            if (scrollState.maxValue == 0) {
                0f
            } else {
                scrollState.value.toFloat() / scrollState.maxValue
            }
        }
    }
    val shape = RaydroidTheme.shapes.extraSmall
    val thumbOffset = CalculationScrollThumbTravel * progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .width(CalculationScrollTrackWidth)
            .height(CalculationExpressionMaxHeight)
            .clip(shape)
            .background(PluginColor.OnSurfaceVariant.toColor().copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = thumbOffset)
                .width(CalculationScrollTrackWidth)
                .height(CalculationScrollThumbHeight)
                .clip(shape)
                .background(PluginColor.Primary.toColor().copy(alpha = 0.72f))
        )
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

private fun shouldPlaceTrailingBelow(title: String, trailing: String?): Boolean {
    if (trailing == null) return false
    return title.length > InlineTrailingTitleThreshold ||
        trailing.length > InlineTrailingValueThreshold ||
        title.length + trailing.length > InlineCombinedTrailingThreshold
}

private fun String.trailingTitleFontSize(compact: Boolean): PluginFontSize = when {
    compact && length > InlineTrailingTitleThreshold -> PluginFontSize.Small
    length <= InlineTrailingTitleThreshold -> PluginFontSize.Large
    length <= CompactTrailingTitleThreshold -> PluginFontSize.Medium
    else -> PluginFontSize.Small
}

private fun String.withCalculationBreaks(): String {
    if (length <= InlineTrailingTitleThreshold) return this
    return buildString(length + count { it in CalculationBreakChars }) {
        this@withCalculationBreaks.forEach { char ->
            append(char)
            if (char in CalculationBreakChars) {
                append(ZeroWidthSpace)
            }
        }
    }
}

private const val InlineTrailingTitleThreshold = 24
private const val InlineTrailingValueThreshold = 8
private const val InlineCombinedTrailingThreshold = 28
private const val CompactTrailingTitleThreshold = 48
private const val ZeroWidthSpace = '\u200B'
private val CalculationExpressionMaxHeight = 220.dp
private val QuickActionButtonSize = 40.dp
private val CalculationScrollTrackWidth = 3.dp
private val CalculationScrollThumbHeight = 44.dp
private val CalculationScrollThumbTravel: Dp = CalculationExpressionMaxHeight - CalculationScrollThumbHeight
private val CalculationBreakChars = setOf('+', '-', '*', '/', '%', '^')
