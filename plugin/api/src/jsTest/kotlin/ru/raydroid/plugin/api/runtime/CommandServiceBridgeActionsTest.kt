package ru.raydroid.plugin.api.runtime

import kotlinx.coroutines.test.runTest
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.actions
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandServiceBridgeActionsTest {
    @Test
    fun entryWithoutModifierActionsUsesCommandActions() = runTest {
        val command = object : CommandService() {
            var clicked = 0

            override fun CommandListScope.content() {
                entry(id = ItemId)
            }

            override fun CommandActionScope.actions(target: CommandActionTarget) {
                if (target.itemId == ItemId) {
                    action(title = UiText.Plain("Default"), primary = true) {
                        clicked++
                    }
                }
            }

            override suspend fun execute(action: CommandAction) = Unit
        }
        val bridge = command.toBridge("TestCommand")
        val presentation = bridge.content().getValue(ItemId)
        val action = presentation.actions.single()

        bridge.update(CommandActionBridge.Internal(InternalCommandActionBridge.Click(action.callback)))

        assertEquals(UiText.Plain("Default"), action.title)
        assertEquals(action.callback, presentation.primaryCallback)
        assertEquals(1, command.clicked)
    }

    @Test
    fun entryModifierActionsOverrideCommandActions() = runTest {
        val command = object : CommandService() {
            var defaultClicked = 0
            var overrideClicked = 0

            override fun CommandListScope.content() {
                entry(
                    id = ItemId,
                    modifier = Modifier.actions {
                        action(title = UiText.Plain("Override"), primary = true) {
                            overrideClicked++
                        }
                    }
                )
            }

            override fun CommandActionScope.actions(target: CommandActionTarget) {
                if (target.itemId == ItemId) {
                    action(title = UiText.Plain("Default")) {
                        defaultClicked++
                    }
                }
            }

            override suspend fun execute(action: CommandAction) = Unit
        }
        val bridge = command.toBridge("TestCommand")
        val presentation = bridge.content().getValue(ItemId)
        val action = presentation.actions.single()

        bridge.update(CommandActionBridge.Internal(InternalCommandActionBridge.Click(action.callback)))

        assertEquals(UiText.Plain("Override"), action.title)
        assertEquals(0, command.defaultClicked)
        assertEquals(1, command.overrideClicked)
    }

    private companion object {
        val ItemId = CommandItemId("test.item")
    }
}
