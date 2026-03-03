package ru.raydroid.core.model.ui

data class SearchListItem(
    val pluginId: String,
    val commandService: String,
    val id: ItemId,
    val icon: Icon,
    val title: UiText,
    val description: UiText
)

sealed class Icon {
    data class App(val appId: String): Icon()

    data class Url(val url: String): Icon()

    data class Resource(val resource: String): Icon()

    data class Base64(val value: String): Icon()
}

data class UiText(
    val text: String,
    val type: Type
) {
    enum class Type {
        Plain,
        Resource
    }
}
