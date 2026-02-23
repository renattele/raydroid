package ru.raydroid.plugin.host.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import ru.raydroid.plugin.api.core.RayItem
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.api.ui.BoxAlignment
import ru.raydroid.plugin.api.ui.BoxData
import ru.raydroid.plugin.api.ui.Orientation
import ru.raydroid.plugin.api.ui.OrientedBoxData
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.TextData

@Composable
fun ComposeRayItemRenderer(
    data: State<RayItems>
) {
    data.value.forEach { (_, nodes) ->
        ComposeRayItemRenderer(nodes)
    }
}

@Composable
fun ComposeRayItemRenderer(
    data: List<RayNodeData>
) {
    data.forEach { node ->
        when (node) {
            is BoxData -> BoxRenderer(node)
            is OrientedBoxData -> OrientedBoxRenderer(node)
            is TextData -> TextRenderer(node)
        }
    }
}

@Composable
private fun BoxRenderer(data: BoxData) {
    Box(contentAlignment = data.alignment.toComposeAlignment()) {
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
private fun OrientedBoxRenderer(data: OrientedBoxData) {
    if (data.orientation == Orientation.Vertical) {
        Column(
            horizontalAlignment = data.alignment.toComposeHorizontalAlignment(),
            verticalArrangement = if (data.spacing != Spacing.Zero) Arrangement.spacedBy(0.dp, data.alignment.toComposeVerticalAlignment())
            else data.arrangement.toComposeVerticalArrangement(),
        ) {
            ComposeRayItemRenderer(data.children)
        }
    } else {
        Row(
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
private fun TextRenderer(data: TextData) {
    Text(data.text)
}