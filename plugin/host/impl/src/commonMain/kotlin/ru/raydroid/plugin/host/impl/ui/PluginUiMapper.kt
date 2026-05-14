package ru.raydroid.plugin.host.impl.ui

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.api.ui.Alignment
import ru.raydroid.plugin.api.ui.ActionPanelHintMode
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
import ru.raydroid.plugin.api.ui.MotionToken
import ru.raydroid.plugin.api.ui.Orientation
import ru.raydroid.plugin.api.ui.OrientedBoxData
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.RayModifier
import ru.raydroid.plugin.api.ui.ShapeToken
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.TextData
import ru.raydroid.plugin.api.ui.DetailData
import ru.raydroid.plugin.api.ui.DetailMetadataItemData
import ru.raydroid.plugin.api.ui.EmptyViewData
import ru.raydroid.plugin.api.ui.FormData
import ru.raydroid.plugin.api.ui.FormFieldData
import ru.raydroid.plugin.api.ui.FormSubmitStyle
import ru.raydroid.plugin.api.ui.FormValue
import ru.raydroid.plugin.api.ui.FormValues
import ru.raydroid.plugin.api.ui.GridAspectRatio
import ru.raydroid.plugin.api.ui.GridData
import ru.raydroid.plugin.api.ui.GridItemData
import ru.raydroid.plugin.api.ui.GridSectionData
import ru.raydroid.plugin.api.ui.ListData
import ru.raydroid.plugin.api.ui.ListItemData
import ru.raydroid.plugin.api.ui.ListSectionData
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.ui.PluginAlignment
import ru.raydroid.plugin.host.api.ui.PluginActionPanelHintMode
import ru.raydroid.plugin.host.api.ui.PluginArrangement
import ru.raydroid.plugin.host.api.ui.PluginBoxAlignment
import ru.raydroid.plugin.host.api.ui.PluginBoxData
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation
import ru.raydroid.plugin.host.api.ui.PluginDetailData
import ru.raydroid.plugin.host.api.ui.PluginDetailMetadataItemData
import ru.raydroid.plugin.host.api.ui.PluginEmptyViewData
import ru.raydroid.plugin.host.api.ui.PluginFormData
import ru.raydroid.plugin.host.api.ui.PluginFormFieldData
import ru.raydroid.plugin.host.api.ui.PluginFormSubmitCallback
import ru.raydroid.plugin.host.api.ui.PluginFormSubmitData
import ru.raydroid.plugin.host.api.ui.PluginFormSubmitStyle
import ru.raydroid.plugin.host.api.ui.PluginFormValue
import ru.raydroid.plugin.host.api.ui.PluginFormValues
import ru.raydroid.plugin.host.api.ui.PluginGridAspectRatio
import ru.raydroid.plugin.host.api.ui.PluginGridData
import ru.raydroid.plugin.host.api.ui.PluginGridItemData
import ru.raydroid.plugin.host.api.ui.PluginGridSectionData
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginImageData
import ru.raydroid.plugin.host.api.ui.PluginListData
import ru.raydroid.plugin.host.api.ui.PluginListItemData
import ru.raydroid.plugin.host.api.ui.PluginListSectionData
import ru.raydroid.plugin.host.api.ui.PluginMotionToken
import ru.raydroid.plugin.host.api.ui.PluginOrientation
import ru.raydroid.plugin.host.api.ui.PluginOrientedBoxData
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginRayModifier
import ru.raydroid.plugin.host.api.ui.PluginShapeToken
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
    Icon.Type.Builtin -> PluginIcon.Builtin(value)
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

internal fun ShapeToken.toPluginShapeToken(): PluginShapeToken = when (this) {
    ShapeToken.None -> PluginShapeToken.None
    ShapeToken.ExtraSmall -> PluginShapeToken.ExtraSmall
    ShapeToken.Small -> PluginShapeToken.Small
    ShapeToken.Medium -> PluginShapeToken.Medium
    ShapeToken.Large -> PluginShapeToken.Large
    ShapeToken.ExtraLarge -> PluginShapeToken.ExtraLarge
    ShapeToken.Full -> PluginShapeToken.Full
}

internal fun MotionToken.toPluginMotionToken(): PluginMotionToken = when (this) {
    MotionToken.None -> PluginMotionToken.None
    MotionToken.Fast -> PluginMotionToken.Fast
    MotionToken.Default -> PluginMotionToken.Default
    MotionToken.Emphasized -> PluginMotionToken.Emphasized
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
        enabled = enabled,
    )
}

internal fun CommandListAction.toPluginCommandListAction(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit = {}
): PluginCommandListAction {
    return PluginCommandListAction(
        callback = callback.toPluginCommandCallback(dispatchCallback),
        title = title.toPluginUiText(pluginId),
        description = description?.toPluginUiText(pluginId),
        icon = icon?.toPluginIcon(pluginId),
        group = group?.toPluginUiText(pluginId),
        style = style.toPluginStyle(),
        primary = primary,
        enabled = enabled,
    )
}

internal fun CommandPresentation.toPluginCommandPresentation(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit = {},
    dispatchFormCallback: suspend (CommandCallbackRef, FormValues) -> Unit = { _, _ -> }
): PluginCommandPresentation {
    return PluginCommandPresentation(
        listEntry = listEntry.toPluginCommandListItem(pluginId),
        primaryCallback = primaryCallback?.toPluginCommandCallback(dispatchCallback),
        actions = actions.map { action -> action.toPluginCommandListAction(pluginId, dispatchCallback) },
        content = content.map { it.toPluginRayNodeData(pluginId, dispatchCallback, dispatchFormCallback) }
    )
}

internal fun RayNodeData.toPluginRayNodeData(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit = {},
    dispatchFormCallback: suspend (CommandCallbackRef, FormValues) -> Unit = { _, _ -> }
): PluginRayNodeData = when (this) {
    is BoxData -> PluginBoxData(
        alignment = alignment.toPluginBoxAlignment(),
        shape = shape.toPluginShapeToken(),
        children = children.map { it.toPluginRayNodeData(pluginId, dispatchCallback, dispatchFormCallback) }
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is OrientedBoxData -> PluginOrientedBoxData(
        orientation = orientation.toPluginOrientation(),
        alignment = alignment.toPluginAlignment(),
        arrangement = arrangement.toPluginArrangement(),
        spacing = spacing.toPluginSpacing(),
        shape = shape.toPluginShapeToken(),
        children = children.map { it.toPluginRayNodeData(pluginId, dispatchCallback, dispatchFormCallback) }
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is TextData -> PluginTextData(
        text = text.toPluginUiText(pluginId),
        fontSize = fontSize.toPluginFontSize(),
        color = color.toPluginColor()
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is IconData -> PluginIconData(
        icon = icon.toPluginIcon(pluginId),
        contentDescription = contentDescription,
        size = size.toPluginIconSize()
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is ImageData -> PluginImageData(
        image = image.toPluginImage(pluginId),
        contentDescription = contentDescription,
        width = width,
        height = height,
        shape = shape.toPluginShapeToken()
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is DetailData -> toPluginDetailData(pluginId).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is FormData -> PluginFormData(
        isLoading = isLoading,
        navigationTitle = navigationTitle?.toPluginUiText(pluginId),
        fields = fields.map { it.toPluginFormFieldData(pluginId) },
        submit = submit?.let { submit ->
            PluginFormSubmitData(
                title = submit.title.toPluginUiText(pluginId),
                callback = PluginFormSubmitCallback { values ->
                    dispatchFormCallback(submit.callback, values.toApiFormValues())
                },
                icon = submit.icon?.toPluginIcon(pluginId),
                style = submit.style.toPluginFormSubmitStyle(),
                enabled = submit.enabled
            )
        },
        suppressHostActions = suppressHostActions,
        requireChanges = requireChanges,
        unchangedView = unchangedView?.toPluginEmptyViewData(pluginId),
        actionPanelHintMode = actionPanelHintMode.toPluginActionPanelHintMode()
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is ListData -> PluginListData(
        sections = sections.map { it.toPluginListSectionData(pluginId, dispatchCallback, dispatchFormCallback) },
        emptyView = emptyView?.toPluginEmptyViewData(pluginId),
        isLoading = isLoading,
        filtering = filtering,
        searchBarPlaceholder = searchBarPlaceholder?.toPluginUiText(pluginId),
        lazy = lazy
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
    is GridData -> PluginGridData(
        sections = sections.map { it.toPluginGridSectionData(pluginId, dispatchCallback) },
        emptyView = emptyView?.toPluginEmptyViewData(pluginId),
        isLoading = isLoading,
        filtering = filtering,
        searchBarPlaceholder = searchBarPlaceholder?.toPluginUiText(pluginId),
        columns = columns,
        aspectRatio = aspectRatio.toPluginGridAspectRatio(),
        lazy = lazy
    ).withModifier(modifier.toPluginRayModifier(pluginId, dispatchCallback))
}

private fun DetailData.toPluginDetailData(pluginId: PluginId): PluginDetailData = PluginDetailData(
    markdown = markdown,
    metadata = metadata.map { it.toPluginDetailMetadataItemData(pluginId) },
    isLoading = isLoading,
    navigationTitle = navigationTitle?.toPluginUiText(pluginId)
)

private fun DetailMetadataItemData.toPluginDetailMetadataItemData(pluginId: PluginId): PluginDetailMetadataItemData =
    when (this) {
        is DetailMetadataItemData.Label -> PluginDetailMetadataItemData.Label(
            title = title.toPluginUiText(pluginId),
            text = text?.toPluginUiText(pluginId),
            icon = icon?.toPluginIcon(pluginId)
        )
        is DetailMetadataItemData.Link -> PluginDetailMetadataItemData.Link(
            title = title.toPluginUiText(pluginId),
            text = text.toPluginUiText(pluginId),
            target = target
        )
        is DetailMetadataItemData.TagList -> PluginDetailMetadataItemData.TagList(
            title = title.toPluginUiText(pluginId),
            tags = tags.map { tag ->
                PluginDetailMetadataItemData.TagList.Tag(
                    text = tag.text?.toPluginUiText(pluginId),
                    icon = tag.icon?.toPluginIcon(pluginId)
                )
            }
        )
        DetailMetadataItemData.Separator -> PluginDetailMetadataItemData.Separator
    }

private fun FormFieldData.toPluginFormFieldData(pluginId: PluginId): PluginFormFieldData = when (this) {
    is FormFieldData.TextField -> PluginFormFieldData.TextField(
        id = id,
        title = title?.toPluginUiText(pluginId),
        required = required,
        placeholder = placeholder?.toPluginUiText(pluginId),
        defaultValue = defaultValue,
        password = password,
        multiline = multiline
    )
    is FormFieldData.Checkbox -> PluginFormFieldData.Checkbox(
        id = id,
        title = title?.toPluginUiText(pluginId),
        required = required,
        defaultValue = defaultValue
    )
    is FormFieldData.Dropdown -> PluginFormFieldData.Dropdown(
        id = id,
        title = title?.toPluginUiText(pluginId),
        required = required,
        options = options.map { option ->
            PluginFormFieldData.Dropdown.Option(
                value = option.value,
                title = option.title.toPluginUiText(pluginId),
                icon = option.icon?.toPluginIcon(pluginId)
            )
        },
        defaultValue = defaultValue
    )
    is FormFieldData.DatePicker -> PluginFormFieldData.DatePicker(
        id = id,
        title = title?.toPluginUiText(pluginId),
        required = required,
        defaultValue = defaultValue
    )
    is FormFieldData.Separator -> PluginFormFieldData.Separator(id)
    is FormFieldData.Description -> PluginFormFieldData.Description(id, text.toPluginUiText(pluginId))
}

private fun ListSectionData.toPluginListSectionData(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit,
    dispatchFormCallback: suspend (CommandCallbackRef, FormValues) -> Unit
): PluginListSectionData = PluginListSectionData(
    title = title?.toPluginUiText(pluginId),
    items = items.map { it.toPluginListItemData(pluginId, dispatchCallback, dispatchFormCallback) }
)

private fun ListItemData.toPluginListItemData(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit,
    dispatchFormCallback: suspend (CommandCallbackRef, FormValues) -> Unit
): PluginListItemData = PluginListItemData(
    id = id,
    title = title.toPluginUiText(pluginId),
    subtitle = subtitle?.toPluginUiText(pluginId),
    icon = icon?.toPluginIcon(pluginId),
    keywords = keywords,
    detail = detail?.toPluginDetailData(pluginId),
    content = content.map { it.toPluginRayNodeData(pluginId, dispatchCallback, dispatchFormCallback) },
    itemModifier = modifier.toPluginRayModifier(pluginId, dispatchCallback)
)

private fun GridSectionData.toPluginGridSectionData(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit
): PluginGridSectionData = PluginGridSectionData(
    title = title?.toPluginUiText(pluginId),
    items = items.map { it.toPluginGridItemData(pluginId, dispatchCallback) }
)

private fun GridItemData.toPluginGridItemData(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit
): PluginGridItemData = PluginGridItemData(
    id = id,
    title = title.toPluginUiText(pluginId),
    subtitle = subtitle?.toPluginUiText(pluginId),
    content = content?.toPluginImage(pluginId),
    icon = icon?.toPluginIcon(pluginId),
    keywords = keywords,
    itemModifier = modifier.toPluginRayModifier(pluginId, dispatchCallback)
)

private fun EmptyViewData.toPluginEmptyViewData(pluginId: PluginId): PluginEmptyViewData =
    PluginEmptyViewData(
        title = title.toPluginUiText(pluginId),
        description = description?.toPluginUiText(pluginId),
        icon = icon?.toPluginIcon(pluginId)
    )

private fun GridAspectRatio.toPluginGridAspectRatio(): PluginGridAspectRatio = when (this) {
    GridAspectRatio.OneToOne -> PluginGridAspectRatio.OneToOne
    GridAspectRatio.ThreeToTwo -> PluginGridAspectRatio.ThreeToTwo
    GridAspectRatio.TwoToThree -> PluginGridAspectRatio.TwoToThree
    GridAspectRatio.FourToThree -> PluginGridAspectRatio.FourToThree
    GridAspectRatio.ThreeToFour -> PluginGridAspectRatio.ThreeToFour
    GridAspectRatio.SixteenToNine -> PluginGridAspectRatio.SixteenToNine
    GridAspectRatio.NineToSixteen -> PluginGridAspectRatio.NineToSixteen
}

private fun FormSubmitStyle.toPluginFormSubmitStyle(): PluginFormSubmitStyle = when (this) {
    FormSubmitStyle.Filled -> PluginFormSubmitStyle.Filled
    FormSubmitStyle.Tonal -> PluginFormSubmitStyle.Tonal
}

private fun ActionPanelHintMode.toPluginActionPanelHintMode(): PluginActionPanelHintMode = when (this) {
    ActionPanelHintMode.Full -> PluginActionPanelHintMode.Full
    ActionPanelHintMode.MenuOnly -> PluginActionPanelHintMode.MenuOnly
    ActionPanelHintMode.Hidden -> PluginActionPanelHintMode.Hidden
}

private fun PluginFormValues.toApiFormValues(): FormValues = mapValues { (_, value) ->
    when (value) {
        is PluginFormValue.Text -> FormValue.Text(value.value)
        is PluginFormValue.BooleanValue -> FormValue.BooleanValue(value.value)
        is PluginFormValue.DateValue -> FormValue.DateValue(value.value)
    }
}

private fun CommandListAction.Style.toPluginStyle(): PluginCommandListAction.Style = when (this) {
    CommandListAction.Style.Default -> PluginCommandListAction.Style.Default
    CommandListAction.Style.Destructive -> PluginCommandListAction.Style.Destructive
}

private fun RayModifier?.toPluginRayModifier(
    pluginId: PluginId,
    dispatchCallback: suspend (CommandCallbackRef) -> Unit
): PluginRayModifier? {
    if (this == null) {
        return null
    }
    return PluginRayModifier(
        enabled = enabled,
        actions = actions.map { action -> action.toPluginCommandListAction(pluginId, dispatchCallback) }
    )
}

private fun CommandCallbackRef.toPluginCommandCallback(
    dispatchCallback: suspend (CommandCallbackRef) -> Unit
): PluginCommandCallback {
    return PluginCommandCallback(
        ref = this,
        dispatch = dispatchCallback
    )
}

private fun <T : PluginRayNodeData> T.withModifier(modifier: PluginRayModifier?): T {
    this.modifier = modifier
    return this
}
