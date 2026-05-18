package ru.raydroid.feature.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandCallbackId
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.application.usecase.CloseCommandUseCase
import ru.raydroid.plugin.host.api.application.usecase.CommandActionDispatcher
import ru.raydroid.plugin.host.api.application.usecase.EmitEventUseCase
import ru.raydroid.plugin.host.api.application.usecase.EnterItemUseCase
import ru.raydroid.plugin.host.api.application.usecase.ExecuteCommandCallbackUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetCommandFullscreenUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetEventsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetSearchFieldRequestsUseCase
import ru.raydroid.plugin.host.api.application.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.application.usecase.OpenCommandUseCase
import ru.raydroid.plugin.host.api.application.usecase.RemoveSearchAliasUseCase
import ru.raydroid.plugin.host.api.application.usecase.SaveSearchAliasUseCase
import ru.raydroid.plugin.host.api.application.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.application.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.api.application.usecase.UpdateCommandQueryUseCase
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginDescriptor
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchAliasEntry
import ru.raydroid.plugin.host.api.domain.model.SearchAliasSaveResult
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultScore
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.repository.PluginRepository
import ru.raydroid.plugin.host.api.domain.repository.SearchAliasRepository
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.api.domain.service.SearchResultRanker
import ru.raydroid.plugin.host.api.event.EventGateway
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.api.event.PluginEvent
import ru.raydroid.plugin.host.api.event.SearchFieldGateway
import ru.raydroid.plugin.host.api.event.SearchFieldRequest
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginUiText
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun `open search updates query state`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.viewModel.openSearch("calc")

        val state = fixture.viewModel.currentState()
        assertEquals("calc", state.searchFieldState.query)
        assertEquals(SearchFieldSelection.CursorAtEnd, state.searchFieldState.selection)
    }

    @Test
    fun `open command updates query state`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.viewModel.openCommand("calculator")

        val state = fixture.viewModel.currentState()
        assertEquals("calculator", state.searchFieldState.query)
        assertEquals(SearchFieldSelection.CursorAtEnd, state.searchFieldState.selection)
    }

    @Test
    fun `toggle actions opens and hide closes overlays`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.viewModel.toggleActions()
        assertEquals(true, fixture.viewModel.currentState().overlayState.showActions)

        fixture.viewModel.hideActions()
        val state = fixture.viewModel.currentState()
        assertEquals(false, state.overlayState.showActions)
        assertEquals(false, state.overlayState.showContextActions)
    }

    @Test
    fun `show context actions tracks active source`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.runtime.actionResults = listOf(
            PluginCommandListAction(
                callback = callback("open"),
                title = PluginUiText.Plain("Open"),
                description = null,
                icon = null
            )
        )
        advanceUntilIdle()

        fixture.viewModel.dispatchEvent(
            SearchScreenEvent.ShowContextActions(
                resultId = fixture.resultId,
                sourceId = "result-row-1",
                actions = fixture.runtime.actionResults
            )
        )

        val state = fixture.viewModel.currentState().overlayState
        assertEquals(true, state.showContextActions)
        assertEquals(false, state.showActions)
        assertEquals("result-row-1", state.activeContextSourceId)
        assertEquals(listOf(PluginUiText.Plain("Open")), state.contextActions.map { it.action.title })

        fixture.viewModel.hideActions()
        assertEquals(null, fixture.viewModel.currentState().overlayState.activeContextSourceId)
    }

    @Test
    fun `result long press ignores empty actions`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.commands.value = listOf(
            PluginRuntimeCoordinator.CommandItem(
                runtime = fixture.runtime,
                listEntry = listEntry("Calculator", "Evaluate"),
                resultId = fixture.resultId
            )
        )

        fixture.viewModel.start()
        fixture.viewModel.openSearch("calc")
        advanceUntilIdle()

        fixture.viewModel.onEvent(
            SearchScreenEvent.ShowResultContextActions(
                resultId = fixture.resultId,
                sourceId = "result-row-1"
            )
        )
        advanceUntilIdle()

        val state = fixture.viewModel.currentState().overlayState
        assertEquals(false, state.showContextActions)
        assertEquals(emptyList(), state.contextActions)
        assertEquals(null, state.activeContextSourceId)
    }

    @Test
    fun `toast events are shown then hidden`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.viewModel.start()
        runCurrent()

        val toast = NotificationEvent.ShowToast(
            pluginId = fixture.pluginId,
            toastId = "toast-1",
            toast = NotificationEvent.Toast(
                message = PluginUiText.Plain("Done"),
                style = NotificationEvent.Toast.Style.Success,
                autoDismissMillis = null
            )
        )
        fixture.events.emit(
            PluginEvent(
                id = PluginEvent.Id("toast"),
                pluginId = fixture.pluginId,
                data = toast
            )
        )
        runCurrent()

        assertEquals(listOf(toast), fixture.viewModel.currentState().toasts)

        fixture.viewModel.hideToast("toast-1")
        runCurrent()
        assertEquals(emptyList(), fixture.viewModel.currentState().toasts)
    }

    @Test
    fun `toast with same id replaces existing toast`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.viewModel.start()
        runCurrent()

        val firstToast = NotificationEvent.ShowToast(
            pluginId = fixture.pluginId,
            toastId = "toast-1",
            toast = NotificationEvent.Toast(
                message = PluginUiText.Plain("Loading"),
                style = NotificationEvent.Toast.Style.Animated,
                autoDismissMillis = null
            )
        )
        val secondToast = NotificationEvent.ShowToast(
            pluginId = fixture.pluginId,
            toastId = "toast-1",
            toast = NotificationEvent.Toast(
                message = PluginUiText.Plain("Done"),
                style = NotificationEvent.Toast.Style.Success,
                autoDismissMillis = null
            )
        )

        fixture.events.emit(
            PluginEvent(
                id = PluginEvent.Id("toast-1"),
                pluginId = fixture.pluginId,
                data = firstToast
            )
        )
        fixture.events.emit(
            PluginEvent(
                id = PluginEvent.Id("toast-1b"),
                pluginId = fixture.pluginId,
                data = secondToast
            )
        )
        runCurrent()

        assertEquals(listOf(secondToast), fixture.viewModel.currentState().toasts)
    }

    @Test
    fun `animated toasts with same message collapse to one toast`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.viewModel.start()
        runCurrent()

        val firstToast = NotificationEvent.ShowToast(
            pluginId = fixture.pluginId,
            toastId = "toast-1",
            toast = NotificationEvent.Toast(
                message = PluginUiText.Plain("Loading..."),
                style = NotificationEvent.Toast.Style.Animated,
                autoDismissMillis = null
            )
        )
        val secondToast = NotificationEvent.ShowToast(
            pluginId = fixture.pluginId,
            toastId = "toast-2",
            toast = NotificationEvent.Toast(
                message = PluginUiText.Plain("Loading..."),
                style = NotificationEvent.Toast.Style.Animated,
                autoDismissMillis = null
            )
        )

        fixture.events.emit(
            PluginEvent(
                id = PluginEvent.Id("toast-2a"),
                pluginId = fixture.pluginId,
                data = firstToast
            )
        )
        fixture.events.emit(
            PluginEvent(
                id = PluginEvent.Id("toast-2b"),
                pluginId = fixture.pluginId,
                data = secondToast
            )
        )
        runCurrent()

        assertEquals(listOf(secondToast), fixture.viewModel.currentState().toasts)
    }

    @Test
    fun `confirm alert removes alert and emits confirm event`() = runTest {
        val fixture = SearchViewModelFixture(this)
        fixture.viewModel.start()
        runCurrent()

        val alert = NotificationEvent.Alert(
            pluginId = fixture.pluginId,
            title = PluginUiText.Plain("Heads up"),
            message = PluginUiText.Plain("Proceed"),
            confirmAction = NotificationEvent.AlertAction(
                title = PluginUiText.Plain("OK"),
                style = NotificationEvent.AlertAction.Style.Default
            )
        )

        fixture.events.emit(
            PluginEvent(
                id = PluginEvent.Id("alert"),
                pluginId = fixture.pluginId,
                data = alert
            )
        )
        runCurrent()
        assertEquals(listOf(alert), fixture.viewModel.currentState().alerts)

        fixture.viewModel.confirmAlert(alert)
        runCurrent()

        assertEquals(emptyList(), fixture.viewModel.currentState().alerts)
        val emitted = fixture.eventGateway.emitted.single()
        assertEquals(fixture.pluginId, emitted.first)
        assertEquals(
            NotificationEvent.AlertResult(NotificationEvent.Selection.Confirm),
            emitted.second
        )
    }

    @Test
    fun `command with alias exposes host alias actions`() = runTest {
        val fixture = SearchViewModelFixture(this)
        val result = SearchResultSet.CommandSearchResult(
            resultId = fixture.resultId,
            listEntry = PluginCommandListItem(
                id = CommandItemId.CommandRoot,
                icon = null,
                title = PluginUiText.Plain("Calculator"),
                description = null,
                alias = "oy"
            )
        )
        val actions = result.actions(emptyMap()).map { it.title }
        assertEquals(
            listOf(
                PluginUiText.Plain("Edit Alias"),
                PluginUiText.Plain("Remove Alias")
            ),
            actions
        )
    }

    @Test
    fun `duplicate alias keeps editor open with inline error`() = runTest {
        val fixture = SearchViewModelFixture(this)
        val editAction = FocusedCommandAction(
            resultId = fixture.resultId,
            action = SearchPanelAction(
                title = PluginUiText.Plain("Edit Alias"),
                kind = SearchPanelAction.Kind.OpenAliasEditor(existingAlias = "calc")
            )
        )
        fixture.aliasRepository.nextSaveResult = SearchAliasSaveResult.Conflict(
            alias = "oy",
            existingResultId = SearchResultId(
                pluginId = PluginId("ru.raydroid.notes"),
                commandName = "notes",
                itemId = CommandItemId.CommandRoot
            )
        )

        fixture.viewModel.onEvent(SearchScreenEvent.EnterAction(editAction))
        fixture.viewModel.onEvent(SearchScreenEvent.UpdateAliasEditorInput("oy"))
        fixture.viewModel.onEvent(SearchScreenEvent.SaveAliasEditor)
        advanceUntilIdle()

        assertEquals(
            PluginUiText.Plain("Alias 'oy' is already in use"),
            fixture.viewModel.currentState().overlayState.aliasEditor?.error
        )
    }

    @Test
    fun `next focus picks first result when nothing is focused`() {
        assertEquals(0, nextSearchResultsFocusIndex(currentIndex = null, resultCount = 2))
    }

    @Test
    fun `previous focus picks last result when nothing is focused`() {
        assertEquals(1, previousSearchResultsFocusIndex(currentIndex = null, resultCount = 2))
    }
}

private class SearchViewModelFixture(testScope: kotlinx.coroutines.test.TestScope) {
    val pluginId = PluginId("ru.raydroid.calculator")
    val resultId = SearchResultId(
        pluginId = pluginId,
        commandName = "calculator",
        itemId = CommandItemId.CommandRoot
    )
    val runtime = FakePluginRuntime(
        manifest = manifest(
            pluginId = pluginId,
            commandName = "calculator",
            placeholder = UiText.Plain("Type a query")
        )
    )
    val runtimes = MutableStateFlow(listOf<PluginRuntime>(runtime))
    val commands = MutableStateFlow<List<PluginRuntimeCoordinator.CommandItem>>(emptyList())
    val content = MutableStateFlow<List<PluginRuntimeCoordinator.ContentItem>>(emptyList())
    val cachedItems = MutableStateFlow<Map<PluginRuntime, List<SearchIndexMutation>>>(emptyMap())
    val eventGateway = FakeEventGateway()
    val events = MutableSharedFlow<PluginEvent<*>>(extraBufferCapacity = 8)
    val searchFieldGateway = FakeSearchFieldGateway()
    val searchFieldRequests = MutableSharedFlow<SearchFieldRequest>(extraBufferCapacity = 8)
    val searchRepository = FakeSearchIndexRepository()
    val aliasRepository = FakeSearchAliasRepository()

    private val coordinator = FakePluginRuntimeCoordinator(
        runtimes = runtimes,
        commands = commands,
        content = content,
        cachedItems = cachedItems
    )
    private val registry = FakePluginRuntimeRegistry(coordinator)

    val viewModel = SearchViewModel(
        applicationScope = testScope,
        syncCacheUseCase = SyncCacheUseCase(searchRepository, registry),
        loadRuntimesUseCase = LoadRuntimesUseCase(
            pluginRepository = EmptyPluginRepository(),
            pluginRuntimeRegistry = registry,
            pluginLoader = EmptyPluginLoader()
        ),
        searchUseCase = SearchUseCase(registry, searchRepository, FakeSearchResultRanker(), aliasRepository),
        getPluginsUseCase = GetPluginsUseCase(registry),
        openCommandUseCase = OpenCommandUseCase(CommandActionDispatcher(registry), searchRepository),
        enterItemUseCase = EnterItemUseCase(CommandActionDispatcher(registry), searchRepository),
        closeCommandUseCase = CloseCommandUseCase(CommandActionDispatcher(registry)),
        executeCommandCallbackUseCase = ExecuteCommandCallbackUseCase(searchRepository),
        getCommandFullscreenUseCase = GetCommandFullscreenUseCase(registry),
        getEventsUseCase = GetEventsUseCase(eventGateway.also { gateway ->
            gateway.source = events
        }),
        emitEventUseCase = EmitEventUseCase(eventGateway),
        getSearchFieldRequestsUseCase = GetSearchFieldRequestsUseCase(searchFieldGateway.also { gateway ->
            gateway.source = searchFieldRequests
        }),
        updateCommandQueryUseCase = UpdateCommandQueryUseCase(registry),
        saveSearchAliasUseCase = SaveSearchAliasUseCase(aliasRepository),
        removeSearchAliasUseCase = RemoveSearchAliasUseCase(aliasRepository)
    )
}

private class FakePluginRuntimeCoordinator(
    private val runtimes: StateFlow<List<PluginRuntime>>,
    private val commands: StateFlow<List<PluginRuntimeCoordinator.CommandItem>>,
    private val content: StateFlow<List<PluginRuntimeCoordinator.ContentItem>>,
    private val cachedItems: Flow<Map<PluginRuntime, List<SearchIndexMutation>>>
) : PluginRuntimeCoordinator {
    val updates = mutableListOf<CommandActionBridge>()

    override fun cachedItems(): Flow<Map<PluginRuntime, List<SearchIndexMutation>>> = cachedItems

    override fun runtimes(): StateFlow<List<PluginRuntime>> = runtimes

    override fun content(): StateFlow<List<PluginRuntimeCoordinator.ContentItem>> = content

    override fun commands(): StateFlow<List<PluginRuntimeCoordinator.CommandItem>> = commands

    override suspend fun update(action: CommandActionBridge) {
        updates += action
    }
}

private class FakePluginRuntimeRegistry(
    private val coordinator: PluginRuntimeCoordinator
) : PluginRuntimeRegistry {
    override suspend fun load(runtime: PluginRuntime) = Unit

    override suspend fun unload(runtime: PluginRuntime) = Unit

    override fun get(): PluginRuntimeCoordinator = coordinator
}

private class FakePluginRuntime(
    override val manifest: Manifest
) : PluginRuntime {
    override val pluginId: PluginId = PluginId(manifest.name)
    override val resources: FileSystem = FakeFileSystem()
    var actionResults: List<PluginCommandListAction> = emptyList()
    private val fullscreen = MutableStateFlow(
        PluginRuntime.FullscreenContent(
            commandName = "calculator",
            content = emptyList()
        )
    )
    val commandUpdates = mutableListOf<Pair<String, CommandActionBridge>>()

    override fun cachedItems(chunkSize: Int): Flow<List<SearchIndexMutation>> = emptyFlow()

    override fun content(): StateFlow<List<PluginRuntime.ContentItem>> = MutableStateFlow(emptyList())

    override fun fullscreen(commandName: String): StateFlow<PluginRuntime.FullscreenContent?> = fullscreen

    override suspend fun actions(
        commandName: String,
        itemId: CommandItemId
    ): List<PluginCommandListAction> = actionResults

    override suspend fun update(action: CommandActionBridge) = Unit

    override suspend fun update(commandName: String, action: CommandActionBridge) {
        commandUpdates += commandName to action
    }

    override suspend fun back(commandName: String): Boolean = false

    override suspend fun unload() = Unit
}

private class FakeSearchIndexRepository : SearchIndexRepository {
    val results = MutableStateFlow<List<RankedSearchResult>>(emptyList())
    val usageUpdates = mutableListOf<SearchResultId>()
    var preview: RankedSearchResult? = null

    override suspend fun update(mutations: List<SearchIndexMutation>) = Unit

    override suspend fun updateUsage(resultId: SearchResultId) {
        usageUpdates += resultId
    }

    override suspend fun getPreview(resultId: SearchResultId): RankedSearchResult? = preview

    override fun search(query: String, limit: Int): Flow<List<RankedSearchResult>> = results
}

private class FakeSearchAliasRepository : SearchAliasRepository {
    val aliases = MutableStateFlow<Map<SearchResultId, String>>(emptyMap())
    var nextSaveResult: SearchAliasSaveResult? = null

    override fun observeAliases(): Flow<Map<SearchResultId, String>> = aliases

    override suspend fun saveAlias(
        resultId: SearchResultId,
        alias: String
    ): SearchAliasSaveResult {
        nextSaveResult?.let { result ->
            nextSaveResult = null
            return result
        }
        aliases.value = aliases.value + (resultId to alias)
        return SearchAliasSaveResult.Success(SearchAliasEntry(resultId, alias))
    }

    override suspend fun removeAlias(resultId: SearchResultId) {
        aliases.value = aliases.value - resultId
    }

    override suspend fun resolveExactAlias(alias: String): SearchResultId? {
        return aliases.value.entries.firstOrNull { entry -> entry.value == alias }?.key
    }
}

private class FakeSearchResultRanker : SearchResultRanker {
    override fun rankLive(
        query: String,
        contentSnapshot: List<PluginRuntimeCoordinator.ContentItem>,
        limit: Int
    ): List<RankedSearchResult> {
        return contentSnapshot.map { item ->
            RankedSearchResult(
                result = SearchResultSet.LiveSearchResult(
                    resultId = item.resultId,
                    listEntry = item.listEntry,
                    presentation = item.presentation
                ),
                score = SearchResultScore(textScore = 1.0, live = true)
            )
        }
    }

    override fun rankCommands(
        query: String,
        commandsSnapshot: List<PluginRuntimeCoordinator.CommandItem>,
        limit: Int
    ): List<RankedSearchResult> {
        return commandsSnapshot.map { item ->
            RankedSearchResult(
                result = SearchResultSet.CommandSearchResult(
                    resultId = item.resultId,
                    listEntry = item.listEntry
                ),
                score = SearchResultScore(textScore = 1.0, prefix = true)
            )
        }
    }

    override fun merge(
        commandResults: List<RankedSearchResult>,
        liveResults: List<RankedSearchResult>,
        cachedResults: List<RankedSearchResult>,
        limit: Int
    ): List<SearchResultSet.SearchResult> {
        return (commandResults + liveResults + cachedResults)
            .take(limit)
            .map { it.result }
    }
}

private class FakeEventGateway : EventGateway {
    lateinit var source: Flow<PluginEvent<*>>
    val emitted = mutableListOf<Pair<PluginId, Any>>()

    override fun get(): Flow<PluginEvent<*>> = source

    override fun get(pluginId: PluginId): Flow<PluginEvent<*>> = source

    override suspend fun emit(pluginId: PluginId, data: Any) {
        emitted += pluginId to data
    }
}

private class FakeSearchFieldGateway : SearchFieldGateway {
    lateinit var source: Flow<SearchFieldRequest>

    override fun get(): Flow<SearchFieldRequest> = source

    override suspend fun emit(request: SearchFieldRequest) = Unit
}

private class EmptyPluginRepository : PluginRepository {
    override suspend fun installPlugin(url: String) = Unit

    override suspend fun listInstalledPlugins(): List<PluginId> = emptyList()

    override suspend fun loadPlugin(pluginId: PluginId): PluginArtifact? = null

    override suspend fun deletePlugin(pluginId: PluginId) = Unit
}

private class EmptyPluginLoader : PluginLoader {
    override suspend fun loadPlugin(plugin: PluginArtifact): PluginRuntime? = null

    override suspend fun loadPluginMetadata(plugin: PluginArtifact): PluginDescriptor {
        return object : PluginDescriptor {
            override val pluginId: PluginId = PluginId.Invalid
            override val manifest: Manifest = manifest(
                pluginId = PluginId.Invalid,
                commandName = "noop"
            )
            override val resources: FileSystem = FakeFileSystem()
        }
    }

    override suspend fun join(
        plugins: StateFlow<List<PluginRuntime>>
    ): PluginRuntimeCoordinator {
        error("Not used in SearchViewModel tests")
    }
}

private fun manifest(
    pluginId: PluginId,
    commandName: String,
    placeholder: UiText? = null
): Manifest {
    return Manifest(
        name = pluginId.id,
        title = UiText.Plain("Calculator"),
        description = UiText.Plain("Calculator extension"),
        author = UiText.Plain("Raydroid"),
        version = 1,
        platforms = listOf(Platform.IOS),
        categories = emptyList(),
        license = "MIT",
        commands = listOf(
            Command(
                service = commandName,
                title = UiText.Plain("Calculator"),
                description = UiText.Plain("Calculator extension"),
                placeholder = placeholder,
                mode = Command.Mode.View,
                match = null,
                searchable = true,
                arguments = emptyList(),
                preferences = emptyList()
            )
        ),
        resources = emptyMap()
    )
}

private fun listEntry(title: String, description: String): PluginCommandListItem {
    return PluginCommandListItem(
        id = CommandItemId.CommandRoot,
        icon = null,
        title = PluginUiText.Plain(title),
        description = PluginUiText.Plain(description)
    )
}

private fun callback(name: String): ru.raydroid.plugin.host.api.ui.PluginCommandCallback {
    return ru.raydroid.plugin.host.api.ui.PluginCommandCallback(
        ref = CommandCallbackRef(CommandCallbackId(name), generation = 0),
        dispatch = {}
    )
}
