package ru.raydroid.plugin.host.impl.ui

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.api.ui.Alignment
import ru.raydroid.plugin.api.ui.Arrangement
import ru.raydroid.plugin.api.ui.BoxAlignment
import ru.raydroid.plugin.api.ui.BoxData
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.FontSize
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.IconData
import ru.raydroid.plugin.api.ui.IconSize
import ru.raydroid.plugin.api.ui.Image
import ru.raydroid.plugin.api.ui.ImageData
import ru.raydroid.plugin.api.ui.Orientation
import ru.raydroid.plugin.api.ui.OrientedBoxData
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.TextData
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.ui.PluginAlignment
import ru.raydroid.plugin.host.api.ui.PluginArrangement
import ru.raydroid.plugin.host.api.ui.PluginBoxAlignment
import ru.raydroid.plugin.host.api.ui.PluginBoxData
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginImageData
import ru.raydroid.plugin.host.api.ui.PluginOrientation
import ru.raydroid.plugin.host.api.ui.PluginOrientedBoxData
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginSpacing
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText

internal fun UiText.toPluginUiText(pluginId: PluginId): PluginUiText = when (type) {
    UiText.Type.Plain -> PluginUiText.Plain(text)
    UiText.Type.Resource -> PluginUiText.Resource(pluginId = pluginId, key = text)
}

internal fun Icon.toPluginIcon(pluginId: PluginId): PluginIcon = when (type) {
    Icon.Type.Url -> PluginIcon.Url(value)
    Icon.Type.Resource -> PluginIcon.Resource(pluginId = pluginId, key = value)
    Icon.Type.Base64 -> PluginIcon.Base64(value)
}

internal fun Image.toPluginImage(pluginId: PluginId): PluginImage = when (this) {
    is Image.Url -> PluginImage.Url(url)
    is Image.Resource -> PluginImage.Resource(pluginId = pluginId, key = resource)
}

internal fun Color.toPluginColor(): PluginColor = when (this) {
    Color.Primary -> PluginColor.Primary
    Color.PrimaryContainer -> PluginColor.PrimaryContainer
    Color.OnPrimary -> PluginColor.OnPrimary
    Color.OnPrimaryContainer -> PluginColor.OnPrimaryContainer
    Color.Secondary -> PluginColor.Secondary
    Color.OnSecondary -> PluginColor.OnSecondary
    Color.SecondaryContainer -> PluginColor.SecondaryContainer
    Color.OnSecondaryContainer -> PluginColor.OnSecondaryContainer
    Color.Tertiary -> PluginColor.Tertiary
    Color.OnTertiary -> PluginColor.OnTertiary
    Color.TertiaryContainer -> PluginColor.TertiaryContainer
    Color.OnTertiaryContainer -> PluginColor.OnTertiaryContainer
    Color.Error -> PluginColor.Error
    Color.ErrorContainer -> PluginColor.ErrorContainer
    Color.OnError -> PluginColor.OnError
    Color.OnErrorContainer -> PluginColor.OnErrorContainer
    Color.PrimaryFixed -> PluginColor.PrimaryFixed
    Color.PrimaryFixedDim -> PluginColor.PrimaryFixedDim
    Color.OnPrimaryFixed -> PluginColor.OnPrimaryFixed
    Color.OnPrimaryFixedVariant -> PluginColor.OnPrimaryFixedVariant
    Color.SecondaryFixed -> PluginColor.SecondaryFixed
    Color.SecondaryFixedDim -> PluginColor.SecondaryFixedDim
    Color.OnSecondaryFixed -> PluginColor.OnSecondaryFixed
    Color.OnSecondaryFixedVariant -> PluginColor.OnSecondaryFixedVariant
    Color.TertiaryFixed -> PluginColor.TertiaryFixed
    Color.TertiaryFixedDim -> PluginColor.TertiaryFixedDim
    Color.OnTertiaryFixed -> PluginColor.OnTertiaryFixed
    Color.OnTertiaryFixedVariant -> PluginColor.OnTertiaryFixedVariant
    Color.SurfaceDim -> PluginColor.SurfaceDim
    Color.Surface -> PluginColor.Surface
    Color.SurfaceBright -> PluginColor.SurfaceBright
    Color.SurfaceContainerLowest -> PluginColor.SurfaceContainerLowest
    Color.SurfaceContainerLow -> PluginColor.SurfaceContainerLow
    Color.SurfaceContainer -> PluginColor.SurfaceContainer
    Color.SurfaceContainerHigh -> PluginColor.SurfaceContainerHigh
    Color.SurfaceContainerHighest -> PluginColor.SurfaceContainerHighest
    Color.OnSurface -> PluginColor.OnSurface
    Color.OnSurfaceVariant -> PluginColor.OnSurfaceVariant
    Color.Outline -> PluginColor.Outline
    Color.OutlineVariant -> PluginColor.OutlineVariant
    Color.InverseSurface -> PluginColor.InverseSurface
    Color.InverseOnSurface -> PluginColor.InverseOnSurface
    Color.InversePrimary -> PluginColor.InversePrimary
    Color.Scrim -> PluginColor.Scrim
    Color.Transparent -> PluginColor.Transparent
}

internal fun FontSize.toPluginFontSize(): PluginFontSize = when (this) {
    FontSize.ExtraSmall -> PluginFontSize.ExtraSmall
    FontSize.Small -> PluginFontSize.Small
    FontSize.Medium -> PluginFontSize.Medium
    FontSize.Large -> PluginFontSize.Large
    FontSize.ExtraLarge -> PluginFontSize.ExtraLarge
}

internal fun IconSize.toPluginIconSize(): PluginIconSize = when (this) {
    IconSize.ExtraSmall -> PluginIconSize.ExtraSmall
    IconSize.Small -> PluginIconSize.Small
    IconSize.Medium -> PluginIconSize.Medium
    IconSize.Large -> PluginIconSize.Large
    IconSize.ExtraLarge -> PluginIconSize.ExtraLarge
}

internal fun Spacing.toPluginSpacing(): PluginSpacing = when (this) {
    Spacing.Zero -> PluginSpacing.Zero
    Spacing.Minimal -> PluginSpacing.Minimal
    Spacing.Border -> PluginSpacing.Border
    Spacing.ExtraSmall -> PluginSpacing.ExtraSmall
    Spacing.Small -> PluginSpacing.Small
    Spacing.Medium -> PluginSpacing.Medium
    Spacing.Large -> PluginSpacing.Large
    Spacing.ExtraLarge -> PluginSpacing.ExtraLarge
}

internal fun Alignment.toPluginAlignment(): PluginAlignment = when (this) {
    Alignment.Start -> PluginAlignment.Start
    Alignment.Center -> PluginAlignment.Center
    Alignment.End -> PluginAlignment.End
}

internal fun BoxAlignment.toPluginBoxAlignment(): PluginBoxAlignment = when (this) {
    BoxAlignment.TopStart -> PluginBoxAlignment.TopStart
    BoxAlignment.TopCenter -> PluginBoxAlignment.TopCenter
    BoxAlignment.TopEnd -> PluginBoxAlignment.TopEnd
    BoxAlignment.CenterStart -> PluginBoxAlignment.CenterStart
    BoxAlignment.Center -> PluginBoxAlignment.Center
    BoxAlignment.CenterEnd -> PluginBoxAlignment.CenterEnd
    BoxAlignment.BottomStart -> PluginBoxAlignment.BottomStart
    BoxAlignment.BottomCenter -> PluginBoxAlignment.BottomCenter
    BoxAlignment.BottomEnd -> PluginBoxAlignment.BottomEnd
}

internal fun Orientation.toPluginOrientation(): PluginOrientation = when (this) {
    Orientation.Vertical -> PluginOrientation.Vertical
    Orientation.Horizontal -> PluginOrientation.Horizontal
}

internal fun Arrangement.toPluginArrangement(): PluginArrangement = when (this) {
    Arrangement.Start -> PluginArrangement.Start
    Arrangement.Center -> PluginArrangement.Center
    Arrangement.End -> PluginArrangement.End
    Arrangement.SpaceBetween -> PluginArrangement.SpaceBetween
    Arrangement.SpaceAround -> PluginArrangement.SpaceAround
    Arrangement.SpaceEvenly -> PluginArrangement.SpaceEvenly
}

internal fun CommandListItem.toPluginCommandListItem(pluginId: PluginId): PluginCommandListItem {
    return PluginCommandListItem(
        id = id,
        icon = icon?.toPluginIcon(pluginId),
        title = title?.toPluginUiText(pluginId),
        description = description?.toPluginUiText(pluginId),
        actions = actions.map { it.toPluginCommandListAction(pluginId) }
    )
}

internal fun CommandListAction.toPluginCommandListAction(pluginId: PluginId): PluginCommandListAction {
    return PluginCommandListAction(
        id = id,
        title = title.toPluginUiText(pluginId),
        description = description?.toPluginUiText(pluginId),
        icon = icon?.toPluginIcon(pluginId),
        group = group?.toPluginUiText(pluginId),
        style = style.toPluginStyle(),
        primary = primary
    )
}

internal fun CommandPresentation.toPluginCommandPresentation(pluginId: PluginId): PluginCommandPresentation {
    return PluginCommandPresentation(
        listEntry = listEntry.toPluginCommandListItem(pluginId),
        content = content.map { it.toPluginRayNodeData(pluginId) }
    )
}

internal fun RayNodeData.toPluginRayNodeData(pluginId: PluginId): PluginRayNodeData = when (this) {
    is BoxData -> PluginBoxData(
        alignment = alignment.toPluginBoxAlignment(),
        children = children.map { it.toPluginRayNodeData(pluginId) }
    )
    is OrientedBoxData -> PluginOrientedBoxData(
        orientation = orientation.toPluginOrientation(),
        alignment = alignment.toPluginAlignment(),
        arrangement = arrangement.toPluginArrangement(),
        spacing = spacing.toPluginSpacing(),
        children = children.map { it.toPluginRayNodeData(pluginId) }
    )
    is TextData -> PluginTextData(
        text = text.toPluginUiText(pluginId),
        fontSize = fontSize.toPluginFontSize(),
        color = color.toPluginColor()
    )
    is IconData -> PluginIconData(
        icon = icon.toPluginIcon(pluginId),
        contentDescription = contentDescription,
        size = size.toPluginIconSize()
    )
    is ImageData -> PluginImageData(
        image = image.toPluginImage(pluginId),
        contentDescription = contentDescription,
        width = width,
        height = height
    )
}

private fun CommandListAction.Style.toPluginStyle(): PluginCommandListAction.Style = when (this) {
    CommandListAction.Style.Default -> PluginCommandListAction.Style.Default
    CommandListAction.Style.Destructive -> PluginCommandListAction.Style.Destructive
}
