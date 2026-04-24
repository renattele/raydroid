package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.buildCommandActions

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
    val actions: List<CommandListAction> = emptyList()
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

private data class ActionsElement(
    val actions: CommandActionScope.() -> Unit
) : Modifier.Element

fun Modifier.enabled(enabled: Boolean): Modifier {
    return then(EnabledElement(enabled))
}

fun Modifier.actions(actions: CommandActionScope.() -> Unit): Modifier {
    return then(ActionsElement(actions))
}

@Ray
interface RayScope {
    fun add(data: RayNodeData)
    fun fork(content: RayScope.() -> Unit): List<RayNodeData>
    fun modifier(modifier: Modifier): RayModifier?
}

fun buildRayNodes(
    registerCallback: (String, suspend () -> Unit) -> CommandCallbackRef,
    content: RayScope.() -> Unit
): List<RayNodeData> {
    return buildRayNodes(
        path = "node",
        registerCallback = registerCallback,
        content = content
    )
}

private fun buildRayNodes(
    path: String,
    registerCallback: (String, suspend () -> Unit) -> CommandCallbackRef,
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
                path = "$path:children:${nodeIndex++}",
                registerCallback = registerCallback,
                content = content
            )
        }

        override fun modifier(modifier: Modifier): RayModifier? {
            val modifierPath = "$path:modifier:${nodeIndex++}"
            return modifier.toRayModifier(modifierPath, registerCallback)
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
        actions = actions.flatMapIndexed { index, element ->
            buildCommandActions(
                path = "$path:actions:$index",
                registerCallback = registerCallback,
                content = element.actions
            )
        }
    )
}
