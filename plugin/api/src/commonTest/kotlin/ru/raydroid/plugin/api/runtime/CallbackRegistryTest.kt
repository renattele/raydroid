package ru.raydroid.plugin.api.runtime

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.OrientedBoxData
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Text
import ru.raydroid.plugin.api.ui.TextData
import ru.raydroid.plugin.api.ui.actions
import ru.raydroid.plugin.api.ui.buildRayNodes
import ru.raydroid.plugin.api.ui.Detail
import ru.raydroid.plugin.api.ui.DetailData
import ru.raydroid.plugin.api.ui.Form
import ru.raydroid.plugin.api.ui.FormData
import ru.raydroid.plugin.api.ui.FormValue
import ru.raydroid.plugin.api.ui.Grid
import ru.raydroid.plugin.api.ui.GridData
import ru.raydroid.plugin.api.ui.LazyGrid
import ru.raydroid.plugin.api.ui.LazyList
import ru.raydroid.plugin.api.ui.List
import ru.raydroid.plugin.api.ui.ListData
import ru.raydroid.plugin.api.ui.enabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CallbackRegistryTest {
    @Test
    fun `focus action updates service focused item id`() = runTest {
        val service = object : CommandService() {
            override fun CommandListScope.content() = Unit

            override suspend fun execute(action: CommandAction) = Unit
        }
        val itemId = CommandItemId("focused")

        service.update(CommandActionBridge.Regular(CommandAction.Focus(itemId)))

        assertEquals(itemId, service.focusedItemId)
    }

    @Test
    fun `plugin render can read focused item id`() = runTest {
        val itemId = CommandItemId("focused")
        val service = object : CommandService() {
            override fun CommandListScope.content() = Unit

            override fun RayScope.fullscreen() {
                Text(UiText.Plain(focusedItemId?.value ?: "none"))
            }

            override suspend fun execute(action: CommandAction) = Unit
        }

        service.update(CommandActionBridge.Regular(CommandAction.Focus(itemId)))
        val frame = CallbackRegistry().beginFrame()
        val node = assertIs<TextData>(
            buildRayNodes(registerCallback = frame::register) {
                with(service) {
                    fullscreen()
                }
            }.single()
        )

        assertEquals("focused", node.text.text)
    }

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

    @Test
    fun `component dsl builds detail form list and grid nodes`() = runTest {
        val registry = CallbackRegistry()
        val frame = registry.beginFrame()

        val nodes = buildRayNodes(
            registerCallback = frame::register,
            registerFormCallback = frame::registerForm
        ) {
            Detail(markdown = "# Title")
            Form {
                textField(id = "name", title = UiText.Plain("Name"))
                submit(UiText.Plain("Save")) {}
            }
            List {
                item(id = CommandItemId("list.item"), title = UiText.Plain("List"))
            }
            LazyList {
                item(id = CommandItemId("lazy.list.item"), title = UiText.Plain("Lazy List"))
            }
            Grid {
                item(id = CommandItemId("grid.item"), title = UiText.Plain("Grid"))
            }
            LazyGrid {
                item(id = CommandItemId("lazy.grid.item"), title = UiText.Plain("Lazy Grid"))
            }
        }

        assertIs<DetailData>(nodes[0])
        assertIs<FormData>(nodes[1])
        assertIs<ListData>(nodes[2])
        assertTrue(assertIs<ListData>(nodes[3]).lazy)
        assertIs<GridData>(nodes[4])
        assertTrue(assertIs<GridData>(nodes[5]).lazy)
    }

    @Test
    fun `form submit callback receives current values`() = runTest {
        val registry = CallbackRegistry()
        val frame = registry.beginFrame()
        var submitted: Map<String, FormValue>? = null

        val nodes = buildRayNodes(
            registerCallback = frame::register,
            registerFormCallback = frame::registerForm
        ) {
            Form {
                textField(id = "name", defaultValue = "Initial")
                submit(UiText.Plain("Save")) { values ->
                    submitted = values
                }
            }
        }

        val form = assertIs<FormData>(nodes.single())
        val callback = assertNotNull(form.submit).callback
        registry.invoke(callback, mapOf("name" to FormValue.Text("Updated")))

        assertEquals(FormValue.Text("Updated"), submitted?.get("name"))
    }
}
