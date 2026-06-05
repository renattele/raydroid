package ru.raydroid.plugin.host.api.ui

sealed class PluginRayNodeData {
    var modifier: PluginRayModifier? = null
}

data class PluginRayModifier(
    val enabled: Boolean = true,
    val click: PluginCommandCallback? = null,
    val actions: List<PluginCommandListAction> = emptyList(),
    val weight: Float? = null,
    val fillMaxSize: Boolean = false,
    val padding: PluginSpacing? = null,
)

data class PluginTextData(
    val text: PluginUiText,
    val fontSize: PluginFontSize = PluginFontSize.Medium,
    val fontWeight: PluginFontWeight = PluginFontWeight.Normal,
    val color: PluginColor = PluginColor.OnSurface,
) : PluginRayNodeData()

data class PluginIconData(
    val icon: PluginIcon,
    val contentDescription: String? = null,
    val size: PluginIconSize = PluginIconSize.Medium,
    val color: PluginColor? = null,
) : PluginRayNodeData()

data class PluginImageData(
    val image: PluginImage,
    val contentDescription: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val shape: PluginShapeToken = PluginShapeToken.None,
) : PluginRayNodeData()

data class PluginBoxData(
    val alignment: PluginBoxAlignment,
    val shape: PluginShapeToken = PluginShapeToken.None,
    val children: List<PluginRayNodeData>,
) : PluginRayNodeData()

data class PluginOrientedBoxData(
    val orientation: PluginOrientation,
    val alignment: PluginAlignment,
    val arrangement: PluginArrangement,
    val spacing: PluginSpacing,
    val shape: PluginShapeToken = PluginShapeToken.None,
    val children: List<PluginRayNodeData>,
) : PluginRayNodeData()

data class PluginDetailData(
    val markdown: String,
    val metadata: List<PluginDetailMetadataItemData> = emptyList(),
    val isLoading: Boolean = false,
    val navigationTitle: PluginUiText? = null,
    val autoScrollToEnd: Boolean = false,
    val showScrollHandle: Boolean = false,
) : PluginRayNodeData()

data class PluginEditableTextData(
    val id: String,
    val value: String,
    val selection: Int = value.length,
    val displayValue: String? = null,
    val displayFormatter: PluginEditableTextDisplayFormatter = PluginEditableTextDisplayFormatter.None,
    val placeholder: PluginUiText? = null,
    val multiline: Boolean = true,
    val maxLines: Int = 8,
    val autoScrollToEnd: Boolean = false,
    val onChange: PluginFormSubmitCallback,
) : PluginRayNodeData()

enum class PluginEditableTextDisplayFormatter {
    None,
    CalculatorExpression,
}

sealed interface PluginDetailMetadataItemData {
    data class Label(
        val title: PluginUiText,
        val text: PluginUiText? = null,
        val icon: PluginIcon? = null,
    ) : PluginDetailMetadataItemData

    data class Link(
        val title: PluginUiText,
        val text: PluginUiText,
        val target: String,
    ) : PluginDetailMetadataItemData

    data class TagList(
        val title: PluginUiText,
        val tags: List<Tag>,
    ) : PluginDetailMetadataItemData {
        data class Tag(
            val text: PluginUiText? = null,
            val icon: PluginIcon? = null,
        )
    }

    data object Separator : PluginDetailMetadataItemData
}

data class PluginFormData(
    val isLoading: Boolean = false,
    val navigationTitle: PluginUiText? = null,
    val fields: List<PluginFormFieldData>,
    val submit: PluginFormSubmitData? = null,
    val suppressHostActions: Boolean = false,
    val requireChanges: Boolean = false,
    val unchangedView: PluginEmptyViewData? = null,
    val actionPanelHintMode: PluginActionPanelHintMode = PluginActionPanelHintMode.Full,
) : PluginRayNodeData()

enum class PluginActionPanelHintMode {
    Full,
    MenuOnly,
    Hidden,
}

data class PluginFormSubmitData(
    val title: PluginUiText,
    val callback: PluginFormSubmitCallback,
    val icon: PluginIcon? = null,
    val style: PluginFormSubmitStyle = PluginFormSubmitStyle.Filled,
    val enabled: Boolean = true,
)

enum class PluginFormSubmitStyle {
    Filled,
    Tonal,
}

sealed class PluginFormValue {
    data class Text(
        val value: String,
    ) : PluginFormValue()

    data class BooleanValue(
        val value: Boolean,
    ) : PluginFormValue()

    data class DateValue(
        val value: String?,
    ) : PluginFormValue()
}

typealias PluginFormValues = Map<String, PluginFormValue>

class PluginFormSubmitCallback(
    private val dispatch: suspend (PluginFormValues) -> Unit,
) {
    suspend operator fun invoke(values: PluginFormValues) {
        dispatch(values)
    }
}

sealed interface PluginFormFieldData {
    val id: String
    val title: PluginUiText?
    val required: Boolean

    data class TextField(
        override val id: String,
        override val title: PluginUiText?,
        override val required: Boolean = false,
        val placeholder: PluginUiText? = null,
        val defaultValue: String = "",
        val password: Boolean = false,
        val multiline: Boolean = false,
    ) : PluginFormFieldData

    data class Checkbox(
        override val id: String,
        override val title: PluginUiText?,
        override val required: Boolean = false,
        val defaultValue: Boolean = false,
    ) : PluginFormFieldData

    data class Dropdown(
        override val id: String,
        override val title: PluginUiText?,
        override val required: Boolean = false,
        val options: List<Option>,
        val defaultValue: String? = null,
    ) : PluginFormFieldData {
        data class Option(
            val value: String,
            val title: PluginUiText,
            val icon: PluginIcon? = null,
        )
    }

    data class DatePicker(
        override val id: String,
        override val title: PluginUiText?,
        override val required: Boolean = false,
        val defaultValue: String? = null,
    ) : PluginFormFieldData

    data class Separator(
        override val id: String,
    ) : PluginFormFieldData {
        override val title: PluginUiText? = null
        override val required: Boolean = false
    }

    data class Description(
        override val id: String,
        val text: PluginUiText,
    ) : PluginFormFieldData {
        override val title: PluginUiText? = null
        override val required: Boolean = false
    }
}

data class PluginListData(
    val sections: List<PluginListSectionData>,
    val emptyView: PluginEmptyViewData? = null,
    val isLoading: Boolean = false,
    val filtering: Boolean = true,
    val searchBarPlaceholder: PluginUiText? = null,
    val lazy: Boolean = false,
) : PluginRayNodeData()

data class PluginListSectionData(
    val title: PluginUiText? = null,
    val items: List<PluginListItemData>,
)

data class PluginListItemData(
    val id: ru.raydroid.plugin.api.presentation.CommandItemId,
    val title: PluginUiText,
    val subtitle: PluginUiText? = null,
    val icon: PluginIcon? = null,
    val iconColor: PluginColor? = null,
    val keywords: List<String> = emptyList(),
    val detail: PluginDetailData? = null,
    val content: List<PluginRayNodeData> = emptyList(),
    val itemModifier: PluginRayModifier? = null,
)

data class PluginGridData(
    val sections: List<PluginGridSectionData>,
    val emptyView: PluginEmptyViewData? = null,
    val isLoading: Boolean = false,
    val filtering: Boolean = true,
    val searchBarPlaceholder: PluginUiText? = null,
    val columns: Int? = null,
    val aspectRatio: PluginGridAspectRatio = PluginGridAspectRatio.OneToOne,
    val lazy: Boolean = false,
) : PluginRayNodeData()

data class PluginGridSectionData(
    val title: PluginUiText? = null,
    val items: List<PluginGridItemData>,
)

data class PluginGridItemData(
    val id: ru.raydroid.plugin.api.presentation.CommandItemId,
    val title: PluginUiText,
    val subtitle: PluginUiText? = null,
    val content: PluginImage? = null,
    val icon: PluginIcon? = null,
    val keywords: List<String> = emptyList(),
    val itemModifier: PluginRayModifier? = null,
)

enum class PluginGridAspectRatio {
    OneToOne,
    ThreeToTwo,
    TwoToThree,
    FourToThree,
    ThreeToFour,
    SixteenToNine,
    NineToSixteen,
}

data class PluginEmptyViewData(
    val title: PluginUiText,
    val description: PluginUiText? = null,
    val icon: PluginIcon? = null,
    val iconColor: PluginColor? = null,
)
