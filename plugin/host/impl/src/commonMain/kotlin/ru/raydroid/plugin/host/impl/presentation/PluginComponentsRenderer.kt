package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RButton
import ru.raydroid.core.designsystem.component.RDivider
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.core.designsystem.component.RTextButton
import ru.raydroid.core.designsystem.component.RTextField
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginDetailData
import ru.raydroid.plugin.host.api.ui.PluginDetailMetadataItemData
import ru.raydroid.plugin.host.api.ui.PluginEmptyViewData
import ru.raydroid.plugin.host.api.ui.PluginFormData
import ru.raydroid.plugin.host.api.ui.PluginFormFieldData
import ru.raydroid.plugin.host.api.ui.PluginFormValue
import ru.raydroid.plugin.host.api.ui.PluginGridAspectRatio
import ru.raydroid.plugin.host.api.ui.PluginGridData
import ru.raydroid.plugin.host.api.ui.PluginGridItemData
import ru.raydroid.plugin.host.api.ui.PluginGridSectionData
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginImageData
import ru.raydroid.plugin.host.api.ui.PluginListData
import ru.raydroid.plugin.host.api.ui.PluginListItemData
import ru.raydroid.plugin.host.api.ui.PluginListSectionData
import ru.raydroid.plugin.host.api.ui.PluginRayModifier
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText

@Composable
internal fun DetailRenderer(data: PluginDetailData, modifier: Modifier = Modifier) {
    val spacing = RaydroidTheme.spacing
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        if (data.isLoading) {
            CircularProgressIndicator()
        }
        data.navigationTitle?.let { title -> RText(title.asText(), fontSize = pluginFontSizeLarge()) }
        RText(data.markdown)
        if (data.metadata.isNotEmpty()) {
            RDivider()
            MetadataRenderer(data.metadata)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataRenderer(items: List<PluginDetailMetadataItemData>) {
    val spacing = RaydroidTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        items.forEach { item ->
            when (item) {
                is PluginDetailMetadataItemData.Label -> MetadataRow(item.title.asText(), item.text?.asText(), item.icon)
                is PluginDetailMetadataItemData.Link -> MetadataRow(item.title.asText(), item.text.asText(), null)
                is PluginDetailMetadataItemData.TagList -> Column {
                    RText(item.title.asText(), fontSize = pluginFontSizeSmall())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                        item.tags.forEach { tag ->
                            RText(
                                tag.text?.asText().orEmpty(),
                                modifier = Modifier
                                    .clip(RaydroidTheme.shapes.shape(RaydroidShapeToken.Small))
                                    .background(RaydroidTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = spacing.small, vertical = spacing.extraSmall),
                                fontSize = pluginFontSizeSmall()
                            )
                        }
                    }
                }
                PluginDetailMetadataItemData.Separator -> RDivider()
            }
        }
    }
}

@Composable
private fun MetadataRow(title: String, value: String?, icon: ru.raydroid.plugin.host.api.ui.PluginIcon?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconRenderer(PluginIconData(icon = icon, size = PluginIconSize.Small))
        }
        RText(title, fontSize = pluginFontSizeSmall())
        if (value != null) {
            RText(value)
        }
    }
}

@Composable
internal fun FormRenderer(data: PluginFormData, modifier: Modifier = Modifier) {
    val spacing = RaydroidTheme.spacing
    val coroutineScope = rememberCoroutineScope()
    val values = remember(data.fields) {
        mutableStateMapOf<String, PluginFormValue>().apply {
            data.fields.forEach { field ->
                when (field) {
                    is PluginFormFieldData.TextField -> put(field.id, PluginFormValue.Text(field.defaultValue))
                    is PluginFormFieldData.Checkbox -> put(field.id, PluginFormValue.BooleanValue(field.defaultValue))
                    is PluginFormFieldData.Dropdown -> put(
                        field.id,
                        PluginFormValue.Text(field.defaultValue ?: field.options.firstOrNull()?.value.orEmpty())
                    )
                    is PluginFormFieldData.DatePicker -> put(field.id, PluginFormValue.DateValue(field.defaultValue))
                    is PluginFormFieldData.Separator,
                    is PluginFormFieldData.Description -> Unit
                }
            }
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        if (data.isLoading) {
            CircularProgressIndicator()
        }
        data.navigationTitle?.let { title -> RText(title.asText(), fontSize = pluginFontSizeLarge()) }
        data.fields.forEach { field ->
            FormFieldRenderer(field, values)
        }
        val submit = data.submit
        if (submit != null) {
            RButton(
                enabled = submit.enabled,
                onClick = {
                    coroutineScope.launch {
                        submit.callback(values.toMap())
                    }
                }
            ) {
                RText(submit.title.asText())
            }
        }
    }
}

@Composable
private fun FormFieldRenderer(
    field: PluginFormFieldData,
    values: MutableMap<String, PluginFormValue>
) {
    val spacing = RaydroidTheme.spacing
    when (field) {
        is PluginFormFieldData.TextField -> {
            val state = remember(field.id) { TextFieldState(field.defaultValue) }
            LaunchedEffect(state.text) {
                values[field.id] = PluginFormValue.Text(state.text.toString())
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                field.title?.let { title -> RText(title.asText(), fontSize = pluginFontSizeSmall()) }
                RTextField(
                    state = state,
                    keyboardOptions = KeyboardOptions(keyboardType = if (field.password) KeyboardType.Password else KeyboardType.Text),
                    lineLimits = if (field.multiline) TextFieldLineLimits.MultiLine(2, 6) else TextFieldLineLimits.SingleLine,
                    placeholder = field.placeholder?.let { placeholder -> { RText(placeholder.asText()) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RaydroidTheme.shapes.small)
                        .background(RaydroidTheme.colorScheme.surfaceContainer)
                )
            }
        }
        is PluginFormFieldData.Checkbox -> Row(verticalAlignment = Alignment.CenterVertically) {
            var checked by remember(field.id) { mutableStateOf(field.defaultValue) }
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    values[field.id] = PluginFormValue.BooleanValue(it)
                }
            )
            field.title?.let { title -> RText(title.asText()) }
        }
        is PluginFormFieldData.Dropdown -> {
            var selected by remember(field.id) {
                mutableStateOf(field.defaultValue ?: field.options.firstOrNull()?.value.orEmpty())
            }
            val selectedOption = field.options.firstOrNull { option -> option.value == selected }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                field.title?.let { title -> RText(title.asText(), fontSize = pluginFontSizeSmall()) }
                SelectMenu(
                    text = selectedOption?.title?.asText() ?: "Select",
                    enabled = field.options.isNotEmpty()
                ) {
                    field.options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    option.icon?.let { icon -> IconRenderer(PluginIconData(icon, size = PluginIconSize.Small)) }
                                    RText(option.title.asText())
                                }
                            },
                            onClick = {
                                selected = option.value
                                values[field.id] = PluginFormValue.Text(option.value)
                            }
                        )
                    }
                }
            }
        }
        is PluginFormFieldData.DatePicker -> DatePickerField(field, values)
        is PluginFormFieldData.Separator -> RDivider()
        is PluginFormFieldData.Description -> RText(field.text.asText(), fontSize = pluginFontSizeSmall())
    }
}

@Composable
private fun SelectMenu(
    text: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        RTextButton(
            enabled = enabled,
            onClick = { expanded = true },
            modifier = Modifier
                .clip(RaydroidTheme.shapes.small)
                .background(RaydroidTheme.colorScheme.surfaceContainer)
        ) {
            RText(text)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    field: PluginFormFieldData.DatePicker,
    values: MutableMap<String, PluginFormValue>
) {
    val spacing = RaydroidTheme.spacing
    var showDialog by remember(field.id) { mutableStateOf(false) }
    var selectedDate by remember(field.id) { mutableStateOf(field.defaultValue) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = field.defaultValue.parseIsoDate()?.toEpochMillis()
    )
    LaunchedEffect(selectedDate) {
        values[field.id] = PluginFormValue.DateValue(selectedDate)
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        field.title?.let { title -> RText(title.asText(), fontSize = pluginFontSizeSmall()) }
        RTextButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .clip(RaydroidTheme.shapes.small)
                .background(RaydroidTheme.colorScheme.surfaceContainer)
        ) {
            RText(selectedDate ?: "Select date")
        }
    }
    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                RTextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis?.toIsoDate()
                        showDialog = false
                    }
                ) {
                    RText("OK")
                }
            },
            dismissButton = {
                RTextButton(onClick = { showDialog = false }) {
                    RText("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
internal fun ListRenderer(
    data: PluginListData,
    query: String,
    focusedItemId: CommandItemId?,
    modifier: Modifier = Modifier,
    onClick: (PluginCommandCallback) -> Unit,
    onItemEnter: (CommandItemId) -> Unit,
    onFocus: (CommandItemId) -> Unit,
    onActions: (List<PluginCommandListAction>) -> Unit
) {
    val sections = remember(data, query) { data.filtered(query) }
    Box(modifier.fillMaxWidth()) {
        if (data.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        if (sections.all { it.items.isEmpty() } && !data.isLoading) {
            EmptyViewRenderer(data.emptyView)
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(focusedItemId, sections) {
                val index = sections.indexOfListItem(focusedItemId)
                if (index != null) {
                    listState.animateScrollToItem(index)
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.extraSmall),
                state = listState
            ) {
                sections.forEach { section ->
                    section.title?.let { title ->
                        item { RText(title.asText(), modifier = Modifier.padding(RaydroidTheme.spacing.small)) }
                    }
                    items(section.items, key = { it.id.value }) { item ->
                        ComponentListItem(
                            item = item,
                            focused = item.id == focusedItemId,
                            onClick = onClick,
                            onItemEnter = onItemEnter,
                            onFocus = onFocus,
                            onActions = onActions
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentListItem(
    item: PluginListItemData,
    focused: Boolean,
    onClick: (PluginCommandCallback) -> Unit,
    onItemEnter: (CommandItemId) -> Unit,
    onFocus: (CommandItemId) -> Unit,
    onActions: (List<PluginCommandListAction>) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .componentInteractive(item.id, item.itemModifier, onClick, onItemEnter, onFocus, onActions)
            .clip(RaydroidTheme.shapes.shape(RaydroidShapeToken.Medium))
            .background(
                if (focused) {
                    RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                } else {
                    RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                }
            )
            .padding(RaydroidTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.content.isNotEmpty()) {
            ComposeRayItemRenderer(
                data = item.content,
                onClick = onClick,
                onItemEnter = onItemEnter,
                onFocus = onFocus,
                onActions = onActions
            )
        } else {
            item.icon?.let { icon -> IconRenderer(PluginIconData(icon)) }
            Column {
                RText(item.title.asText(), color = RaydroidTheme.colorScheme.onSurface)
                item.subtitle?.let { subtitle ->
                    RText(
                        subtitle.asText(),
                        fontSize = pluginFontSizeSmall(),
                        color = RaydroidTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun GridRenderer(
    data: PluginGridData,
    query: String,
    focusedItemId: CommandItemId?,
    modifier: Modifier = Modifier,
    onClick: (PluginCommandCallback) -> Unit,
    onItemEnter: (CommandItemId) -> Unit,
    onFocus: (CommandItemId) -> Unit,
    onActions: (List<PluginCommandListAction>) -> Unit
) {
    val sections = remember(data, query) { data.filtered(query) }
    Box(modifier.fillMaxWidth()) {
        if (data.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        if (sections.all { it.items.isEmpty() } && !data.isLoading) {
            EmptyViewRenderer(data.emptyView)
        } else {
            LazyVerticalGrid(
                columns = data.columns?.let { GridCells.Fixed(it.coerceIn(1, 8)) } ?: GridCells.Adaptive(GridMinWidth),
                verticalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small),
                modifier = Modifier.heightIn(max = GridMaxHeight)
            ) {
                sections.forEach { section ->
                    items(section.items, key = { it.id.value }) { item ->
                        ComponentGridItem(
                            item = item,
                            aspectRatio = data.aspectRatio,
                            focused = item.id == focusedItemId,
                            onClick = onClick,
                            onItemEnter = onItemEnter,
                            onFocus = onFocus,
                            onActions = onActions
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentGridItem(
    item: PluginGridItemData,
    aspectRatio: PluginGridAspectRatio,
    focused: Boolean,
    onClick: (PluginCommandCallback) -> Unit,
    onItemEnter: (CommandItemId) -> Unit,
    onFocus: (CommandItemId) -> Unit,
    onActions: (List<PluginCommandListAction>) -> Unit
) {
    Column(
        Modifier
            .componentInteractive(item.id, item.itemModifier, onClick, onItemEnter, onFocus, onActions)
            .clip(RaydroidTheme.shapes.shape(RaydroidShapeToken.Medium))
            .background(
                if (focused) {
                    RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                } else {
                    RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                }
            )
            .padding(RaydroidTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.extraSmall)
    ) {
        val content = item.content
        val icon = item.icon
        if (content != null) {
            ImageRenderer(
                PluginImageData(content),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio.value)
            )
        } else if (icon != null) {
            IconRenderer(PluginIconData(icon, size = PluginIconSize.Large))
        }
        RText(
            item.title.asText(),
            fontSize = pluginFontSizeSmall(),
            color = RaydroidTheme.colorScheme.onSurface
        )
        item.subtitle?.let { subtitle ->
            RText(
                subtitle.asText(),
                fontSize = pluginFontSizeSmall(),
                color = RaydroidTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyViewRenderer(emptyView: PluginEmptyViewData?) {
    Column(
        Modifier.fillMaxWidth().padding(RaydroidTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small)
    ) {
        emptyView?.icon?.let { icon -> IconRenderer(PluginIconData(icon, size = PluginIconSize.Large)) }
        RText(emptyView?.title?.asText() ?: "No items")
        emptyView?.description?.let { description -> RText(description.asText(), fontSize = pluginFontSizeSmall()) }
    }
}

private fun PluginListData.filtered(query: String): List<PluginListSectionData> {
    if (!filtering || query.isBlank()) return sections
    return sections.map { section ->
        section.copy(items = section.items.filter { item -> item.matches(query) })
    }
}

private fun PluginGridData.filtered(query: String): List<PluginGridSectionData> {
    if (!filtering || query.isBlank()) return sections
    return sections.map { section ->
        section.copy(items = section.items.filter { item -> item.matches(query) })
    }
}

private fun PluginListItemData.matches(query: String): Boolean {
    val needle = query.lowercase()
    return title.searchText().contains(needle) ||
        subtitle?.searchText()?.contains(needle) == true ||
        keywords.any { it.lowercase().contains(needle) }
}

private fun PluginGridItemData.matches(query: String): Boolean {
    val needle = query.lowercase()
    return title.searchText().contains(needle) ||
        subtitle?.searchText()?.contains(needle) == true ||
        keywords.any { it.lowercase().contains(needle) }
}

private fun PluginUiText.searchText(): String = when (this) {
    is PluginUiText.Plain -> text
    is PluginUiText.Resource -> key
}.lowercase()

private fun Modifier.componentInteractive(
    itemId: CommandItemId,
    modifier: PluginRayModifier?,
    onClick: (PluginCommandCallback) -> Unit,
    onItemEnter: (CommandItemId) -> Unit,
    onFocus: (CommandItemId) -> Unit,
    onActions: (List<PluginCommandListAction>) -> Unit
): Modifier {
    return composed {
        val interactionSource = remember { MutableInteractionSource() }
        val actions = modifier?.actions.orEmpty()
        val primaryAction = remember(actions) {
            modifier?.actions?.find { it.primary } ?: modifier?.actions?.firstOrNull()
        }
        combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = modifier?.enabled ?: true,
            onLongClick = {
                if (actions.isNotEmpty()) {
                    onActions(actions)
                }
            },
            onClick = {
                onFocus(itemId)
                if (primaryAction != null) {
                    onClick(primaryAction.callback)
                } else {
                    onItemEnter(itemId)
                }
            }
        )
    }
}

private fun List<PluginListSectionData>.indexOfListItem(itemId: CommandItemId?): Int? {
    if (itemId == null) return null
    var index = 0
    forEach { section ->
        if (section.title != null) {
            index++
        }
        section.items.forEach { item ->
            if (item.id == itemId) {
                return index
            }
            index++
        }
    }
    return null
}

private data class ParsedDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun toEpochMillis(): Long = daysFromCivil(year, month, day) * MillisPerDay
}

private fun String?.parseIsoDate(): ParsedDate? {
    if (this == null) return null
    val parts = split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val maxDay = daysInMonth(year, month)
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..maxDay } ?: return null
    return ParsedDate(year, month, day)
}

private fun formatDate(year: Int?, month: Int?, day: Int?): String? {
    if (year == null || month == null || day == null) return null
    return buildString {
        append(year)
        append("-")
        append(month.toString().padStart(2, '0'))
        append("-")
        append(day.toString().padStart(2, '0'))
    }
}

private fun Long.toIsoDate(): String {
    val days = floorDiv(this, MillisPerDay)
    val date = civilFromDays(days)
    return formatDate(date.year, date.month, date.day).orEmpty()
}

private fun daysInMonth(year: Int?, month: Int?): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year != null && isLeapYear(year)) 29 else 28
    else -> 31
}

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    var adjustedYear = year
    val adjustedMonth = month
    adjustedYear -= if (adjustedMonth <= 2) 1 else 0
    val era = floorDiv(adjustedYear, 400)
    val yearOfEra = adjustedYear - era * 400
    val monthPrime = adjustedMonth + if (adjustedMonth > 2) -3 else 9
    val dayOfYear = (153 * monthPrime + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return (era * 146097 + dayOfEra - UnixEpochDayOffset).toLong()
}

private fun civilFromDays(daysSinceEpoch: Long): ParsedDate {
    val shiftedDays = daysSinceEpoch + UnixEpochDayOffset
    val era = floorDiv(shiftedDays, 146097)
    val dayOfEra = (shiftedDays - era * 146097).toInt()
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
    var year = (yearOfEra + era * 400).toInt()
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
    val month = monthPrime + if (monthPrime < 10) 3 else -9
    year += if (month <= 2) 1 else 0
    return ParsedDate(year, month, day)
}

private fun floorDiv(a: Int, b: Int): Int {
    var result = a / b
    if ((a xor b) < 0 && result * b != a) result--
    return result
}

private fun floorDiv(a: Long, b: Long): Long {
    var result = a / b
    if ((a xor b) < 0 && result * b != a) result--
    return result
}

private const val MillisPerDay = 86_400_000L
private const val UnixEpochDayOffset = 719468

private val PluginGridAspectRatio.value: Float
    get() = when (this) {
        PluginGridAspectRatio.OneToOne -> 1f
        PluginGridAspectRatio.ThreeToTwo -> 3f / 2f
        PluginGridAspectRatio.TwoToThree -> 2f / 3f
        PluginGridAspectRatio.FourToThree -> 4f / 3f
        PluginGridAspectRatio.ThreeToFour -> 3f / 4f
        PluginGridAspectRatio.SixteenToNine -> 16f / 9f
        PluginGridAspectRatio.NineToSixteen -> 9f / 16f
    }

private val GridMinWidth = 128.dp
private val GridMaxHeight = 520.dp
@Composable
private fun pluginFontSizeSmall() = ru.raydroid.plugin.host.api.ui.PluginFontSize.Small.toTextUnit()

@Composable
private fun pluginFontSizeLarge() = ru.raydroid.plugin.host.api.ui.PluginFontSize.Large.toTextUnit()
