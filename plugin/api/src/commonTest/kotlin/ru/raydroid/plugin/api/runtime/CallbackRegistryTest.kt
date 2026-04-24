package ru.raydroid.plugin.api.runtime

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.OrientedBoxData
import ru.raydroid.plugin.api.ui.Text
import ru.raydroid.plugin.api.ui.TextData
import ru.raydroid.plugin.api.ui.actions
import ru.raydroid.plugin.api.ui.buildRayNodes
import ru.raydroid.plugin.api.ui.enabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CallbackRegistryTest {
    @Test
    fun `callback registry invokes current callback`() = runTest {
        val registry = CallbackRegistry()
        val frame = registry.beginFrame()
        var invoked = false
        val callback = frame.register("click") {
            invoked = true
        }

        assertTrue(registry.invoke(callback))

        assertTrue(invoked)
    }

    @Test
    fun `callback registry ignores stale callback`() = runTest {
        val registry = CallbackRegistry()
        val staleCallback = registry.beginFrame().register("click") {
            error("stale callback should not be invoked")
        }
        registry.beginFrame()
        registry.beginFrame()

        assertFalse(registry.invoke(staleCallback))
    }

    @Test
    fun `ray nodes carry inline actions`() = runTest {
        val registry = CallbackRegistry()
        val frame = registry.beginFrame()
        var parentActionClicked = 0
        var childActionClicked = 0

        val nodes = buildRayNodes(registerCallback = frame::register) {
            Column(
                modifier = Modifier.actions {
                    action(title = UiText.Plain("Parent action")) {
                        parentActionClicked++
                    }
                }
            ) {
                Text(
                    text = UiText.Plain("Child"),
                    modifier = Modifier
                        .actions {
                            action(title = UiText.Plain("Child action")) {
                                childActionClicked++
                            }
                        }
                )
            }
        }

        val column = assertIs<OrientedBoxData>(nodes.single())
        val child = assertIs<TextData>(column.children.single())

        val parentModifier = assertNotNull(column.modifier)
        val childModifier = assertNotNull(child.modifier)
        val parentAction = parentModifier.actions.single()
        val childAction = childModifier.actions.single()

        registry.invoke(parentAction.callback)
        registry.invoke(childAction.callback)

        assertEquals(1, parentActionClicked)
        assertEquals(1, childActionClicked)
    }

    @Test
    fun `modifier action callbacks run in current plugin coroutine context`() = runTest {
        val registry = CallbackRegistry()
        val frame = registry.beginFrame()
        var callbackContextName: String? = null

        val nodes = buildRayNodes(registerCallback = frame::register) {
            Text(
                text = UiText.Plain("Context"),
                modifier = Modifier.actions {
                    action(title = UiText.Plain("Context")) {
                        callbackContextName = currentCoroutineContext()[CoroutineName]?.name
                    }
                }
            )
        }

        val text = assertIs<TextData>(nodes.single())
        val callback = assertNotNull(text.modifier).actions.single().callback

        withContext(CoroutineName("plugin-context")) {
            registry.invoke(callback)
        }

        assertEquals("plugin-context", callbackContextName)
    }

    @Test
    fun `modifier enabled uses last value`() = runTest {
        val registry = CallbackRegistry()
        val frame = registry.beginFrame()

        val nodes = buildRayNodes(registerCallback = frame::register) {
            Text(
                text = UiText.Plain("Enabled"),
                modifier = Modifier
                    .enabled(false)
                    .enabled(true)
            )
        }

        val text = assertIs<TextData>(nodes.single())

        assertTrue(assertNotNull(text.modifier).enabled)
    }
}
