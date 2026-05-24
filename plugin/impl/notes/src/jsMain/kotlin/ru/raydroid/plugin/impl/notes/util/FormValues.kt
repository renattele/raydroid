package ru.raydroid.plugin.impl.notes.util

import ru.raydroid.plugin.api.ui.FormValue

internal fun Map<String, FormValue>.text(id: String): String = (get(id) as? FormValue.Text)?.value.orEmpty()
