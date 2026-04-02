package ru.raydroid.plugin.host.api.ui

sealed class PluginRayNodeData

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
    val height: Int? = null
) : PluginRayNodeData()

data class PluginBoxData(
    val alignment: PluginBoxAlignment,
    val children: List<PluginRayNodeData>
) : PluginRayNodeData()

data class PluginOrientedBoxData(
    val orientation: PluginOrientation,
    val alignment: PluginAlignment,
    val arrangement: PluginArrangement,
    val spacing: PluginSpacing,
    val children: List<PluginRayNodeData>
) : PluginRayNodeData()
