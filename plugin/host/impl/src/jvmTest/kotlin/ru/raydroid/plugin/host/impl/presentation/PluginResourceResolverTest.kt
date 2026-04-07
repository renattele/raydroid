package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.ui.graphics.vector.ImageVector
import ru.raydroid.plugin.host.api.ui.PluginIcon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class PluginResourceResolverTest {
    @Test
    fun `outlined material builtin icon resolver returns outlined icon`() {
        val resolver = OutlinedMaterialBuiltinIconResolver()

        val icon = resolver.resolve("ArrowDropUp")

        assertSame(Icons.Outlined.ArrowDropUp, icon)
    }

    @Test
    fun `outlined material builtin icon resolver falls back to help outline`() {
        val resolver = OutlinedMaterialBuiltinIconResolver()

        val icon = resolver.resolve("DoesNotExist")

        assertSame(Icons.Outlined.HelpOutline, icon)
    }

    @Test
    fun `plugin resource resolver resolves builtin icons as vectors`() {
        val expected = Icons.Outlined.ArrowDropUp
        val resolver = PluginResourceResolver(
            plugins = emptyMap(),
            language = "en",
            builtinIconResolver = object : BuiltinIconResolver {
                override fun resolve(name: String): ImageVector = expected
            }
        )

        val resolved = resolver.resolveIcon(PluginIcon.Builtin("ArrowDropUp"))

        assertSame(expected, assertIs<ResolvedPluginIcon.Vector>(resolved).imageVector)
    }

    @Test
    fun `plugin resource resolver keeps url icons on image model path`() {
        val resolver = PluginResourceResolver(
            plugins = emptyMap(),
            language = "en",
            builtinIconResolver = object : BuiltinIconResolver {
                override fun resolve(name: String): ImageVector = Icons.Outlined.HelpOutline
            }
        )

        val resolved = resolver.resolveIcon(PluginIcon.Url("https://example.com/icon.png"))

        assertEquals(
            "https://example.com/icon.png",
            assertIs<ResolvedPluginIcon.ImageModel>(resolved).model
        )
    }
}
