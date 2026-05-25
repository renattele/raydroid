package ru.raydroid.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import java.awt.Toolkit
import java.awt.Taskbar
import java.awt.Color
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.InputEvent
import java.awt.geom.RoundRectangle2D
import java.awt.event.KeyEvent as AwtKeyEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.KeyStroke

private const val WINDOW_CORNER_RADIUS = 18.0

fun main() {
    startKoin {
        modules(appModule)
    }
    runCatching {
        if (Taskbar.isTaskbarSupported()) {
            val image = ImageIO.read(File("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"))
            if (image != null) {
                Taskbar.getTaskbar().iconImage = image
            }
        }
    }
    val hotReloadEnabled = System.getProperty("compose.reload.isActive") == "true"
    val desktopPlatform = detectDesktopPlatform()
    application {
        val desktopSearchController = remember { DesktopSearchController() }
        var windowVisible by remember { mutableStateOf(true) }
        val closeOrHideWindow = {
            if (desktopSearchController.state.value.hasFullscreen) {
                desktopSearchController.closeFullscreen()
            } else if (desktopPlatform == DesktopPlatform.MAC) {
                windowVisible = false
            } else {
                exitApplication()
            }
        }
        val windowState =
            rememberWindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(600.dp, 400.dp),
            )
        Window(
            onCloseRequest = closeOrHideWindow,
            state = windowState,
            alwaysOnTop = true,
            resizable = false,
            undecorated = !hotReloadEnabled,
            transparent = !hotReloadEnabled,
            title = "Raydroid",
        ) {
            val showAndFocusWindow = {
                windowVisible = true
                window.isVisible = true
                window.toFront()
                window.requestFocus()
                desktopSearchController.requestFocus()
            }
            DisposableEffect(window) {
                val shapeUpdater =
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
                if (!hotReloadEnabled) {
                    window.background = Color(0, 0, 0, 0)
                    shapeUpdater.componentResized(null)
                    window.addComponentListener(shapeUpdater)
                }
                val menuShortcutMask =
                    runCatching { Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx }
                        .getOrDefault(InputEvent.META_DOWN_MASK)
                val menuBar = JMenuBar()
                val appMenu = JMenu("Raydroid")
                val focusItem =
                    JMenuItem("Focus Search").apply {
                        accelerator = KeyStroke.getKeyStroke(AwtKeyEvent.VK_K, menuShortcutMask)
                        addActionListener {
                            showAndFocusWindow()
                        }
                    }
                val closeItem =
                    JMenuItem("Close Window").apply {
                        accelerator = KeyStroke.getKeyStroke(AwtKeyEvent.VK_W, menuShortcutMask)
                        addActionListener {
                            closeOrHideWindow()
                        }
                    }
                val quitItem =
                    JMenuItem("Quit Raydroid").apply {
                        accelerator = KeyStroke.getKeyStroke(AwtKeyEvent.VK_Q, menuShortcutMask)
                        addActionListener {
                            exitApplication()
                        }
                    }
                appMenu.add(focusItem)
                appMenu.add(closeItem)
                appMenu.add(quitItem)
                menuBar.add(appMenu)
                window.jMenuBar = menuBar
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
                    { exitApplication() },
                    KeyStroke.getKeyStroke(AwtKeyEvent.VK_Q, menuShortcutMask),
                    JComponent.WHEN_IN_FOCUSED_WINDOW,
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
            LaunchedEffect(windowVisible) {
                window.isVisible = windowVisible
                if (windowVisible) {
                    window.toFront()
                    window.requestFocus()
                    desktopSearchController.requestFocus()
                }
            }
            App(desktopSearchController = desktopSearchController)
        }
    }
}
