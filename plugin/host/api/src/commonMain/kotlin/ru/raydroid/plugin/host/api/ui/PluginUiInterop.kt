package ru.raydroid.plugin.host.api.ui

object PluginUiBridge {
    fun nodeKind(node: PluginRayNodeData): String = when (node) {
        is PluginTextData -> "text"
        is PluginIconData -> "icon"
        is PluginImageData -> "image"
        is PluginBoxData -> "box"
        is PluginOrientedBoxData -> "orientedBox"
        is PluginDetailData -> "detail"
        is PluginFormData -> "form"
        is PluginListData -> "list"
        is PluginGridData -> "grid"
    }

    fun detailMetadataKind(item: Any): String = when (item as PluginDetailMetadataItemData) {
        is PluginDetailMetadataItemData.Label -> "label"
        is PluginDetailMetadataItemData.Link -> "link"
        is PluginDetailMetadataItemData.TagList -> "tagList"
        PluginDetailMetadataItemData.Separator -> "separator"
    }

    fun formFieldKind(field: Any): String = when (field as PluginFormFieldData) {
        is PluginFormFieldData.TextField -> "textField"
        is PluginFormFieldData.Checkbox -> "checkbox"
        is PluginFormFieldData.Dropdown -> "dropdown"
        is PluginFormFieldData.DatePicker -> "datePicker"
        is PluginFormFieldData.Separator -> "separator"
        is PluginFormFieldData.Description -> "description"
    }
}
