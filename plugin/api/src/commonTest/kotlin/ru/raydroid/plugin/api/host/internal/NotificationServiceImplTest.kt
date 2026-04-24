package ru.raydroid.plugin.api.host.internal

import kotlinx.coroutines.test.runTest
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.api.model.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationServiceImplTest {
    @Test
    fun `scoped toast hides after block completes`() = runTest {
        val bridge = FakeNotificationServiceBridge()
        val service = NotificationServiceImpl(bridge)

        val result = service.showLoadingToast("Loading") {
            "done"
        }

        assertEquals("done", result)
        assertEquals(listOf("show:toast-1", "hide:toast-1"), bridge.calls)
    }

    @Test
    fun `scoped toast hides after block fails`() = runTest {
        val bridge = FakeNotificationServiceBridge()
        val service = NotificationServiceImpl(bridge)

        assertFailsWith<IllegalStateException> {
            service.showLoadingToast("Loading") {
                error("failed")
            }
        }

        assertEquals(listOf("show:toast-1", "hide:toast-1"), bridge.calls)
    }

    @Test
    fun `scoped success toast is not auto dismissed by default`() = runTest {
        val bridge = FakeNotificationServiceBridge()
        val service = NotificationServiceImpl(bridge)

        service.showSuccessToast("Saved") {
            Unit
        }

        assertEquals(null, bridge.shownToasts.single().autoDismissMillis)
    }

    private class FakeNotificationServiceBridge : NotificationServiceBridge {
        val calls = mutableListOf<String>()
        val shownToasts = mutableListOf<NotificationServiceBridge.Toast>()

        override suspend fun alert(
            title: UiText,
            message: UiText,
            confirmAction: NotificationServiceBridge.AlertAction,
            dismissAction: NotificationServiceBridge.AlertAction?
        ): NotificationServiceBridge.AlertAction? = confirmAction

        override suspend fun showToast(toast: NotificationServiceBridge.Toast): NotificationServiceBridge.ToastHandle {
            val id = "toast-${calls.count { call -> call.startsWith("show:") } + 1}"
            shownToasts += toast
            calls += "show:$id"
            return NotificationServiceBridge.ToastHandle(id)
        }

        override suspend fun hideToast(toastId: String) {
            calls += "hide:$toastId"
        }
    }
}
