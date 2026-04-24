package ru.raydroid.plugin.host.api.ui

sealed class PluginRayNodeData {
    var modifier: PluginRayModifier? = null
}

data class PluginRayModifier(
    val enabled: Boolean = true,
    val actions: List<PluginCommandListAction> = emptyList()
)

data class PluginTextData(
    val text: PluginUiText,
    val fontSize: PluginFontSize = PluginFontSize.Medium,
    val color: PluginColor = PluginColor.OnSurface
) : PluginRayNodeData()

data class PluginIconData(
    val icon: PluginIcon,
    val contentDescription: String? = null,
    val size: PluginIconSize = PluginIconSize.Medium,
    val color: PluginColor? = null
) : PluginRayNodeData()

data class PluginImageData(
    val image: PluginImage,
    val contentDescription: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val shape: PluginShapeToken = PluginShapeToken.None
) : PluginRayNodeData()

data class PluginBoxData(
    val alignment: PluginBoxAlignment,
    val shape: PluginShapeToken = PluginShapeToken.None,
    val children: List<PluginRayNodeData>
) : PluginRayNodeData()

data class PluginOrientedBoxData(
    val orientation: PluginOrientation,
    val alignment: PluginAlignment,
    val arrangement: PluginArrangement,
    val spacing: PluginSpacing,
    val shape: PluginShapeToken = PluginShapeToken.None,
    val children: List<PluginRayNodeData>
) : PluginRayNodeData()
