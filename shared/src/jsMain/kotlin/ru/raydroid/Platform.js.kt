package ru.raydroid

class JSPlatform : Platform {
    override val name: String = "JavaScript"
}

actual fun getPlatform(): Platform = JSPlatform()
