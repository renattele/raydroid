package ru.raydroid.plugin.host.impl.services

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.impl.event.EventGatewayImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class NotificationServiceBridgeImplTest {
    private val pluginId = PluginId("ru.test.plugin")

    @Test
    fun `show toast returns handle id from emitted toast event`() = runTest {
        val eventGateway = EventGatewayImpl()
        val bridge = NotificationServiceBridgeImpl(eventGateway, pluginId)
        val event = async(start = CoroutineStart.UNDISPATCHED) {
            eventGateway.get(pluginId)
                .map { pluginEvent -> pluginEvent.data }
                .filter { data -> data is NotificationEvent.ShowToast }
                .take(1)
                .toList()
        }

        val handle = bridge.showToast(
            NotificationServiceBridge.Toast(
                message = UiText.Plain("Saved"),
                style = NotificationServiceBridge.Toast.Style.Success
            )
        )

        val showToast = assertIs<NotificationEvent.ShowToast>(event.await().single())
        assertEquals(handle.id, showToast.toastId)
        assertEquals(5_000L, showToast.toast.autoDismissMillis)
    }

    @Test
    fun `identical toasts receive independent ids`() = runTest {
        val eventGateway = EventGatewayImpl()
        val bridge = NotificationServiceBridgeImpl(eventGateway, pluginId)
        val events = async(start = CoroutineStart.UNDISPATCHED) {
            eventGateway.get(pluginId)
                .map { pluginEvent -> pluginEvent.data }
                .filter { data -> data is NotificationEvent.ShowToast }
                .take(2)
                .toList()
                .map { data -> assertIs<NotificationEvent.ShowToast>(data) }
        }
        val toast = NotificationServiceBridge.Toast(
            message = UiText.Plain("Saved"),
            style = NotificationServiceBridge.Toast.Style.Success
        )

        val first = bridge.showToast(toast)
        val second = bridge.showToast(toast)

        val shownToasts = events.await()
        assertEquals(first.id, shownToasts[0].toastId)
        assertEquals(second.id, shownToasts[1].toastId)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `hide toast emits exact handle id`() = runTest {
        val eventGateway = EventGatewayImpl()
        val bridge = NotificationServiceBridgeImpl(eventGateway, pluginId)
        val event = async(start = CoroutineStart.UNDISPATCHED) {
            eventGateway.get(pluginId)
                .map { pluginEvent -> pluginEvent.data }
                .filter { data -> data is NotificationEvent.HideToast }
                .take(1)
                .toList()
        }

        bridge.hideToast("toast-id")

        val hideToast = assertIs<NotificationEvent.HideToast>(event.await().single())
        assertEquals("toast-id", hideToast.toastId)
    }
}
