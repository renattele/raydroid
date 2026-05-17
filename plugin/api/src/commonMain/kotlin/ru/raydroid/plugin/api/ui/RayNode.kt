package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.buildCommandActions
import ru.raydroid.plugin.api.ui.FormValues

@DslMarker
@Target(AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)
annotation class Ray

@Serializable
sealed class RayNodeData {
    var modifier: RayModifier? = null
}

@Serializable
data class RayModifier(
    val enabled: Boolean = true,
    val click: CommandCallbackRef? = null,
    val actions: List<CommandListAction> = emptyList(),
    val weight: Float? = null,
    val fillMaxSize: Boolean = false,
    val padding: Spacing? = null
)

@Ray
interface Modifier {
    fun then(other: Modifier): Modifier

    @Ray
    interface Element : Modifier {
        override fun then(other: Modifier): Modifier {
            return CombinedModifier(this, other)
        }
    }

    companion object : Modifier {
        override fun then(other: Modifier): Modifier {
            return other
        }
    }
}

private data class CombinedModifier(
    val outer: Modifier,
    val inner: Modifier
) : Modifier {
    override fun then(other: Modifier): Modifier {
        return CombinedModifier(this, other)
    }
}

private data class EnabledElement(
    val enabled: Boolean
) : Modifier.Element

private data class ClickElement(
    val onClick: suspend () -> Unit
) : Modifier.Element

private data class ActionsElement(
    val actions: CommandActionScope.() -> Unit
) : Modifier.Element

private data class WeightElement(
    val weight: Float
) : Modifier.Element

private data object FillMaxSizeElement : Modifier.Element

private data class PaddingElement(
    val spacing: Spacing
) : Modifier.Element

fun Modifier.enabled(enabled: Boolean): Modifier {
    return then(EnabledElement(enabled))
}

fun Modifier.onClick(onClick: suspend () -> Unit): Modifier {
    return then(ClickElement(onClick))
}

fun Modifier.actions(actions: CommandActionScope.() -> Unit): Modifier {
    return then(ActionsElement(actions))
}

fun Modifier.weight(weight: Float): Modifier {
    return then(WeightElement(weight))
}

fun Modifier.fillMaxSize(): Modifier {
    return then(FillMaxSizeElement)
}

fun Modifier.padding(spacing: Spacing): Modifier {
    return then(PaddingElement(spacing))
}

@Ray
interface RayScope {
    fun add(data: RayNodeData)
    fun fork(content: RayScope.() -> Unit): List<RayNodeData>
    fun modifier(modifier: Modifier): RayModifier?
    fun registerFormCallback(path: String, callback: suspend (FormValues) -> Unit): CommandCallbackRef
}

fun buildRayNodes(
    registerCallback: (String, suspend () -> Unit) -> CommandCallbackRef,
    registerFormCallback: (String, suspend (FormValues) -> Unit) -> CommandCallbackRef = { path, callback ->
        registerCallback(path) { callback(emptyMap()) }
    },
    content: RayScope.() -> Unit
): List<RayNodeData> {
    return buildRayNodes(
        nodePath = "node",
        registerCallback = registerCallback,
        registerFormCallback = registerFormCallback,
        content = content
    )
}

private fun buildRayNodes(
    nodePath: String,
    registerCallback: (String, suspend () -> Unit) -> CommandCallbackRef,
    registerFormCallback: (String, suspend (FormValues) -> Unit) -> CommandCallbackRef,
    content: RayScope.() -> Unit
): List<RayNodeData> {
    val nodes = mutableListOf<RayNodeData>()
    var nodeIndex = 0
    val scope = object : RayScope {
        override fun add(data: RayNodeData) {
            nodes.add(data)
        }

        override fun fork(content: RayScope.() -> Unit): List<RayNodeData> {
            return buildRayNodes(
                nodePath = "$nodePath:children:${nodeIndex++}",
                registerCallback = registerCallback,
                registerFormCallback = registerFormCallback,
                content = content
            )
        }

        override fun modifier(modifier: Modifier): RayModifier? {
            val modifierPath = "$nodePath:modifier:${nodeIndex++}"
            return modifier.toRayModifier(modifierPath, registerCallback)
        }

        override fun registerFormCallback(path: String, callback: suspend (FormValues) -> Unit): CommandCallbackRef {
            return registerFormCallback("$nodePath:form:$path", callback)
        }
    }
    scope.content()
    return nodes
}

internal fun <T : RayNodeData> T.withModifier(modifier: RayModifier?): T {
    this.modifier = modifier
    return this
}

internal fun Modifier.elements(): List<Modifier.Element> = when (this) {
    Modifier -> emptyList()
    is Modifier.Element -> listOf(this)
    is CombinedModifier -> outer.elements() + inner.elements()
    else -> emptyList()
}

internal fun Modifier.hasActions(): Boolean {
    return elements().any { element -> element is ActionsElement }
}

internal fun Modifier.toRayModifier(
    path: String,
    registerCallback: (String, suspend () -> Unit) -> CommandCallbackRef
): RayModifier? {
    val elements = elements()
    if (elements.isEmpty()) {
        return null
    }
    val actions = elements.filterIsInstance<ActionsElement>()
    return RayModifier(
        enabled = elements.filterIsInstance<EnabledElement>().lastOrNull()?.enabled ?: true,
        click = elements.filterIsInstance<ClickElement>().lastOrNull()?.let { element ->
            registerCallback("$path:click", element.onClick)
        },
        actions = actions.flatMapIndexed { index, element ->
            buildCommandActions(
                path = "$path:actions:$index",
                registerCallback = registerCallback,
                content = element.actions
            )
        },
        weight = elements.filterIsInstance<WeightElement>().lastOrNull()?.weight,
        fillMaxSize = elements.any { element -> element is FillMaxSizeElement },
        padding = elements.filterIsInstance<PaddingElement>().lastOrNull()?.spacing
    )
}
