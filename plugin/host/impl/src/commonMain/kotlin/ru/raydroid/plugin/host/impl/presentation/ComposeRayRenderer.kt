package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.intl.Locale
import coil3.compose.AsyncImage
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginAlignment
import ru.raydroid.plugin.host.api.ui.PluginArrangement
import ru.raydroid.plugin.host.api.ui.PluginBoxAlignment
import ru.raydroid.plugin.host.api.ui.PluginBoxData
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginImageData
import ru.raydroid.plugin.host.api.ui.PluginOrientedBoxData
import ru.raydroid.plugin.host.api.ui.PluginOrientation
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginSpacing
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.impl.resource.readBinaryResource
import ru.raydroid.plugin.host.impl.resource.resolveLocalizedString

@Composable
fun PreviewResourceResolverProvider(content: @Composable () -> Unit) {
    val resolver = remember {
        object : ResourceResolver {
            override fun resolveText(text: PluginUiText): String = when (text) {
                is PluginUiText.Plain -> text.text
                is PluginUiText.Resource -> text.key
            }

            override fun resolveIcon(icon: PluginIcon): Any? = when (icon) {
                is PluginIcon.Url -> icon.url
                is PluginIcon.Base64 -> icon.base64
                is PluginIcon.Resource -> null
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
    val resolver = remember(plugins, locale) {
        ResourceResolverImpl(plugins = plugins, language = locale.language)
    }
    CompositionLocalProvider(LocalResourceResolver provides resolver) {
        content()
    }
}

@Composable
fun ComposeRayRenderer(
    data: List<PluginRayNodeData>,
    modifier: Modifier = Modifier
) {
    ComposeRayItemRenderer(data, modifier)
}

private class ResourceResolverImpl(
    private val plugins: Map<PluginId, PluginRuntime>,
    private val language: String,
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

    override fun resolveIcon(icon: PluginIcon): Any? = when (icon) {
        is PluginIcon.Url -> icon.url
        is PluginIcon.Base64 -> icon.base64
        is PluginIcon.Resource -> plugins[icon.pluginId]?.let { runtime ->
            readBinaryResource(runtime.resources, icon.key)
        }
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
    modifier: Modifier = Modifier
) {
    data.forEach { node ->
        when (node) {
            is PluginBoxData -> BoxRenderer(node, modifier)
            is PluginOrientedBoxData -> OrientedBoxRenderer(node, modifier)
            is PluginTextData -> TextRenderer(node, modifier)
            is PluginIconData -> IconRenderer(node, modifier)
            is PluginImageData -> ImageRenderer(node, modifier)
        }
    }
}

@Composable
internal fun ImageRenderer(data: PluginImageData, modifier: Modifier = Modifier) {
    val resourceResolver = LocalResourceResolver.current
    val resource = remember(data.image) {
        resourceResolver.resolveImage(data.image)
    }
    AsyncImage(
        model = resource,
        contentDescription = data.contentDescription,
        modifier = modifier
    )
}

@Composable
internal fun IconRenderer(data: PluginIconData, modifier: Modifier = Modifier) {
    val resourceResolver = LocalResourceResolver.current
    val resource = remember(data.icon) {
        resourceResolver.resolveIcon(data.icon)
    }
    AsyncImage(
        model = resource,
        contentDescription = data.contentDescription,
        modifier = modifier.size(data.size.toDp()),
        colorFilter = data.color?.let { color -> ColorFilter.tint(color.toColor()) }
    )
}

@Composable
internal fun BoxRenderer(data: PluginBoxData, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = data.alignment.toComposeAlignment()) {
        ComposeRayItemRenderer(data.children)
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
private fun OrientedBoxRenderer(data: PluginOrientedBoxData, modifier: Modifier = Modifier) {
    if (data.orientation == PluginOrientation.Vertical) {
        Column(
            modifier,
            horizontalAlignment = data.alignment.toComposeHorizontalAlignment(),
            verticalArrangement = if (data.spacing != PluginSpacing.Zero) {
                Arrangement.spacedBy(data.spacing.toDp())
            } else {
                data.arrangement.toComposeVerticalArrangement()
            },
        ) {
            ComposeRayItemRenderer(data.children)
        }
    } else {
        Row(
            modifier,
            horizontalArrangement = if (data.spacing != PluginSpacing.Zero) {
                Arrangement.spacedBy(data.spacing.toDp())
            } else {
                data.arrangement.toComposeHorizontalArrangement()
            },
            verticalAlignment = data.alignment.toComposeVerticalAlignment()
        ) {
            ComposeRayItemRenderer(data.children)
        }
    }
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
    Text(
        text = data.text.asText(),
        modifier = modifier,
        fontSize = data.fontSize.toTextUnit(),
        color = data.color.toColor()
    )
}

@Composable
fun PluginUiText.asText(): String = LocalResourceResolver.current.resolveText(this)
