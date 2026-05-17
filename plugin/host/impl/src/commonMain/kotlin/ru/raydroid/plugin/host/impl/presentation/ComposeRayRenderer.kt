package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import coil3.compose.AsyncImage
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RIcon
import ru.raydroid.core.designsystem.component.RText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginAlignment
import ru.raydroid.plugin.host.api.ui.PluginArrangement
import ru.raydroid.plugin.host.api.ui.PluginBoxAlignment
import ru.raydroid.plugin.host.api.ui.PluginBoxData
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginDetailData
import ru.raydroid.plugin.host.api.ui.PluginEditableTextData
import ru.raydroid.plugin.host.api.ui.PluginFormData
import ru.raydroid.plugin.host.api.ui.PluginGridData
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginImageData
import ru.raydroid.plugin.host.api.ui.PluginListData
import ru.raydroid.plugin.host.api.ui.PluginOrientation
import ru.raydroid.plugin.host.api.ui.PluginOrientedBoxData
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginRayModifier
import ru.raydroid.plugin.host.api.ui.PluginShapeToken
import ru.raydroid.plugin.host.api.ui.PluginSpacing
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginFontWeight
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.impl.resource.readBinaryResource
import ru.raydroid.plugin.host.impl.resource.resolveLocalizedString

@Composable
fun PreviewResourceResolverProvider(content: @Composable () -> Unit) {
    val builtinIconResolver = remember { OutlinedMaterialBuiltinIconResolver() }
    val resolver = remember {
        object : ResourceResolver {
            override fun resolveText(text: PluginUiText): String = when (text) {
                is PluginUiText.Plain -> text.text
                is PluginUiText.Resource -> text.key
            }

            override fun resolveIcon(icon: PluginIcon): ResolvedPluginIcon? = when (icon) {
                is PluginIcon.Url -> ResolvedPluginIcon.ImageModel(icon.url)
                is PluginIcon.Base64 -> ResolvedPluginIcon.ImageModel(icon.base64)
                is PluginIcon.Resource -> null
                is PluginIcon.Builtin -> ResolvedPluginIcon.Vector(builtinIconResolver.resolve(icon.name))
            }

            override fun resolveImage(image: PluginImage): Any? = when (image) {
                is PluginImage.Url -> image.url
                is PluginImage.Resource -> null
            }
        }
    }
    CompositionLocalProvider(LocalResourceResolver provides resolver) {
        content()
    }
}

@Composable
fun ResourceResolverProvider(
    plugins: Map<PluginId, PluginRuntime>,
    content: @Composable () -> Unit
) {
    val locale = Locale.current
    val builtinIconResolver = remember { OutlinedMaterialBuiltinIconResolver() }
    val resolver = remember(plugins, locale) {
        PluginResourceResolver(
            plugins = plugins,
            language = locale.language,
            builtinIconResolver = builtinIconResolver
        )
    }
    CompositionLocalProvider(LocalResourceResolver provides resolver) {
        content()
    }
}

@Composable
fun ComposeRayRenderer(
    data: List<PluginRayNodeData>,
    modifier: Modifier = Modifier,
    query: String = "",
    focusedItemId: CommandItemId? = null,
    onClick: (PluginCommandCallback) -> Unit = {},
    onItemEnter: (CommandItemId) -> Unit = {},
    onFocus: (CommandItemId) -> Unit = {},
    onActions: (List<PluginCommandListAction>) -> Unit = {}
) {
    ComposeRayItemRenderer(data, modifier, query, focusedItemId, onClick, onItemEnter, onFocus, onActions)
}

internal class PluginResourceResolver(
    private val plugins: Map<PluginId, PluginRuntime>,
    private val language: String,
    private val builtinIconResolver: BuiltinIconResolver,
) : ResourceResolver {
    override fun resolveText(text: PluginUiText): String = when (text) {
        is PluginUiText.Plain -> text.text
        is PluginUiText.Resource -> {
            val runtime = plugins[text.pluginId]
            runtime?.let {
                resolveLocalizedString(it.manifest.resources, text.key, language)
            } ?: text.key
        }
    }

    override fun resolveIcon(icon: PluginIcon): ResolvedPluginIcon? = when (icon) {
        is PluginIcon.Url -> ResolvedPluginIcon.ImageModel(icon.url)
        is PluginIcon.Base64 -> ResolvedPluginIcon.ImageModel(icon.base64)
        is PluginIcon.Resource -> plugins[icon.pluginId]
            ?.let { runtime -> readBinaryResource(runtime.resources, icon.key) }
            ?.let(ResolvedPluginIcon::ImageModel)
        is PluginIcon.Builtin -> ResolvedPluginIcon.Vector(builtinIconResolver.resolve(icon.name))
    }

    override fun resolveImage(image: PluginImage): Any? = when (image) {
        is PluginImage.Url -> image.url
        is PluginImage.Resource -> plugins[image.pluginId]?.let { runtime ->
            readBinaryResource(runtime.resources, image.key)
        }
    }
}

@Composable
fun ComposeRayItemRenderer(
    data: List<PluginRayNodeData>,
    modifier: Modifier = Modifier,
    query: String = "",
    focusedItemId: CommandItemId? = null,
    onClick: (PluginCommandCallback) -> Unit = {},
    onItemEnter: (CommandItemId) -> Unit = {},
    onFocus: (CommandItemId) -> Unit = {},
    onActions: (List<PluginCommandListAction>) -> Unit = {}
) {
    data.forEach { node ->
        val nodeModifier = modifier
            .pluginLayout(node.modifier)
            .interactive(node.modifier, onClick, onActions)
        when (node) {
            is PluginBoxData -> BoxRenderer(node, nodeModifier, query, focusedItemId, onClick, onItemEnter, onFocus, onActions)
            is PluginOrientedBoxData -> OrientedBoxRenderer(node, nodeModifier, query, focusedItemId, onClick, onItemEnter, onFocus, onActions)
            is PluginTextData -> TextRenderer(node, nodeModifier)
            is PluginIconData -> IconRenderer(node, nodeModifier)
            is PluginImageData -> ImageRenderer(node, nodeModifier)
            is PluginDetailData -> DetailRenderer(node, nodeModifier)
            is PluginEditableTextData -> EditableTextRenderer(node, nodeModifier)
            is PluginFormData -> FormRenderer(node, nodeModifier)
            is PluginListData -> ListRenderer(node, query, focusedItemId, nodeModifier, onClick, onItemEnter, onFocus, onActions)
            is PluginGridData -> GridRenderer(node, query, focusedItemId, nodeModifier, onClick, onItemEnter, onFocus, onActions)
        }
    }
}

@Composable
internal fun ImageRenderer(data: PluginImageData, modifier: Modifier = Modifier) {
    val resourceResolver = LocalResourceResolver.current
    val resource = remember(resourceResolver, data.image) {
        resourceResolver.resolveImage(data.image)
    }
    AsyncImage(
        model = resource,
        contentDescription = data.contentDescription,
        modifier = modifier.clip(data.shape.toShape())
    )
}

@Composable
internal fun IconRenderer(data: PluginIconData, modifier: Modifier = Modifier) {
    val resourceResolver = LocalResourceResolver.current
    val resource = remember(resourceResolver, data.icon) {
        resourceResolver.resolveIcon(data.icon)
    }
    when (resource) {
        is ResolvedPluginIcon.ImageModel -> AsyncImage(
            model = resource.model,
            contentDescription = data.contentDescription,
            modifier = modifier.size(data.size.toDp()),
            colorFilter = data.color?.let { color -> ColorFilter.tint(color.toColor()) }
        )
        is ResolvedPluginIcon.Vector -> RIcon(
            imageVector = resource.imageVector,
            contentDescription = data.contentDescription,
            modifier = modifier.size(data.size.toDp()),
            tint = data.color?.toColor() ?: LocalContentColor.current
        )
        null -> Unit
    }
}

@Composable
internal fun BoxRenderer(
    data: PluginBoxData,
    modifier: Modifier = Modifier,
    query: String = "",
    focusedItemId: CommandItemId? = null,
    onClick: (PluginCommandCallback) -> Unit = {},
    onItemEnter: (CommandItemId) -> Unit = {},
    onFocus: (CommandItemId) -> Unit = {},
    onActions: (List<PluginCommandListAction>) -> Unit = {}
) {
    Box(
        modifier
            .clip(data.shape.toShape())
            .surfaceBackground(data.shape),
        contentAlignment = data.alignment.toComposeAlignment()
    ) {
        ComposeRayItemRenderer(
            data.children,
            query = query,
            focusedItemId = focusedItemId,
            onClick = onClick,
            onItemEnter = onItemEnter,
            onFocus = onFocus,
            onActions = onActions
        )
    }
}

private fun PluginBoxAlignment.toComposeAlignment() = when (this) {
    PluginBoxAlignment.TopStart -> Alignment.TopStart
    PluginBoxAlignment.TopCenter -> Alignment.TopCenter
    PluginBoxAlignment.TopEnd -> Alignment.TopEnd
    PluginBoxAlignment.CenterStart -> Alignment.CenterStart
    PluginBoxAlignment.Center -> Alignment.Center
    PluginBoxAlignment.CenterEnd -> Alignment.CenterEnd
    PluginBoxAlignment.BottomStart -> Alignment.BottomStart
    PluginBoxAlignment.BottomCenter -> Alignment.BottomCenter
    PluginBoxAlignment.BottomEnd -> Alignment.BottomEnd
}

@Composable
private fun OrientedBoxRenderer(
    data: PluginOrientedBoxData,
    modifier: Modifier = Modifier,
    query: String = "",
    focusedItemId: CommandItemId? = null,
    onClick: (PluginCommandCallback) -> Unit = {},
    onItemEnter: (CommandItemId) -> Unit = {},
    onFocus: (CommandItemId) -> Unit = {},
    onActions: (List<PluginCommandListAction>) -> Unit = {}
) {
    val containerModifier = modifier
        .clip(data.shape.toShape())
        .surfaceBackground(data.shape)
    if (data.orientation == PluginOrientation.Vertical) {
        Column(
            containerModifier,
            horizontalAlignment = data.alignment.toComposeHorizontalAlignment(),
            verticalArrangement = if (data.spacing != PluginSpacing.Zero) {
                Arrangement.spacedBy(data.spacing.toDp())
            } else {
                data.arrangement.toComposeVerticalArrangement()
            },
        ) {
            data.children.forEach { child ->
                val childModifier = child.modifier
                    ?.weight
                    ?.let { weight -> Modifier.weight(weight) }
                    ?: Modifier
                ComposeRayItemRenderer(
                    listOf(child),
                    modifier = childModifier,
                    query = query,
                    focusedItemId = focusedItemId,
                    onClick = onClick,
                    onItemEnter = onItemEnter,
                    onFocus = onFocus,
                    onActions = onActions
                )
            }
        }
    } else {
        Row(
            containerModifier,
            horizontalArrangement = if (data.spacing != PluginSpacing.Zero) {
                Arrangement.spacedBy(data.spacing.toDp())
            } else {
                data.arrangement.toComposeHorizontalArrangement()
            },
            verticalAlignment = data.alignment.toComposeVerticalAlignment()
        ) {
            data.children.forEach { child ->
                val childModifier = Modifier.weight(child.modifier?.weight ?: 1f)
                ComposeRayItemRenderer(
                    listOf(child),
                    modifier = childModifier,
                    query = query,
                    focusedItemId = focusedItemId,
                    onClick = onClick,
                    onItemEnter = onItemEnter,
                    onFocus = onFocus,
                    onActions = onActions
                )
            }
        }
    }
}

@Composable
private fun Modifier.pluginLayout(modifier: PluginRayModifier?): Modifier {
    var result = this
    if (modifier?.fillMaxSize == true) {
        result = result.fillMaxSize()
    }
    modifier?.padding?.let { padding ->
        result = result.padding(padding.toDp())
    }
    return result
}

private fun PluginAlignment.toComposeHorizontalAlignment() = when (this) {
    PluginAlignment.Start -> Alignment.Start
    PluginAlignment.Center -> Alignment.CenterHorizontally
    PluginAlignment.End -> Alignment.End
}

private fun PluginAlignment.toComposeVerticalAlignment() = when (this) {
    PluginAlignment.Start -> Alignment.Top
    PluginAlignment.Center -> Alignment.CenterVertically
    PluginAlignment.End -> Alignment.Bottom
}

private fun PluginArrangement.toComposeHorizontalArrangement() = when (this) {
    PluginArrangement.Start -> Arrangement.Start
    PluginArrangement.Center -> Arrangement.Center
    PluginArrangement.End -> Arrangement.End
    PluginArrangement.SpaceBetween -> Arrangement.SpaceBetween
    PluginArrangement.SpaceAround -> Arrangement.SpaceAround
    PluginArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
}

private fun PluginArrangement.toComposeVerticalArrangement() = when (this) {
    PluginArrangement.Start -> Arrangement.Top
    PluginArrangement.Center -> Arrangement.Center
    PluginArrangement.End -> Arrangement.Bottom
    PluginArrangement.SpaceBetween -> Arrangement.SpaceBetween
    PluginArrangement.SpaceAround -> Arrangement.SpaceAround
    PluginArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
}

@Composable
internal fun TextRenderer(data: PluginTextData, modifier: Modifier = Modifier) {
    RText(
        text = data.text.asText(),
        modifier = modifier,
        fontSize = data.fontSize.toTextUnit(),
        fontWeight = data.fontWeight.toComposeFontWeight(),
        color = data.color.toColor()
    )
}

private fun PluginFontWeight.toComposeFontWeight(): FontWeight? = when (this) {
    PluginFontWeight.Normal -> null
    PluginFontWeight.Bold -> FontWeight.Bold
}

@Composable
fun PluginUiText.asText(): String = LocalResourceResolver.current.resolveText(this)

private fun Modifier.interactive(
    modifier: PluginRayModifier?,
    onClick: (PluginCommandCallback) -> Unit,
    onActions: (List<PluginCommandListAction>) -> Unit
): Modifier {
    if (modifier == null || (modifier.click == null && modifier.actions.isEmpty())) {
        return this
    }
    return composed {
        val interactionSource = remember { MutableInteractionSource() }
        val primaryAction = remember(modifier.actions) {
            modifier.actions.find { it.primary } ?: modifier.actions.firstOrNull()
        }
        combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = modifier.enabled,
            onLongClick = {
                if (modifier.actions.isNotEmpty()) {
                    onActions(modifier.actions)
                }
            },
            onClick = {
                val click = modifier.click
                if (click != null) {
                    onClick(click)
                } else if (primaryAction != null) {
                    onClick(primaryAction.callback)
                }
            }
        )
    }
}

@Composable
private fun Modifier.surfaceBackground(shape: PluginShapeToken): Modifier {
    if (shape == PluginShapeToken.None) return this
    return background(RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
        .padding(RaydroidTheme.spacing.large)
}
