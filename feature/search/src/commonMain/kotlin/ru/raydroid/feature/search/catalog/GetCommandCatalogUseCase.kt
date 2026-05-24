package ru.raydroid.feature.search

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.host.api.application.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.application.usecase.LoadRuntimesUseCase

class GetCommandCatalogUseCase(
    private val loadRuntimesUseCase: LoadRuntimesUseCase,
    private val getPluginsUseCase: GetPluginsUseCase,
) {
    suspend operator fun invoke(): List<CommandCatalogEntry> {
        loadRuntimesUseCase()
        return getPluginsUseCase()
            .value
            .flatMap { runtime ->
                runtime.manifest.commands.map { command ->
                    CommandCatalogEntry(
                        id = command.service,
                        title = command.title.toPluginUiText(runtime.pluginId),
                    )
                }
            }.distinctBy(CommandCatalogEntry::id)
            .sortedBy(CommandCatalogEntry::id)
    }
}

class CommandCatalogProvider(
    private val getCommandCatalogUseCase: GetCommandCatalogUseCase,
    private val getPluginsUseCase: GetPluginsUseCase,
) {
    suspend fun entries(): List<CommandCatalogEntry> = getCommandCatalogUseCase()

    suspend fun commands(language: String): List<ResolvedCommandCatalogEntry> {
        val entries = getCommandCatalogUseCase()
        val resolver =
            PluginResourceResolver(
                plugins =
                    getPluginsUseCase()
                        .value
                        .associateBy { runtime -> runtime.pluginId },
                language = language,
            )
        return entries.map { entry ->
            ResolvedCommandCatalogEntry(
                id = entry.id,
                title = resolver.resolveText(entry.title),
            )
        }
    }
}

private fun UiText.toPluginUiText(
    pluginId: ru.raydroid.plugin.host.api.domain.model.PluginId,
): ru.raydroid.plugin.host.api.ui.PluginUiText =
    when (type) {
        UiText.Type.Plain -> {
            ru.raydroid.plugin.host.api.ui.PluginUiText
                .Plain(text)
        }

        UiText.Type.Resource -> {
            ru.raydroid.plugin.host.api.ui.PluginUiText.Resource(
                pluginId = pluginId,
                key = text,
            )
        }
    }
