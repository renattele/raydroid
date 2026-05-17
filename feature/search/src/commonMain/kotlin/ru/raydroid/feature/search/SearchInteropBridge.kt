package ru.raydroid.feature.search

import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.ui.PluginActionPanelHintMode
import ru.raydroid.plugin.host.api.ui.PluginBoxData
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginDetailData
import ru.raydroid.plugin.host.api.ui.PluginDetailMetadataItemData
import ru.raydroid.plugin.host.api.ui.PluginEditableTextData
import ru.raydroid.plugin.host.api.ui.PluginEmptyViewData
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginFormData
import ru.raydroid.plugin.host.api.ui.PluginFormFieldData
import ru.raydroid.plugin.host.api.ui.PluginFormSubmitCallback
import ru.raydroid.plugin.host.api.ui.PluginFormSubmitData
import ru.raydroid.plugin.host.api.ui.PluginGridAspectRatio
import ru.raydroid.plugin.host.api.ui.PluginGridData
import ru.raydroid.plugin.host.api.ui.PluginGridItemData
import ru.raydroid.plugin.host.api.ui.PluginGridSectionData
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginImageData
import ru.raydroid.plugin.host.api.ui.PluginListData
import ru.raydroid.plugin.host.api.ui.PluginListItemData
import ru.raydroid.plugin.host.api.ui.PluginListSectionData
import ru.raydroid.plugin.host.api.ui.PluginOrientedBoxData
import ru.raydroid.plugin.host.api.ui.PluginRayModifier
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginShapeToken
import ru.raydroid.plugin.host.api.ui.PluginSpacing
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.actionPanelHintMode
import ru.raydroid.plugin.host.api.ui.pluginFocusModel
import ru.raydroid.plugin.host.api.ui.suppressesHostActions

object SearchInteropBridge {
    fun nodeKind(node: PluginRayNodeData): String = when (node) {
        is PluginTextData -> "text"
        is PluginIconData -> "icon"
        is PluginImageData -> "image"
        is PluginBoxData -> "box"
        is PluginOrientedBoxData -> "orientedBox"
        is PluginDetailData -> "detail"
        is PluginEditableTextData -> "editableText"
        is PluginFormData -> "form"
        is PluginListData -> "list"
        is PluginGridData -> "grid"
    }

    fun nodeChildren(node: PluginRayNodeData): List<PluginRayNodeData> = when (node) {
        is PluginBoxData -> node.children
        is PluginOrientedBoxData -> node.children
        else -> emptyList()
    }

    fun nodeText(node: PluginRayNodeData): PluginUiText? = when (node) {
        is PluginTextData -> node.text
        else -> null
    }

    fun nodeFontSize(node: PluginRayNodeData): String = when (node) {
        is PluginTextData -> node.fontSize.name
        else -> PluginFontSize.Medium.name
    }

    fun nodeColor(node: PluginRayNodeData): String = when (node) {
        is PluginTextData -> node.color.name
        is PluginIconData -> node.color?.name.orEmpty()
        else -> ""
    }

    fun nodeIcon(node: PluginRayNodeData): PluginIcon? = when (node) {
        is PluginIconData -> node.icon
        else -> null
    }

    fun nodeIconSize(node: PluginRayNodeData): String = when (node) {
        is PluginIconData -> node.size.name
        else -> PluginIconSize.Medium.name
    }

    fun nodeImage(node: PluginRayNodeData): PluginImage? = when (node) {
        is PluginImageData -> node.image
        else -> null
    }

    fun nodeImageWidth(node: PluginRayNodeData): Int? = when (node) {
        is PluginImageData -> node.width
        else -> null
    }

    fun nodeImageHeight(node: PluginRayNodeData): Int? = when (node) {
        is PluginImageData -> node.height
        else -> null
    }

    fun nodeShape(node: PluginRayNodeData): String = when (node) {
        is PluginImageData -> node.shape.name
        is PluginBoxData -> node.shape.name
        is PluginOrientedBoxData -> node.shape.name
        else -> PluginShapeToken.None.name
    }

    fun nodeAlignment(node: PluginRayNodeData): String = when (node) {
        is PluginBoxData -> node.alignment.name
        else -> ""
    }

    fun nodeOrientation(node: PluginRayNodeData): String = when (node) {
        is PluginOrientedBoxData -> node.orientation.name
        else -> ""
    }

    fun nodeSpacing(node: PluginRayNodeData): String = when (node) {
        is PluginOrientedBoxData -> node.spacing.name
        else -> PluginSpacing.Medium.name
    }

    fun detailMarkdown(node: PluginRayNodeData): String = when (node) {
        is PluginDetailData -> node.markdown
        else -> ""
    }

    fun detailNavigationTitle(node: PluginRayNodeData): PluginUiText? = when (node) {
        is PluginDetailData -> node.navigationTitle
        else -> null
    }

    fun detailMetadata(node: PluginRayNodeData): List<Any> = when (node) {
        is PluginDetailData -> node.metadata.map { it as Any }
        else -> emptyList()
    }

    fun editableTextId(node: PluginRayNodeData): String = (node as? PluginEditableTextData)?.id.orEmpty()

    fun editableTextValue(node: PluginRayNodeData): String = (node as? PluginEditableTextData)?.value.orEmpty()

    fun editableTextSelection(node: PluginRayNodeData): Int = (node as? PluginEditableTextData)?.selection ?: 0

    fun editableTextDisplayValue(node: PluginRayNodeData): String? = (node as? PluginEditableTextData)?.displayValue

    fun editableTextDisplayFormatter(node: PluginRayNodeData): String =
        (node as? PluginEditableTextData)?.displayFormatter?.name.orEmpty()

    fun editableTextPlaceholder(node: PluginRayNodeData): PluginUiText? =
        (node as? PluginEditableTextData)?.placeholder

    fun editableTextMultiline(node: PluginRayNodeData): Boolean =
        (node as? PluginEditableTextData)?.multiline ?: true

    fun editableTextMaxLines(node: PluginRayNodeData): Int =
        (node as? PluginEditableTextData)?.maxLines ?: 8

    fun editableTextAutoScrollToEnd(node: PluginRayNodeData): Boolean =
        (node as? PluginEditableTextData)?.autoScrollToEnd ?: false

    fun editableTextOnChange(node: PluginRayNodeData): PluginFormSubmitCallback? =
        (node as? PluginEditableTextData)?.onChange

    fun detailMetadataKind(item: Any): String = when (item as PluginDetailMetadataItemData) {
        is PluginDetailMetadataItemData.Label -> "label"
        is PluginDetailMetadataItemData.Link -> "link"
        is PluginDetailMetadataItemData.TagList -> "tagList"
        PluginDetailMetadataItemData.Separator -> "separator"
    }

    fun detailMetadataTitle(item: Any): PluginUiText? = when (val value = item as PluginDetailMetadataItemData) {
        is PluginDetailMetadataItemData.Label -> value.title
        is PluginDetailMetadataItemData.Link -> value.title
        is PluginDetailMetadataItemData.TagList -> value.title
        PluginDetailMetadataItemData.Separator -> null
    }

    fun detailMetadataText(item: Any): PluginUiText? = when (val value = item as PluginDetailMetadataItemData) {
        is PluginDetailMetadataItemData.Label -> value.text
        is PluginDetailMetadataItemData.Link -> value.text
        else -> null
    }

    fun detailMetadataIcon(item: Any): PluginIcon? = when (val value = item as PluginDetailMetadataItemData) {
        is PluginDetailMetadataItemData.Label -> value.icon
        else -> null
    }

    fun detailMetadataTarget(item: Any): String = when (val value = item as PluginDetailMetadataItemData) {
        is PluginDetailMetadataItemData.Link -> value.target
        else -> ""
    }

    fun detailMetadataTags(item: Any): List<Any> = when (val value = item as PluginDetailMetadataItemData) {
        is PluginDetailMetadataItemData.TagList -> value.tags.map { it as Any }
        else -> emptyList()
    }

    fun detailTagText(tag: Any): PluginUiText? = (tag as? PluginDetailMetadataItemData.TagList.Tag)?.text

    fun listSections(node: PluginRayNodeData): List<Any> = when (node) {
        is PluginListData -> node.sections.map { it as Any }
        is PluginGridData -> node.sections.map { it as Any }
        else -> emptyList()
    }

    fun listEmptyView(node: PluginRayNodeData): Any? = when (node) {
        is PluginListData -> node.emptyView
        is PluginGridData -> node.emptyView
        else -> null
    }

    fun listIsLoading(node: PluginRayNodeData): Boolean = when (node) {
        is PluginListData -> node.isLoading
        is PluginGridData -> node.isLoading
        is PluginFormData -> node.isLoading
        else -> false
    }

    fun gridColumns(node: PluginRayNodeData): Int? = when (node) {
        is PluginGridData -> node.columns
        else -> null
    }

    fun gridAspectRatio(node: PluginRayNodeData): String = when (node) {
        is PluginGridData -> node.aspectRatio.name
        else -> PluginGridAspectRatio.OneToOne.name
    }

    fun sectionTitle(section: Any): PluginUiText? = when (section) {
        is PluginListSectionData -> section.title
        is PluginGridSectionData -> section.title
        else -> null
    }

    fun sectionItems(section: Any): List<Any> = when (section) {
        is PluginListSectionData -> section.items.map { it as Any }
        is PluginGridSectionData -> section.items.map { it as Any }
        else -> emptyList()
    }

    fun listItemId(item: Any): CommandItemId? = when (item) {
        is PluginListItemData -> item.id
        is PluginGridItemData -> item.id
        else -> null
    }

    fun listItemTitle(item: Any): PluginUiText? = when (item) {
        is PluginListItemData -> item.title
        is PluginGridItemData -> item.title
        else -> null
    }

    fun listItemSubtitle(item: Any): PluginUiText? = when (item) {
        is PluginListItemData -> item.subtitle
        is PluginGridItemData -> item.subtitle
        else -> null
    }

    fun listItemIcon(item: Any): PluginIcon? = when (item) {
        is PluginListItemData -> item.icon
        is PluginGridItemData -> item.icon
        else -> null
    }

    fun listItemKeywords(item: Any): List<String> = when (item) {
        is PluginListItemData -> item.keywords
        is PluginGridItemData -> item.keywords
        else -> emptyList()
    }

    fun listItemModifier(item: Any): PluginRayModifier? = when (item) {
        is PluginListItemData -> item.itemModifier
        is PluginGridItemData -> item.itemModifier
        else -> null
    }

    fun listItemContent(item: Any): List<PluginRayNodeData> = when (item) {
        is PluginListItemData -> item.content
        else -> emptyList()
    }

    fun gridItemImage(item: Any): PluginImage? = when (item) {
        is PluginGridItemData -> item.content
        else -> null
    }

    fun emptyViewTitle(emptyView: Any): PluginUiText? = (emptyView as? PluginEmptyViewData)?.title

    fun emptyViewDescription(emptyView: Any): PluginUiText? = (emptyView as? PluginEmptyViewData)?.description

    fun emptyViewIcon(emptyView: Any): PluginIcon? = (emptyView as? PluginEmptyViewData)?.icon

    fun formFields(node: PluginRayNodeData): List<Any> = when (node) {
        is PluginFormData -> node.fields.map { it as Any }
        else -> emptyList()
    }

    fun formSubmit(node: PluginRayNodeData): Any? = when (node) {
        is PluginFormData -> node.submit
        else -> null
    }

    fun formRequireChanges(node: PluginRayNodeData): Boolean = when (node) {
        is PluginFormData -> node.requireChanges
        else -> false
    }

    fun formUnchangedView(node: PluginRayNodeData): Any? = when (node) {
        is PluginFormData -> node.unchangedView
        else -> null
    }

    fun formActionPanelHintMode(node: PluginRayNodeData): String = when (node) {
        is PluginFormData -> node.actionPanelHintMode.name
        else -> PluginActionPanelHintMode.Full.name
    }

    fun formNavigationTitle(node: PluginRayNodeData): PluginUiText? = when (node) {
        is PluginFormData -> node.navigationTitle
        else -> null
    }

    fun formSubmitTitle(submit: Any): PluginUiText? = (submit as? PluginFormSubmitData)?.title

    fun formSubmitEnabled(submit: Any): Boolean = (submit as? PluginFormSubmitData)?.enabled ?: true

    fun formSubmitStyle(submit: Any): String = (submit as? PluginFormSubmitData)?.style?.name ?: ""

    fun formSubmitIcon(submit: Any): PluginIcon? = (submit as? PluginFormSubmitData)?.icon

    fun formSubmitCallback(submit: Any): PluginFormSubmitCallback? = (submit as? PluginFormSubmitData)?.callback

    fun formFieldKind(field: Any): String = when (field as PluginFormFieldData) {
        is PluginFormFieldData.TextField -> "textField"
        is PluginFormFieldData.Checkbox -> "checkbox"
        is PluginFormFieldData.Dropdown -> "dropdown"
        is PluginFormFieldData.DatePicker -> "datePicker"
        is PluginFormFieldData.Separator -> "separator"
        is PluginFormFieldData.Description -> "description"
    }

    fun formFieldId(field: Any): String = (field as PluginFormFieldData).id

    fun formFieldTitle(field: Any): PluginUiText? = (field as PluginFormFieldData).title

    fun formFieldRequired(field: Any): Boolean = (field as PluginFormFieldData).required

    fun formFieldText(field: Any): PluginUiText? = when (field) {
        is PluginFormFieldData.Description -> field.text
        else -> null
    }

    fun formFieldPlaceholder(field: Any): PluginUiText? = (field as? PluginFormFieldData.TextField)?.placeholder

    fun formFieldDefaultText(field: Any): String = (field as? PluginFormFieldData.TextField)?.defaultValue.orEmpty()

    fun formFieldPassword(field: Any): Boolean = (field as? PluginFormFieldData.TextField)?.password ?: false

    fun formFieldMultiline(field: Any): Boolean = (field as? PluginFormFieldData.TextField)?.multiline ?: false

    fun formFieldDefaultBoolean(field: Any): Boolean = (field as? PluginFormFieldData.Checkbox)?.defaultValue ?: false

    fun formFieldOptions(field: Any): List<Any> = when (field) {
        is PluginFormFieldData.Dropdown -> field.options.map { it as Any }
        else -> emptyList()
    }

    fun formFieldDefaultOption(field: Any): String? = (field as? PluginFormFieldData.Dropdown)?.defaultValue

    fun formFieldDefaultDate(field: Any): String? = (field as? PluginFormFieldData.DatePicker)?.defaultValue

    fun formOptionValue(option: Any): String = (option as PluginFormFieldData.Dropdown.Option).value

    fun formOptionTitle(option: Any): PluginUiText? = (option as PluginFormFieldData.Dropdown.Option).title

    fun formOptionIcon(option: Any): PluginIcon? = (option as PluginFormFieldData.Dropdown.Option).icon

    fun searchResultTitleMatches(result: SearchResultSet.SearchResult): List<Int> = when (result) {
        is SearchResultSet.CachedSearchResult -> flattenMatches(result.titleMatches)
        is SearchResultSet.CommandSearchResult,
        is SearchResultSet.LiveSearchResult -> emptyList()
    }

    fun searchResultDescriptionMatches(result: SearchResultSet.SearchResult): List<Int> = when (result) {
        is SearchResultSet.CachedSearchResult -> flattenMatches(result.descriptionMatches)
        is SearchResultSet.CommandSearchResult,
        is SearchResultSet.LiveSearchResult -> emptyList()
    }

    fun searchResultIsLive(result: SearchResultSet.SearchResult): Boolean =
        result is SearchResultSet.LiveSearchResult

    fun searchResultContent(result: SearchResultSet.SearchResult): List<PluginRayNodeData> = when (result) {
        is SearchResultSet.LiveSearchResult -> result.presentation.content
        is SearchResultSet.CachedSearchResult,
        is SearchResultSet.CommandSearchResult -> emptyList()
    }

    fun suppressesHostActions(nodes: List<PluginRayNodeData>): Boolean = nodes.suppressesHostActions()

    fun actionPanelHintMode(nodes: List<PluginRayNodeData>): String = nodes.actionPanelHintMode().name

    fun focusedActions(
        nodes: List<PluginRayNodeData>,
        focusedItemValue: String?,
        query: String
    ): List<PluginCommandListAction> = nodes.pluginFocusModel(
        focusedItemId = focusedItemValue?.let(::CommandItemId),
        query = query
    ).focusedActions

    private fun flattenMatches(ranges: List<IntRange>): List<Int> = buildList(ranges.size * 2) {
        ranges.forEach { range ->
            add(range.first)
            add(range.last)
        }
    }
}
