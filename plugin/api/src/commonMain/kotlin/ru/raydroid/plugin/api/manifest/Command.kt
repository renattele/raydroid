package ru.raydroid.plugin.api.manifest

import kotlinx.serialization.Serializable


@Serializable
data class Command(
    val service: String,
    val title: String,
    val description: String,
    val mode: Mode,
    val match: String?,
    val arguments: List<Argument>,
    val preferences: List<Preference>,
) {
    @Serializable
    enum class Mode {
        View,
        NoView,
        Inline
    }

    @Serializable
    data class Argument(
        val name: String,
        val placeholder: String,
        val type: Type,
        val required: Boolean
    ) {
        @Serializable
        enum class Type {
            Text,
            Password,
            Dropdown
        }
    }

    @Serializable
    data class Preference(
        val name: String,
        val title: String,
        val description: String,
        val type: Type,
        val placeholder: String?,
        val default: String
    ) {
        enum class Type {
            Text,
            Password,
            Checkbox,
            Dropdown,
            AppPicker,
            File,
            Directory
        }
    }
}
