package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.manifest.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvironmentServiceBridgeImplTest {
    @Test
    fun `environment service returns looked up values and host platform`() {
        val bridge =
            EnvironmentServiceBridgeImpl(
                environmentLookup = { key ->
                    when (key) {
                        "HOME" -> "/Users/tester"
                        else -> null
                    }
                },
                platform = Platform.MacOS,
            )

        assertEquals("/Users/tester", bridge.get("HOME"))
        assertNull(bridge.get("MISSING"))
        assertEquals(Platform.MacOS, bridge.platform)
    }
}
