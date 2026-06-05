package ru.raydroid.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import ru.raydroid.App
import ru.raydroid.feature.search.DesktopSearchController
import ru.raydroid.plugin.host.impl.DesktopPlatform
import ru.raydroid.plugin.host.impl.detectDesktopPlatform
import ru.raydroid.sharedui.appModule
import java.awt.Color
import java.awt.Taskbar
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.InputEvent
import java.awt.geom.RoundRectangle2D
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.KeyStroke
import java.awt.event.KeyEvent as AwtKeyEvent

private const val WINDOW_CORNER_RADIUS = 18.0
private const val APP_ICON_PATH = "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"

fun main() {
    initializeDesktopApplication()
    val hotReloadEnabled = System.getProperty("compose.reload.isActive") == "true"
    val desktopPlatform = detectDesktopPlatform()
    application {
        RaydroidDesktopApp(
            hotReloadEnabled = hotReloadEnabled,
            desktopPlatform = desktopPlatform,
            quitApplication = ::exitApplication,
        )
    }
}

private fun initializeDesktopApplication() {
    startKoin {
        modules(appModule)
    }
    runCatching {
        if (Taskbar.isTaskbarSupported()) {
            val image = ImageIO.read(File(APP_ICON_PATH))
            if (image != null) {
                Taskbar.getTaskbar().iconImage = image
            }
        }
    }
}

@Composable
private fun RaydroidDesktopApp(
    hotReloadEnabled: Boolean,
    desktopPlatform: DesktopPlatform,
    quitApplication: () -> Unit,
) {
    val desktopSearchController = remember { DesktopSearchController() }
    var windowVisible by remember { mutableStateOf(true) }
    val closeOrHideWindow = {
        if (desktopSearchController.state.value.hasFullscreen) {
            desktopSearchController.closeFullscreen()
        } else if (desktopPlatform == DesktopPlatform.MAC) {
            windowVisible = false
        } else {
            quitApplication()
        }
    }
    Window(
        onCloseRequest = closeOrHideWindow,
        state = rememberLauncherWindowState(),
        alwaysOnTop = true,
        resizable = false,
        undecorated = !hotReloadEnabled,
        transparent = !hotReloadEnabled,
        title = "Raydroid",
    ) {
        val showAndFocusWindow = {
            showAndFocusWindow(desktopSearchController) {
                windowVisible = true
            }
        }
        ConfigureDesktopWindow(
            hotReloadEnabled = hotReloadEnabled,
            desktopSearchController = desktopSearchController,
            closeOrHideWindow = closeOrHideWindow,
            showAndFocusWindow = showAndFocusWindow,
            quitApplication = quitApplication,
        )
        SyncWindowVisibility(
            windowVisible = windowVisible,
            desktopSearchController = desktopSearchController,
        )
        App(desktopSearchController = desktopSearchController)
    }
}

@Composable
private fun rememberLauncherWindowState() =
    rememberWindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(600.dp, 400.dp),
    )

@Composable
private fun FrameWindowScope.ConfigureDesktopWindow(
    hotReloadEnabled: Boolean,
    desktopSearchController: DesktopSearchController,
    closeOrHideWindow: () -> Unit,
    showAndFocusWindow: () -> Unit,
    quitApplication: () -> Unit,
) {
    DisposableEffect(window) {
        val shapeUpdater = createShapeUpdater(window)
        if (!hotReloadEnabled) {
            window.background = Color(0, 0, 0, 0)
            shapeUpdater.componentResized(null)
            window.addComponentListener(shapeUpdater)
        }
        val menuBar =
            createMenuBar(
                showAndFocusWindow = showAndFocusWindow,
                closeOrHideWindow = closeOrHideWindow,
                quitApplication = quitApplication,
            )
        window.jMenuBar = menuBar
        registerKeyboardActions(
            desktopSearchController = desktopSearchController,
            closeOrHideWindow = closeOrHideWindow,
            showAndFocusWindow = showAndFocusWindow,
            quitApplication = quitApplication,
        )
        onDispose {
            if (!hotReloadEnabled) {
                window.removeComponentListener(shapeUpdater)
            }
            if (window.jMenuBar === menuBar) {
                window.jMenuBar = null
            }
        }
    }
}

@Composable
private fun FrameWindowScope.SyncWindowVisibility(
    windowVisible: Boolean,
    desktopSearchController: DesktopSearchController,
) {
    LaunchedEffect(windowVisible) {
        window.isVisible = windowVisible
        if (windowVisible) {
            window.toFront()
            window.requestFocus()
            desktopSearchController.requestFocus()
        }
    }
}

private fun FrameWindowScope.showAndFocusWindow(
    desktopSearchController: DesktopSearchController,
    onShow: () -> Unit,
) {
    onShow()
    window.isVisible = true
    window.toFront()
    window.requestFocus()
    desktopSearchController.requestFocus()
}

private fun createShapeUpdater(window: java.awt.Window) =
    object : ComponentAdapter() {
        override fun componentResized(event: ComponentEvent?) {
            window.shape =
                RoundRectangle2D.Double(
                    0.0,
                    0.0,
                    window.width.toDouble(),
                    window.height.toDouble(),
                    WINDOW_CORNER_RADIUS,
                    WINDOW_CORNER_RADIUS,
                )
        }
    }

private fun createMenuBar(
    showAndFocusWindow: () -> Unit,
    closeOrHideWindow: () -> Unit,
    quitApplication: () -> Unit,
): JMenuBar {
    val menuShortcutMask =
        runCatching { Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx }
            .getOrDefault(InputEvent.META_DOWN_MASK)
    val appMenu =
        JMenu("Raydroid").apply {
            add(menuItem("Focus Search", AwtKeyEvent.VK_K, menuShortcutMask, showAndFocusWindow))
            add(menuItem("Close Window", AwtKeyEvent.VK_W, menuShortcutMask, closeOrHideWindow))
            add(menuItem("Quit Raydroid", AwtKeyEvent.VK_Q, menuShortcutMask, quitApplication))
        }
    return JMenuBar().apply {
        add(appMenu)
    }
}

private fun menuItem(
    title: String,
    keyCode: Int,
    modifiers: Int,
    action: () -> Unit,
) = JMenuItem(title).apply {
    accelerator = KeyStroke.getKeyStroke(keyCode, modifiers)
    addActionListener { action() }
}

private fun FrameWindowScope.registerKeyboardActions(
    desktopSearchController: DesktopSearchController,
    closeOrHideWindow: () -> Unit,
    showAndFocusWindow: () -> Unit,
    quitApplication: () -> Unit,
) {
    val menuShortcutMask =
        runCatching { Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx }
            .getOrDefault(InputEvent.META_DOWN_MASK)
    window.rootPane.registerKeyboardAction(
        { desktopSearchController.handleEscape() },
        KeyStroke.getKeyStroke(AwtKeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW,
    )
    window.rootPane.registerKeyboardAction(
        { closeOrHideWindow() },
        KeyStroke.getKeyStroke(AwtKeyEvent.VK_W, menuShortcutMask),
        JComponent.WHEN_IN_FOCUSED_WINDOW,
    )
    window.rootPane.registerKeyboardAction(
        { showAndFocusWindow() },
        KeyStroke.getKeyStroke(AwtKeyEvent.VK_K, menuShortcutMask),
        JComponent.WHEN_IN_FOCUSED_WINDOW,
    )
    window.rootPane.registerKeyboardAction(
        { quitApplication() },
        KeyStroke.getKeyStroke(AwtKeyEvent.VK_Q, menuShortcutMask),
        JComponent.WHEN_IN_FOCUSED_WINDOW,
    )
}
