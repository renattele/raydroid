package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

@DslMarker
@Target(AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)
annotation class Ray

@Serializable
sealed class RayNodeData

@Ray
interface RayScope {
    fun add(data: RayNodeData)
    fun fork(content: RayScope.() -> Unit): List<RayNodeData>
}

fun buildRayNodes(content: RayScope.() -> Unit): List<RayNodeData> {
    val nodes = mutableListOf<RayNodeData>()
    val scope = object : RayScope {
        override fun add(data: RayNodeData) {
            nodes.add(data)
        }

        override fun fork(content: RayScope.() -> Unit): List<RayNodeData> {
            val nodes = buildRayNodes(content)
            return nodes
        }
    }
    scope.content()
    return nodes
}