package ru.raydroid.plugin.host.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
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
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.Resources
import ru.raydroid.plugin.api.core.UiText
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
import ru.raydroid.plugin.host.api.Plugin

@Composable
fun ComposeRayRenderer(
    resources: FileSystem,
    manifest: Manifest,
    data: List<RayNodeData>,
    modifier: Modifier = Modifier
) {
    val locale = Locale.current
    val resolver = remember { ResourceResolverImpl(manifest, resources, locale) }
    CompositionLocalProvider(LocalResourceResolver provides resolver) {
        ComposeRayItemRenderer(data, modifier)
    }
}

private class ResourceResolverImpl(
    private val manifest: Manifest,
    private val resources: FileSystem,
    private val locale: Locale,
): ResourceResolver {
    override fun resolveString(resource: String): String {
        val language = locale.language
        val resources = manifest.resources["strings-$language"]
            ?: manifest.resources["strings"]
        if (resources == null) {
            return "NOT FOUND"
        }
        return resources[resource] ?: "NOT FOUND"
    }

    override fun resolveImage(resource: String): ByteArray? {
        val imageData = resources.read("plugin/resources/$resource".toPath()) {
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
private fun ImageRenderer(data: ImageData, modifier: Modifier = Modifier) {
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
private fun IconRenderer(data: IconData, modifier: Modifier = Modifier) {
    val resourceResolver = LocalResourceResolver.current
    val resource = remember(data.icon) {
        when (val icon = data.icon) {
            // TODO: Fix app icon resolving
            is Icon.App -> icon.appId
            is Icon.Base64 -> icon.value
            is Icon.Resource -> resourceResolver.resolveImage(icon.resource)
            is Icon.Url -> icon.url
        }
    }
    AsyncImage(
        resource,
        contentDescription = data.contentDescription,
        modifier = modifier
    )
}

@Composable
private fun BoxRenderer(data: BoxData, modifier: Modifier = Modifier) {
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
    if (data.orientation == Orientation.Vertical) {
        Column(
            modifier,
            horizontalAlignment = data.alignment.toComposeHorizontalAlignment(),
            verticalArrangement = if (data.spacing != Spacing.Zero) Arrangement.spacedBy(0.dp, data.alignment.toComposeVerticalAlignment())
            else data.arrangement.toComposeVerticalArrangement(),
        ) {
            ComposeRayItemRenderer(data.children)
        }
    } else {
        Row(
            modifier,
            horizontalArrangement =
                if (data.spacing != Spacing.Zero) Arrangement.spacedBy(0.dp, data.alignment.toComposeHorizontalAlignment())
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
private fun TextRenderer(data: TextData, modifier: Modifier = Modifier) {
    Text(data.text.asText())
}

@Composable
internal fun UiText.asText(): String = when (type) {
    UiText.Type.Plain -> this.text
    UiText.Type.Resource -> LocalResourceResolver.current.resolveString(this.text)
}