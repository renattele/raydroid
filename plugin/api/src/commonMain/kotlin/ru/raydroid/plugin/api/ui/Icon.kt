package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

@Serializable
sealed class Icon {
    @Serializable
    data class App(val appId: String): Icon()

    @Serializable
    data class Url(val url: String): Icon()


    @Serializable
    data class Base64(val value: String): Icon()
}