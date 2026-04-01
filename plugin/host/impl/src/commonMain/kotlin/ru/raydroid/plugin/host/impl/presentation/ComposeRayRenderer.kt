package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import okio.FileSystem
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.ui.BoxAlignment
import ru.raydroid.plugin.api.ui.BoxData
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.IconData
import ru.raydroid.plugin.api.ui.Image
import ru.raydroid.plugin.api.ui.ImageData
import ru.raydroid.plugin.api.ui.Orientation
import ru.raydroid.plugin.api.ui.OrientedBoxData
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.TextData
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime

@Composable
fun ThemeProvider(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    val themeResolver = remember { ThemeResolverImpl(colorScheme) }
    CompositionLocalProvider(LocalThemeResolver provides themeResolver) {
        content()
    }
}

@Composable
fun PreviewResourceResolverProvider(content: @Composable () -> Unit) {
    val resolver = remember {
        object : ResourceResolver {
            override fun resolveString(resource: String): String {
                return "Nothing"
            }

            override fun resolveImage(resource: String): ByteArray? {
                return null
            }
        }
    }
    CompositionLocalProvider(LocalResourceResolver provides resolver) {
        content()
    }
}

@Composable
fun ResourceResolverProvider(
    runtime: PluginRuntime,
    content: @Composable () -> Unit
) {
    val locale = Locale.current
    val resolver = remember { ResourceResolverImpl(runtime, locale) }
    CompositionLocalProvider(LocalResourceResolver provides resolver) {
        content()
    }
}

@Composable
fun ComposeRayRenderer(
    runtime: PluginRuntime,
    data: List<RayNodeData>,
    modifier: Modifier = Modifier
) {
    val locale = Locale.current
    val resolver = remember { ResourceResolverImpl(runtime, locale) }
    CompositionLocalProvider(LocalResourceResolver provides resolver) {
        ComposeRayItemRenderer(data, modifier)
    }
}

private class ResourceResolverImpl(
    private val runtime: PluginRuntime,
    private val locale: Locale,
) : ResourceResolver {
    override fun resolveString(resource: String): String {
        val language = locale.language
        val resources = runtime.manifest.resources["strings-$language"]
            ?: runtime.manifest.resources["strings"]
        if (resources == null) {
            return "NOT FOUND"
        }
        return resources[resource] ?: "NOT FOUND"
    }

    override fun resolveImage(resource: String): ByteArray? {
        val imageData = runtime.resources.read("plugin/resources/$resource".toPath()) {
            readByteArray()
        }
        return imageData
    }
}

@Composable
fun ComposeRayItemRenderer(
    data: List<RayNodeData>,
    modifier: Modifier = Modifier
) {
    data.forEach { node ->
        when (node) {
            is BoxData -> BoxRenderer(node, modifier)
            is OrientedBoxData -> OrientedBoxRenderer(node, modifier)
            is TextData -> TextRenderer(node, modifier)
            is IconData -> IconRenderer(node, modifier)
            is ImageData -> ImageRenderer(node, modifier)
        }
    }
}

@Composable
internal fun ImageRenderer(data: ImageData, modifier: Modifier = Modifier) {
    val resourceResolver = LocalResourceResolver.current
    val resource = remember(data.image) {
        when (val image = data.image) {
            is Image.Resource -> resourceResolver.resolveImage(image.resource)
            is Image.Url -> image.url
        }
    }
    AsyncImage(
        resource,
        contentDescription = data.contentDescription,
        modifier = modifier
    )
}

@Composable
internal fun IconRenderer(data: IconData, modifier: Modifier = Modifier) {
    val resourceResolver = LocalResourceResolver.current
    val themeResolver = LocalThemeResolver.current
    val resource = remember(data.icon) {
        when (val iconType = data.icon.type) {
            Icon.Type.Url -> data.icon.value
            Icon.Type.Resource -> resourceResolver.resolveImage(data.icon.value)
            Icon.Type.Base64 -> data.icon.value
        }
    }
    AsyncImage(
        resource,
        contentDescription = data.contentDescription,
        modifier = modifier.size(themeResolver.iconSize(data.size)),
    )
}

@Composable
internal fun BoxRenderer(data: BoxData, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = data.alignment.toComposeAlignment()) {
        ComposeRayItemRenderer(data.children)
    }
}

private fun BoxAlignment.toComposeAlignment() = when (this) {
    BoxAlignment.TopStart -> Alignment.TopStart
    BoxAlignment.TopCenter -> Alignment.TopCenter
    BoxAlignment.TopEnd -> Alignment.TopEnd
    BoxAlignment.CenterStart -> Alignment.CenterStart
    BoxAlignment.Center -> Alignment.Center
    BoxAlignment.CenterEnd -> Alignment.CenterEnd
    BoxAlignment.BottomStart -> Alignment.BottomStart
    BoxAlignment.BottomCenter -> Alignment.BottomCenter
    BoxAlignment.BottomEnd -> Alignment.BottomEnd
}

@Composable
private fun OrientedBoxRenderer(data: OrientedBoxData, modifier: Modifier = Modifier) {
    val themeResolver = LocalThemeResolver.current
    if (data.orientation == Orientation.Vertical) {
        Column(
            modifier,
            horizontalAlignment = data.alignment.toComposeHorizontalAlignment(),
            verticalArrangement = if (data.spacing != Spacing.Zero) Arrangement.spacedBy(
                themeResolver.spacing(data.spacing),
            )
            else data.arrangement.toComposeVerticalArrangement(),
        ) {
            ComposeRayItemRenderer(data.children)
        }
    } else {
        Row(
            modifier,
            horizontalArrangement =
                if (data.spacing != Spacing.Zero) Arrangement.spacedBy(
                    themeResolver.spacing(data.spacing)
                )
                else data.arrangement.toComposeHorizontalArrangement(),
            verticalAlignment = data.alignment.toComposeVerticalAlignment()
        ) {
            ComposeRayItemRenderer(data.children)
        }
    }
}

private fun ru.raydroid.plugin.api.ui.Alignment.toComposeHorizontalAlignment() = when (this) {
    ru.raydroid.plugin.api.ui.Alignment.Start -> Alignment.Start
    ru.raydroid.plugin.api.ui.Alignment.Center -> Alignment.CenterHorizontally
    ru.raydroid.plugin.api.ui.Alignment.End -> Alignment.End
}

private fun ru.raydroid.plugin.api.ui.Alignment.toComposeVerticalAlignment() = when (this) {
    ru.raydroid.plugin.api.ui.Alignment.Start -> Alignment.Top
    ru.raydroid.plugin.api.ui.Alignment.Center -> Alignment.CenterVertically
    ru.raydroid.plugin.api.ui.Alignment.End -> Alignment.Bottom
}

private fun ru.raydroid.plugin.api.ui.Arrangement.toComposeHorizontalArrangement() = when (this) {
    ru.raydroid.plugin.api.ui.Arrangement.Start -> Arrangement.Start
    ru.raydroid.plugin.api.ui.Arrangement.Center -> Arrangement.Center
    ru.raydroid.plugin.api.ui.Arrangement.End -> Arrangement.End
    ru.raydroid.plugin.api.ui.Arrangement.SpaceBetween -> Arrangement.SpaceBetween
    ru.raydroid.plugin.api.ui.Arrangement.SpaceAround -> Arrangement.SpaceAround
    ru.raydroid.plugin.api.ui.Arrangement.SpaceEvenly -> Arrangement.SpaceEvenly
}

private fun ru.raydroid.plugin.api.ui.Arrangement.toComposeVerticalArrangement() = when (this) {
    ru.raydroid.plugin.api.ui.Arrangement.Start -> Arrangement.Top
    ru.raydroid.plugin.api.ui.Arrangement.Center -> Arrangement.Center
    ru.raydroid.plugin.api.ui.Arrangement.End -> Arrangement.Bottom
    ru.raydroid.plugin.api.ui.Arrangement.SpaceBetween -> Arrangement.SpaceBetween
    ru.raydroid.plugin.api.ui.Arrangement.SpaceAround -> Arrangement.SpaceAround
    ru.raydroid.plugin.api.ui.Arrangement.SpaceEvenly -> Arrangement.SpaceEvenly
}

@Composable
internal fun TextRenderer(data: TextData, modifier: Modifier = Modifier) {
    val themeResolver = LocalThemeResolver.current
    Text(
        data.text.asText(),
        modifier,
        fontSize = themeResolver.fontSize(data.fontSize),
        color = themeResolver.color(data.color)
    )
}

@Composable
fun UiText.asText(): String = when (type) {
    UiText.Type.Plain -> this.text
    UiText.Type.Resource -> LocalResourceResolver.current.resolveString(this.text)
}