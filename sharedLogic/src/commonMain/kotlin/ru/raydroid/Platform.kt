package ru.raydroid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
