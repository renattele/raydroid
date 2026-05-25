package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import ru.raydroid.plugin.host.api.ui.PluginIcon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import androidx.compose.material.icons.automirrored.outlined.HelpOutline as AutoMirroredHelpOutline

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

        assertSame(Icons.AutoMirrored.Outlined.AutoMirroredHelpOutline, icon)
    }

    @Test
    fun `outlined material builtin icon resolver supports manual aliases`() {
        val resolver = OutlinedMaterialBuiltinIconResolver()

        val icon = resolver.resolve("Help")

        assertSame(Icons.Outlined.HelpOutline, icon)
    }

    @Test
    fun `outlined material builtin icon resolver supports extended manual aliases`() {
        val resolver = OutlinedMaterialBuiltinIconResolver()

        assertSame(Icons.Outlined.Close, resolver.resolve("Close"))
        assertSame(Icons.Outlined.Settings, resolver.resolve("Settings"))
    }

    @Test
    fun `builtin icon mappings cover builtin names used by plugins and ios catalog`() {
        val expectedNames =
            setOf(
                "AcUnit",
                "Add",
                "Article",
                "ArrowDropUp",
                "ArrowForward",
                "AudioFile",
                "Calculate",
                "Call",
                "Check",
                "Clear",
                "Code",
                "Contacts",
                "Delete",
                "Description",
                "DeviceThermostat",
                "Edit",
                "FolderZip",
                "GridView",
                "Help",
                "Image",
                "LocationCity",
                "Message",
                "PictureAsPdf",
                "Save",
                "Search",
                "StickyNote2",
                "Thunderstorm",
                "Tune",
                "VideoFile",
                "WaterDrop",
                "WbCloudy",
                "WbSunny",
            )

        assertTrue(expectedNames.all(BuiltinIconMappings.supportedNames::contains))
    }

    @Test
    fun `plugin resource resolver resolves builtin icons as vectors`() {
        val expected = Icons.Outlined.ArrowDropUp
        val resolver =
            PluginResourceResolver(
                plugins = emptyMap(),
                language = "en",
                builtinIconResolver =
                    object : BuiltinIconResolver {
                        override fun resolve(name: String): ImageVector = expected
                    },
            )

        val resolved = resolver.resolveIcon(PluginIcon.Builtin("ArrowDropUp"))

        assertSame(expected, assertIs<ResolvedPluginIcon.Vector>(resolved).imageVector)
    }

    @Test
    fun `plugin resource resolver keeps url icons on image model path`() {
        val resolver =
            PluginResourceResolver(
                plugins = emptyMap(),
                language = "en",
                builtinIconResolver =
                    object : BuiltinIconResolver {
                        override fun resolve(name: String): ImageVector = Icons.Outlined.HelpOutline
                    },
            )

        val resolved = resolver.resolveIcon(PluginIcon.Url("https://example.com/icon.png"))

        assertEquals(
            "https://example.com/icon.png",
            assertIs<ResolvedPluginIcon.ImageModel>(resolved).model,
        )
    }
}
