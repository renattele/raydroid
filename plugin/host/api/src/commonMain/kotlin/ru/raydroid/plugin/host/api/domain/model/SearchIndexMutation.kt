package ru.raydroid.plugin.host.api.domain.model

import ru.raydroid.plugin.api.presentation.CommandListItem

sealed class SearchIndexMutation {
    data class Upsert(
        val resultId: SearchResultId,
        val listEntry: CommandListItem
    ) : SearchIndexMutation()

    data class Delete(
        val resultId: SearchResultId
    ) : SearchIndexMutation()

    data class ClearOutdated(
        val pluginId: PluginId,
        val commandName: String
    ) : SearchIndexMutation()

    data class MarkAllAsOutdated(
        val pluginId: PluginId,
        val commandName: String
    ): SearchIndexMutation()
}
