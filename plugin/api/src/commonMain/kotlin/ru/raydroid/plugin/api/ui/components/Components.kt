package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandItemId

@Serializable
data class DetailData(
    val markdown: String,
    val metadata: List<DetailMetadataItemData> = emptyList(),
    val isLoading: Boolean = false,
    val navigationTitle: UiText? = null
) : RayNodeData()

@Serializable
sealed interface DetailMetadataItemData {
    @Serializable
    data class Label(
        val title: UiText,
        val text: UiText? = null,
        val icon: Icon? = null
    ) : DetailMetadataItemData

    @Serializable
    data class Link(
        val title: UiText,
        val text: UiText,
        val target: String
    ) : DetailMetadataItemData

    @Serializable
    data class TagList(
        val title: UiText,
        val tags: List<Tag>
    ) : DetailMetadataItemData {
        @Serializable
        data class Tag(
            val text: UiText? = null,
            val icon: Icon? = null
        )
    }

    @Serializable
    data object Separator : DetailMetadataItemData
}

@Ray
class DetailMetadataScope internal constructor() {
    internal val items = mutableListOf<DetailMetadataItemData>()

    fun label(title: UiText, text: UiText? = null, icon: Icon? = null) {
        items += DetailMetadataItemData.Label(title, text, icon)
    }

    fun link(title: UiText, text: UiText, target: String) {
        items += DetailMetadataItemData.Link(title, text, target)
    }

    fun tagList(title: UiText, content: DetailTagListScope.() -> Unit) {
        val scope = DetailTagListScope()
        scope.content()
        items += DetailMetadataItemData.TagList(title, scope.tags)
    }

    fun separator() {
        items += DetailMetadataItemData.Separator
    }
}

@Ray
class DetailTagListScope internal constructor() {
    internal val tags = mutableListOf<DetailMetadataItemData.TagList.Tag>()

    fun item(text: UiText? = null, icon: Icon? = null) {
        tags += DetailMetadataItemData.TagList.Tag(text, icon)
    }
}

@Ray
fun RayScope.Detail(
    markdown: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    navigationTitle: UiText? = null,
    metadata: DetailMetadataScope.() -> Unit = {}
) {
    val metadataScope = DetailMetadataScope()
    metadataScope.metadata()
    add(
        DetailData(
            markdown = markdown,
            metadata = metadataScope.items,
            isLoading = isLoading,
            navigationTitle = navigationTitle
        ).withModifier(modifier(modifier))
    )
}

@Serializable
data class FormData(
    val isLoading: Boolean = false,
    val navigationTitle: UiText? = null,
    val fields: List<FormFieldData>,
    val submit: FormSubmitData? = null,
    val suppressHostActions: Boolean = false,
    val requireChanges: Boolean = false,
    val unchangedView: EmptyViewData? = null,
    val actionPanelHintMode: ActionPanelHintMode = ActionPanelHintMode.Full
) : RayNodeData()

@Serializable
enum class ActionPanelHintMode {
    Full,
    MenuOnly,
    Hidden
}

@Serializable
data class FormSubmitData(
    val title: UiText,
    val callback: CommandCallbackRef,
    val icon: Icon? = null,
    val style: FormSubmitStyle = FormSubmitStyle.Filled,
    val enabled: Boolean = true
)

@Serializable
enum class FormSubmitStyle {
    Filled,
    Tonal
}

@Serializable
sealed class FormValue {
    @Serializable
    data class Text(val value: String) : FormValue()

    @Serializable
    data class BooleanValue(val value: Boolean) : FormValue()

    @Serializable
    data class DateValue(val value: String?) : FormValue()
}

typealias FormValues = Map<String, FormValue>

@Serializable
sealed interface FormFieldData {
    val id: String
    val title: UiText?
    val required: Boolean

    @Serializable
    data class TextField(
        override val id: String,
        override val title: UiText?,
        override val required: Boolean = false,
        val placeholder: UiText? = null,
        val defaultValue: String = "",
        val password: Boolean = false,
        val multiline: Boolean = false
    ) : FormFieldData

    @Serializable
    data class Checkbox(
        override val id: String,
        override val title: UiText?,
        override val required: Boolean = false,
        val defaultValue: Boolean = false
    ) : FormFieldData

    @Serializable
    data class Dropdown(
        override val id: String,
        override val title: UiText?,
        override val required: Boolean = false,
        val options: List<Option>,
        val defaultValue: String? = null
    ) : FormFieldData {
        @Serializable
        data class Option(
            val value: String,
            val title: UiText,
            val icon: Icon? = null
        )
    }

    @Serializable
    data class DatePicker(
        override val id: String,
        override val title: UiText?,
        override val required: Boolean = false,
        val defaultValue: String? = null
    ) : FormFieldData

    @Serializable
    data class Separator(
        override val id: String
    ) : FormFieldData {
        override val title: UiText? = null
        override val required: Boolean = false
    }

    @Serializable
    data class Description(
        override val id: String,
        val text: UiText
    ) : FormFieldData {
        override val title: UiText? = null
        override val required: Boolean = false
    }
}

@Ray
class FormScope internal constructor(
    private val registerFormCallback: (String, suspend (FormValues) -> Unit) -> CommandCallbackRef
) {
    internal val fields = mutableListOf<FormFieldData>()
    internal var submit: FormSubmitData? = null
    private var index = 0

    fun textField(
        id: String,
        title: UiText? = null,
        placeholder: UiText? = null,
        defaultValue: String = "",
        required: Boolean = false
    ) {
        fields += FormFieldData.TextField(id, title, required, placeholder, defaultValue)
    }

    fun passwordField(
        id: String,
        title: UiText? = null,
        placeholder: UiText? = null,
        defaultValue: String = "",
        required: Boolean = false
    ) {
        fields += FormFieldData.TextField(id, title, required, placeholder, defaultValue, password = true)
    }

    fun textArea(
        id: String,
        title: UiText? = null,
        placeholder: UiText? = null,
        defaultValue: String = "",
        required: Boolean = false
    ) {
        fields += FormFieldData.TextField(id, title, required, placeholder, defaultValue, multiline = true)
    }

    fun checkbox(
        id: String,
        title: UiText? = null,
        defaultValue: Boolean = false,
        required: Boolean = false
    ) {
        fields += FormFieldData.Checkbox(id, title, required, defaultValue)
    }

    fun dropdown(
        id: String,
        title: UiText? = null,
        options: List<FormFieldData.Dropdown.Option>,
        defaultValue: String? = null,
        required: Boolean = false
    ) {
        fields += FormFieldData.Dropdown(id, title, required, options, defaultValue)
    }

    fun datePicker(
        id: String,
        title: UiText? = null,
        defaultValue: String? = null,
        required: Boolean = false
    ) {
        fields += FormFieldData.DatePicker(id, title, required, defaultValue)
    }

    fun separator(id: String = "separator-${index++}") {
        fields += FormFieldData.Separator(id)
    }

    fun description(id: String = "description-${index++}", text: UiText) {
        fields += FormFieldData.Description(id, text)
    }

    fun submit(
        title: UiText,
        icon: Icon? = null,
        style: FormSubmitStyle = FormSubmitStyle.Filled,
        enabled: Boolean = true,
        onSubmit: suspend (FormValues) -> Unit
    ) {
        submit = FormSubmitData(
            title = title,
            callback = registerFormCallback("form-submit-${index++}", onSubmit),
            icon = icon,
            style = style,
            enabled = enabled
        )
    }
}

@Ray
fun RayScope.Form(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    navigationTitle: UiText? = null,
    suppressHostActions: Boolean = false,
    requireChanges: Boolean = false,
    unchangedView: EmptyViewData? = null,
    actionPanelHintMode: ActionPanelHintMode = ActionPanelHintMode.Full,
    content: FormScope.() -> Unit
) {
    val formScope = FormScope(::registerFormCallback)
    formScope.content()
    add(
        FormData(
            isLoading = isLoading,
            navigationTitle = navigationTitle,
            fields = formScope.fields,
            submit = formScope.submit,
            suppressHostActions = suppressHostActions,
            requireChanges = requireChanges,
            unchangedView = unchangedView,
            actionPanelHintMode = actionPanelHintMode
        ).withModifier(modifier(modifier))
    )
}

@Serializable
data class ListData(
    val sections: List<ListSectionData>,
    val emptyView: EmptyViewData? = null,
    val isLoading: Boolean = false,
    val filtering: Boolean = true,
    val searchBarPlaceholder: UiText? = null,
    val lazy: Boolean = false
) : RayNodeData()

@Serializable
data class ListSectionData(
    val title: UiText? = null,
    val items: List<ListItemData>
)

@Serializable
data class ListItemData(
    val id: CommandItemId,
    val title: UiText,
    val subtitle: UiText? = null,
    val icon: Icon? = null,
    val iconColor: Color? = null,
    val keywords: List<String> = emptyList(),
    val detail: DetailData? = null,
    val content: List<RayNodeData> = emptyList(),
    val modifier: RayModifier? = null
)

@Serializable
data class GridData(
    val sections: List<GridSectionData>,
    val emptyView: EmptyViewData? = null,
    val isLoading: Boolean = false,
    val filtering: Boolean = true,
    val searchBarPlaceholder: UiText? = null,
    val columns: Int? = null,
    val aspectRatio: GridAspectRatio = GridAspectRatio.OneToOne,
    val lazy: Boolean = false
) : RayNodeData()

@Serializable
data class GridSectionData(
    val title: UiText? = null,
    val items: List<GridItemData>
)

@Serializable
data class GridItemData(
    val id: CommandItemId,
    val title: UiText,
    val subtitle: UiText? = null,
    val content: Image? = null,
    val icon: Icon? = null,
    val keywords: List<String> = emptyList(),
    val modifier: RayModifier? = null
)

@Serializable
enum class GridAspectRatio {
    OneToOne,
    ThreeToTwo,
    TwoToThree,
    FourToThree,
    ThreeToFour,
    SixteenToNine,
    NineToSixteen
}

@Serializable
data class EmptyViewData(
    val title: UiText,
    val description: UiText? = null,
    val icon: Icon? = null,
    val iconColor: Color? = null
)

@Ray
class ListScope internal constructor(private val owner: RayScope) {
    internal val sections = mutableListOf<ListSectionData>()
    internal var emptyView: EmptyViewData? = null
    private val defaultItems = mutableListOf<ListItemData>()

    fun section(title: UiText? = null, content: ListSectionScope.() -> Unit) {
        val scope = ListSectionScope(owner)
        scope.content()
        sections += ListSectionData(title, scope.items)
    }

    fun item(
        id: CommandItemId,
        title: UiText,
        subtitle: UiText? = null,
        icon: Icon? = null,
        iconColor: Color? = null,
        keywords: List<String> = emptyList(),
        modifier: Modifier = Modifier,
        detail: (DetailMetadataScope.() -> Unit)? = null,
        detailMarkdown: String? = null,
        content: (RayScope.() -> Unit)? = null
    ) {
        defaultItems += listItem(
            owner = owner,
            id = id,
            title = title,
            subtitle = subtitle,
            icon = icon,
            iconColor = iconColor,
            keywords = keywords,
            modifier = modifier,
            detailMarkdown = detailMarkdown,
            detail = detail,
            content = content
        )
    }

    fun emptyView(title: UiText, description: UiText? = null, icon: Icon? = null, iconColor: Color? = null) {
        emptyView = EmptyViewData(title, description, icon, iconColor)
    }

    internal fun allSections(): List<ListSectionData> =
        if (defaultItems.isEmpty()) sections else listOf(ListSectionData(items = defaultItems)) + sections
}

@Ray
class ListSectionScope internal constructor(private val owner: RayScope) {
    internal val items = mutableListOf<ListItemData>()

    fun item(
        id: CommandItemId,
        title: UiText,
        subtitle: UiText? = null,
        icon: Icon? = null,
        iconColor: Color? = null,
        keywords: List<String> = emptyList(),
        modifier: Modifier = Modifier,
        detail: (DetailMetadataScope.() -> Unit)? = null,
        detailMarkdown: String? = null,
        content: (RayScope.() -> Unit)? = null
    ) {
        items += listItem(
            owner = owner,
            id = id,
            title = title,
            subtitle = subtitle,
            icon = icon,
            iconColor = iconColor,
            keywords = keywords,
            modifier = modifier,
            detailMarkdown = detailMarkdown,
            detail = detail,
            content = content
        )
    }
}

private fun listItem(
    owner: RayScope,
    id: CommandItemId,
    title: UiText,
    subtitle: UiText?,
    icon: Icon?,
    iconColor: Color?,
    keywords: List<String>,
    modifier: Modifier,
    detailMarkdown: String?,
    detail: (DetailMetadataScope.() -> Unit)?,
    content: (RayScope.() -> Unit)?
): ListItemData {
    val metadataScope = DetailMetadataScope()
    if (detail != null) {
        metadataScope.detail()
    }
    return ListItemData(
        id = id,
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconColor = iconColor,
        keywords = keywords,
        detail = detailMarkdown?.let { DetailData(markdown = it, metadata = metadataScope.items) },
        content = content?.let(owner::fork).orEmpty(),
        modifier = owner.modifier(modifier)
    )
}

@Ray
fun RayScope.List(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    filtering: Boolean = true,
    searchBarPlaceholder: UiText? = null,
    content: ListScope.() -> Unit
) {
    val scope = ListScope(this)
    scope.content()
    add(
        ListData(
            sections = scope.allSections(),
            emptyView = scope.emptyView,
            isLoading = isLoading,
            filtering = filtering,
            searchBarPlaceholder = searchBarPlaceholder
        ).withModifier(modifier(modifier))
    )
}

@Ray
fun RayScope.LazyList(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    filtering: Boolean = true,
    searchBarPlaceholder: UiText? = null,
    content: ListScope.() -> Unit
) {
    val scope = ListScope(this)
    scope.content()
    add(
        ListData(
            sections = scope.allSections(),
            emptyView = scope.emptyView,
            isLoading = isLoading,
            filtering = filtering,
            searchBarPlaceholder = searchBarPlaceholder,
            lazy = true
        ).withModifier(modifier(modifier))
    )
}

@Ray
class GridScope internal constructor(private val owner: RayScope) {
    internal val sections = mutableListOf<GridSectionData>()
    internal var emptyView: EmptyViewData? = null
    private val defaultItems = mutableListOf<GridItemData>()

    fun section(title: UiText? = null, content: GridSectionScope.() -> Unit) {
        val scope = GridSectionScope(owner)
        scope.content()
        sections += GridSectionData(title, scope.items)
    }

    fun item(
        id: CommandItemId,
        title: UiText,
        subtitle: UiText? = null,
        content: Image? = null,
        icon: Icon? = null,
        keywords: List<String> = emptyList(),
        modifier: Modifier = Modifier
    ) {
        defaultItems += gridItem(owner, id, title, subtitle, content, icon, keywords, modifier)
    }

    fun emptyView(title: UiText, description: UiText? = null, icon: Icon? = null) {
        emptyView = EmptyViewData(title, description, icon)
    }

    internal fun allSections(): List<GridSectionData> =
        if (defaultItems.isEmpty()) sections else listOf(GridSectionData(items = defaultItems)) + sections
}

@Ray
class GridSectionScope internal constructor(private val owner: RayScope) {
    internal val items = mutableListOf<GridItemData>()

    fun item(
        id: CommandItemId,
        title: UiText,
        subtitle: UiText? = null,
        content: Image? = null,
        icon: Icon? = null,
        keywords: List<String> = emptyList(),
        modifier: Modifier = Modifier
    ) {
        items += gridItem(owner, id, title, subtitle, content, icon, keywords, modifier)
    }
}

private fun gridItem(
    owner: RayScope,
    id: CommandItemId,
    title: UiText,
    subtitle: UiText?,
    content: Image?,
    icon: Icon?,
    keywords: List<String>,
    modifier: Modifier
) = GridItemData(
    id = id,
    title = title,
    subtitle = subtitle,
    content = content,
    icon = icon,
    keywords = keywords,
    modifier = owner.modifier(modifier)
)

@Ray
fun RayScope.Grid(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    filtering: Boolean = true,
    searchBarPlaceholder: UiText? = null,
    columns: Int? = null,
    aspectRatio: GridAspectRatio = GridAspectRatio.OneToOne,
    content: GridScope.() -> Unit
) {
    val scope = GridScope(this)
    scope.content()
    add(
        GridData(
            sections = scope.allSections(),
            emptyView = scope.emptyView,
            isLoading = isLoading,
            filtering = filtering,
            searchBarPlaceholder = searchBarPlaceholder,
            columns = columns,
            aspectRatio = aspectRatio
        ).withModifier(modifier(modifier))
    )
}

@Ray
fun RayScope.LazyGrid(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    filtering: Boolean = true,
    searchBarPlaceholder: UiText? = null,
    columns: Int? = null,
    aspectRatio: GridAspectRatio = GridAspectRatio.OneToOne,
    content: GridScope.() -> Unit
) {
    val scope = GridScope(this)
    scope.content()
    add(
        GridData(
            sections = scope.allSections(),
            emptyView = scope.emptyView,
            isLoading = isLoading,
            filtering = filtering,
            searchBarPlaceholder = searchBarPlaceholder,
            columns = columns,
            aspectRatio = aspectRatio,
            lazy = true
        ).withModifier(modifier(modifier))
    )
}
