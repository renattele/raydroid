package ru.raydroid.plugin.host.impl

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.impl.ui.toPluginCommandListItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PluginUiMapperTest {
    @Test
    fun `command list item maps actions without custom shape or motion`() {
        val pluginId = PluginId("ru.test.plugin")
        val item = CommandListItem(
            id = CommandItemId("item"),
            icon = null,
            title = UiText.Plain("Title"),
            description = UiText.Plain("Description"),
            actions = listOf(
                CommandListAction(
                    id = "delete",
                    title = UiText.Plain("Delete"),
                    description = null,
                    icon = null,
                    style = CommandListAction.Style.Destructive
                )
            )
        )

        val mapped = item.toPluginCommandListItem(pluginId)

        assertEquals("delete", mapped.actions.single().id)
        assertEquals(PluginCommandListAction.Style.Destructive, mapped.actions.single().style)
    }

    @Test
    fun `command list item maps built in icons`() {
        val pluginId = PluginId("ru.test.plugin")
        val item = CommandListItem(
            id = CommandItemId("item"),
            icon = Icon.Builtin("ArrowDropUp"),
            title = UiText.Plain("Title"),
            description = null,
        )

        val mapped = item.toPluginCommandListItem(pluginId)

        assertEquals("ArrowDropUp", assertIs<PluginIcon.Builtin>(mapped.icon).name)
    }
}
