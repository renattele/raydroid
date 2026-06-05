package ru.raydroid.feature.search

import ru.raydroid.plugin.host.api.ui.PluginFormValue

fun createPluginTextFormValue(value: String): PluginFormValue = PluginFormValue.Text(value)

fun createPluginBooleanFormValue(value: Boolean): PluginFormValue = PluginFormValue.BooleanValue(value)

fun createPluginDateFormValue(value: String?): PluginFormValue = PluginFormValue.DateValue(value)

class PluginFormValuesBuilder {
    private val values = mutableMapOf<String, PluginFormValue>()

    fun putText(
        key: String,
        value: String,
    ) {
        values[key] = PluginFormValue.Text(value)
    }

    fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        values[key] = PluginFormValue.BooleanValue(value)
    }

    fun putDate(
        key: String,
        value: String?,
    ) {
        values[key] = PluginFormValue.DateValue(value)
    }

    fun build(): Map<String, PluginFormValue> = values.toMap()
}
