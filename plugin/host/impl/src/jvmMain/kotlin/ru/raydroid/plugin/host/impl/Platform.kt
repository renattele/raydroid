package ru.raydroid.plugin.host.impl

enum class DesktopPlatform { WINDOWS, MAC, LINUX, OTHER }

fun detectDesktopPlatform(): DesktopPlatform {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> DesktopPlatform.WINDOWS
        os.contains("mac") || os.contains("darwin") -> DesktopPlatform.MAC
        os.contains("nux") || os.contains("nix") -> DesktopPlatform.LINUX
        else -> DesktopPlatform.OTHER
    }
}