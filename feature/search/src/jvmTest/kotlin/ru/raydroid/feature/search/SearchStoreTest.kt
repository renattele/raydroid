package ru.raydroid.feature.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import ru.raydroid.plugin.host.api.application.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.application.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.api.application.usecase.UpdateCommandQueryUseCase
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginDescriptor
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultScore
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.repository.PluginRepository
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
class SearchStoreTest {
    @Test
    fun `open search updates query state`() = runTest {
        val fixture = SearchStoreFixture(this)
        fixture.store.openSearch("calc")

        val state = fixture.store.currentState()
        assertEquals("calc", state.searchFieldState.query)
        assertEquals(SearchFieldSelection.CursorAtEnd, state.searchFieldState.selection)
    }

    @Test
    fun `open command updates query state`() = runTest {
        val fixture = SearchStoreFixture(this)
        fixture.store.openCommand("calculator")

        val state = fixture.store.currentState()
        assertEquals("calculator", state.searchFieldState.query)
        assertEquals(SearchFieldSelection.CursorAtEnd, state.searchFieldState.selection)
    }

    @Test
    fun `toggle actions opens and hide closes overlays`() = runTest {
        val fixture = SearchStoreFixture(this)
        fixture.store.toggleActions()
        assertEquals(true, fixture.store.currentState().showActions)

        fixture.store.hideActions()
        val state = fixture.store.currentState()
        assertEquals(false, state.showActions)
        assertEquals(false, state.showContextActions)
    }

    @Test
    fun `toast events are shown then hidden`() = runTest {
        val fixture = SearchStoreFixture(this)
        fixture.store.start()
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

        assertEquals(listOf(toast), fixture.store.currentState().toasts)

        fixture.store.hideToast("toast-1")
        runCurrent()
        assertEquals(emptyList(), fixture.store.currentState().toasts)
    }

    @Test
    fun `toast with same id replaces existing toast`() = runTest {
        val fixture = SearchStoreFixture(this)
        fixture.store.start()
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

        assertEquals(listOf(secondToast), fixture.store.currentState().toasts)
    }

    @Test
    fun `animated toasts with same message collapse to one toast`() = runTest {
        val fixture = SearchStoreFixture(this)
        fixture.store.start()
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

        assertEquals(listOf(secondToast), fixture.store.currentState().toasts)
    }

    @Test
    fun `confirm alert removes alert and emits confirm event`() = runTest {
        val fixture = SearchStoreFixture(this)
        fixture.store.start()
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
        assertEquals(listOf(alert), fixture.store.currentState().alerts)

        fixture.store.confirmAlert(alert)
        runCurrent()

        assertEquals(emptyList(), fixture.store.currentState().alerts)
        val emitted = fixture.eventGateway.emitted.single()
        assertEquals(fixture.pluginId, emitted.first)
        assertEquals(
            NotificationEvent.AlertResult(NotificationEvent.Selection.Confirm),
            emitted.second
        )
    }
}

private class SearchStoreFixture(testScope: kotlinx.coroutines.test.TestScope) {
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

    private val coordinator = FakePluginRuntimeCoordinator(
        runtimes = runtimes,
        commands = commands,
        content = content,
        cachedItems = cachedItems
    )
    private val registry = FakePluginRuntimeRegistry(coordinator)

    val store = SearchStore(
        applicationScope = testScope,
        syncCacheUseCase = SyncCacheUseCase(searchRepository, registry),
        loadRuntimesUseCase = LoadRuntimesUseCase(
            pluginRepository = EmptyPluginRepository(),
            pluginRuntimeRegistry = registry,
            pluginLoader = EmptyPluginLoader()
        ),
        searchUseCase = SearchUseCase(registry, searchRepository, FakeSearchResultRanker()),
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
        updateCommandQueryUseCase = UpdateCommandQueryUseCase(registry)
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
    ): List<PluginCommandListAction> = emptyList()

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

    override suspend fun update(mutations: List<SearchIndexMutation>) = Unit

    override suspend fun updateUsage(resultId: SearchResultId) {
        usageUpdates += resultId
    }

    override fun search(query: String, limit: Int): Flow<List<RankedSearchResult>> = results
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
        error("Not used in SearchStore tests")
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
