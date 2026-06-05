package ru.raydroid.plugin.host.impl

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandCallbackId
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.ui.DetailData
import ru.raydroid.plugin.api.ui.DetailMetadataItemData
import ru.raydroid.plugin.api.ui.FormData
import ru.raydroid.plugin.api.ui.FormFieldData
import ru.raydroid.plugin.api.ui.GridAspectRatio
import ru.raydroid.plugin.api.ui.GridData
import ru.raydroid.plugin.api.ui.GridItemData
import ru.raydroid.plugin.api.ui.GridSectionData
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.ListData
import ru.raydroid.plugin.api.ui.ListItemData
import ru.raydroid.plugin.api.ui.ListSectionData
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginDetailData
import ru.raydroid.plugin.host.api.ui.PluginDetailMetadataItemData
import ru.raydroid.plugin.host.api.ui.PluginFormData
import ru.raydroid.plugin.host.api.ui.PluginGridAspectRatio
import ru.raydroid.plugin.host.api.ui.PluginGridData
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginListData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.pluginFocusModel
import ru.raydroid.plugin.host.impl.ui.toPluginCommandListAction
import ru.raydroid.plugin.host.impl.ui.toPluginCommandListItem
import ru.raydroid.plugin.host.impl.ui.toPluginRayNodeData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PluginUiMapperTest {
    @Test
    fun `command list item maps presentation fields`() {
        val pluginId = PluginId("ru.test.plugin")
        val item =
            CommandListItem(
                id = CommandItemId("item"),
                icon = null,
                title = UiText.Plain("Title"),
                description = UiText.Plain("Description"),
            )

        val mapped = item.toPluginCommandListItem(pluginId)

        assertEquals(CommandItemId("item"), mapped.id)
        assertEquals("Title", assertIs<PluginUiText.Plain>(mapped.title).text)
        assertEquals("Description", assertIs<PluginUiText.Plain>(mapped.description).text)
    }

    @Test
    fun `command list action maps style`() {
        val pluginId = PluginId("ru.test.plugin")
        val callback = CommandCallbackRef(CommandCallbackId("delete"), generation = 1)
        val action =
            CommandListAction(
                callback = callback,
                title = UiText.Plain("Delete"),
                description = null,
                icon = null,
                style = CommandListAction.Style.Destructive,
            )

        val mapped = action.toPluginCommandListAction(pluginId)

        assertEquals(callback, mapped.callback.ref)
        assertEquals(PluginCommandListAction.Style.Destructive, mapped.style)
    }

    @Test
    fun `command list item maps built in icons`() {
        val pluginId = PluginId("ru.test.plugin")
        val item =
            CommandListItem(
                id = CommandItemId("item"),
                icon = Icon.Builtin("ArrowDropUp"),
                title = UiText.Plain("Title"),
                description = null,
            )

        val mapped = item.toPluginCommandListItem(pluginId)

        assertEquals("ArrowDropUp", assertIs<PluginIcon.Builtin>(mapped.icon).name)
    }

    @Test
    fun `component nodes map nested fields and resources`() {
        val pluginId = PluginId("ru.test.plugin")

        val detail =
            DetailData(
                markdown = "# Details",
                metadata =
                    listOf(
                        DetailMetadataItemData.Label(
                            title = UiText.Resource("label.title"),
                            text = UiText.Plain("Value"),
                            icon = Icon.Resource("label-icon"),
                        ),
                    ),
            ).toPluginRayNodeData(pluginId)
        val form =
            FormData(
                fields =
                    listOf(
                        FormFieldData.Dropdown(
                            id = "mode",
                            title = UiText.Plain("Mode"),
                            options =
                                listOf(
                                    FormFieldData.Dropdown.Option("default", UiText.Resource("mode.default"), Icon.Builtin("Tune")),
                                ),
                        ),
                    ),
            ).toPluginRayNodeData(pluginId)
        val list =
            ListData(
                sections =
                    listOf(
                        ListSectionData(
                            title = UiText.Resource("section"),
                            items =
                                listOf(
                                    ListItemData(
                                        id = CommandItemId("item"),
                                        title = UiText.Plain("Item"),
                                        icon = Icon.Resource("item-icon"),
                                    ),
                                ),
                        ),
                    ),
            ).toPluginRayNodeData(pluginId)
        val grid =
            GridData(
                sections =
                    listOf(
                        GridSectionData(
                            items =
                                listOf(
                                    GridItemData(
                                        id = CommandItemId("grid"),
                                        title = UiText.Plain("Grid"),
                                        icon = Icon.Builtin("GridView"),
                                    ),
                                ),
                        ),
                    ),
                aspectRatio = GridAspectRatio.SixteenToNine,
            ).toPluginRayNodeData(pluginId)

        val mappedDetail = assertIs<PluginDetailData>(detail)
        val detailLabel = assertIs<PluginDetailMetadataItemData.Label>(mappedDetail.metadata.single())
        assertEquals("label.title", assertIs<PluginUiText.Resource>(detailLabel.title).key)
        assertEquals("label-icon", assertIs<PluginIcon.Resource>(detailLabel.icon).key)

        assertIs<PluginFormData>(form)
        val mappedList = assertIs<PluginListData>(list)
        assertEquals("section", assertIs<PluginUiText.Resource>(mappedList.sections.single().title).key)
        assertEquals(
            "item-icon",
            assertIs<PluginIcon.Resource>(
                mappedList.sections
                    .single()
                    .items
                    .single()
                    .icon,
            ).key,
        )

        val mappedGrid = assertIs<PluginGridData>(grid)
        assertEquals(PluginGridAspectRatio.SixteenToNine, mappedGrid.aspectRatio)
        assertEquals(
            "GridView",
            assertIs<PluginIcon.Builtin>(
                mappedGrid.sections
                    .single()
                    .items
                    .single()
                    .icon,
            ).name,
        )
    }

    @Test
    fun `plugin focus model extracts focused list item actions`() {
        val pluginId = PluginId("ru.test.plugin")
        val callback = CommandCallbackRef(CommandCallbackId("open"), generation = 1)
        val list =
            ListData(
                sections =
                    listOf(
                        ListSectionData(
                            items =
                                listOf(
                                    ListItemData(
                                        id = CommandItemId("one"),
                                        title = UiText.Plain("One"),
                                    ),
                                    ListItemData(
                                        id = CommandItemId("two"),
                                        title = UiText.Plain("Two"),
                                        modifier =
                                            ru.raydroid.plugin.api.ui.RayModifier(
                                                actions =
                                                    listOf(
                                                        CommandListAction(
                                                            callback = callback,
                                                            title = UiText.Plain("Open"),
                                                            description = null,
                                                            icon = null,
                                                            primary = true,
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            ).toPluginRayNodeData(pluginId)

        val model = listOf(list).pluginFocusModel(CommandItemId("two"), query = "")

        assertEquals(CommandItemId("two"), model.focusedItemId)
        assertEquals(
            callback,
            model.focusedActions
                .single()
                .callback.ref,
        )
    }
}
