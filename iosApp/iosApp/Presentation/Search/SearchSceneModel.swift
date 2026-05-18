import Foundation
import Observation
import RaydroidShared

typealias SearchQueryState = SearchFieldUiState
typealias SearchSceneViewState = SearchSceneState

enum SearchResultsBodyState {
    case loading
    case empty
    case results([SearchResultRowModel])
}

struct SearchSceneState {
    var query: String
    var placeholder: String
    var selectionName: String
    var resultsBody: SearchResultsBodyState
    var fullscreen: FullscreenModel?
    var loadingStatusMessage: String?
    var actions: [OverlayActionModel]
    var contextActions: [OverlayActionModel]
    var actionTitle: String?
    var showsActionToggle: Bool
    var showsActionsPanel: Bool
    var showsContextMenu: Bool
    var activeContextSourceId: String?
    var showsBackButton: Bool
    var exitBackspaceCount: Int
    var toasts: [ToastModel]
    var alert: AlertModel?
    var aliasEditor: AliasEditorSheetModel?

    static let empty = SearchSceneState(
        query: "",
        placeholder: "Search commands",
        selectionName: "cursorAtEnd",
        resultsBody: .empty,
        fullscreen: nil,
        loadingStatusMessage: nil,
        actions: [],
        contextActions: [],
        actionTitle: nil,
        showsActionToggle: false,
        showsActionsPanel: false,
        showsContextMenu: false,
        activeContextSourceId: nil,
        showsBackButton: false,
        exitBackspaceCount: 0,
        toasts: [],
        alert: nil,
        aliasEditor: nil
    )
}

struct SearchResultRowModel: Identifiable {
    let id: String
    let iconAsset: PluginAsset?
    let title: String
    let alias: String?
    let subtitle: String
    let trailingText: String
    let showsListEntry: Bool
    let titleMatches: [HighlightMatch]
    let subtitleMatches: [HighlightMatch]
    let detailNodes: [PluginNodeViewData]
    let isFocused: Bool
    let contextSourceId: String
    let isContextMenuPresented: Bool
    let isContextMenuActive: Bool
    let onSelect: () -> Void
    let onLongPress: () -> Void
}

struct FullscreenModel {
    let nodes: [PluginNodeViewData]
}

struct OverlayActionModel: Identifiable {
    let id: String
    let title: String
    let iconAsset: PluginAsset?
    let style: PluginActionStyle
    let onSelect: () -> Void
}

struct AliasEditorSheetModel: Identifiable {
    let id: String
    let title: String
    let subjectTitle: String?
    let input: String
    let error: String?
    let showsRemove: Bool
    let onInputChanged: (String) -> Void
    let onSave: () -> Void
    let onRemove: (() -> Void)?
    let onDismiss: () -> Void
}

enum ToastStyle {
    case success
    case failure
    case info
}

struct ToastModel: Identifiable {
    let id: String
    let message: String
    let style: ToastStyle
    let onDismiss: () -> Void
}

enum AlertButtonRole {
    case normal
    case cancel
    case destructive
}

struct AlertDismissButtonModel {
    let title: String
    let role: AlertButtonRole
    let action: () -> Void
}

struct AlertModel: Identifiable {
    let id: String
    let title: String
    let message: String
    let confirmTitle: String
    let onConfirm: () -> Void
    let dismissButton: AlertDismissButtonModel?
}

enum SearchSceneEvent {
    case appear
    case disappear
    case route(AppRoute?)
    case queryChanged(String, String)
    case submit
    case closeFullscreen
    case backspaceOnEmpty
    case toggleActions
    case moveFocusNext
    case moveFocusPrevious
    case hideActions
    case dismissAlert
    case confirmAlert
    case dismissToast(String)
}

@MainActor
@Observable
final class SearchSceneViewModel {
    var state: SearchSceneState

    private let viewModelClient: SearchViewModelClient
    private let router: AppRouter
    private var rawState: SearchScreenState
    private var observation: SearchViewModelObservation?
    private var hasStarted = false

    init(viewModelClient: SearchViewModelClient, router: AppRouter) {
        self.viewModelClient = viewModelClient
        self.router = router
        self.rawState = viewModelClient.currentState
        self.state = SearchSceneState.empty
        self.state = makeMapper().map(state: rawState)
    }

    func send(_ event: SearchSceneEvent) {
        switch event {
        case .appear:
            start()
            applyRoute(router.pendingRoute)
        case .disappear:
            stop()
        case .route(let route):
            applyRoute(route)
        case .queryChanged(let query, let selectionName):
            viewModelClient.updateQuery(query, selectionName: selectionName)
        case .submit:
            viewModelClient.submit(resultId: nil)
        case .closeFullscreen:
            viewModelClient.closeFullscreen()
        case .backspaceOnEmpty:
            viewModelClient.backspaceOnEmpty()
        case .toggleActions:
            viewModelClient.toggleActions()
        case .moveFocusNext:
            viewModelClient.moveFocusNext()
        case .moveFocusPrevious:
            viewModelClient.moveFocusPrevious()
        case .hideActions:
            viewModelClient.hideActions()
        case .dismissAlert:
            dismissAlert()
        case .confirmAlert:
            confirmAlert()
        case .dismissToast(let toastId):
            viewModelClient.hideToast(toastId)
        }
    }

    func dismissAlert() {
        guard let alert = rawState.alerts.first else { return }
        viewModelClient.dismissAlert(alert)
    }

    func confirmAlert() {
        guard let alert = rawState.alerts.first else { return }
        viewModelClient.confirmAlert(alert)
    }

    private func start() {
        guard !hasStarted else { return }
        hasStarted = true
        viewModelClient.start()
        observation = viewModelClient.watch { [weak self] updated in
            self?.handleStateUpdate(updated)
        }
    }

    private func stop() {
        observation?.cancel()
        observation = nil
        hasStarted = false
        viewModelClient.stop()
    }

    private func applyRoute(_ route: AppRoute?) {
        guard let route else { return }
        switch route {
        case .search(let query):
            viewModelClient.openSearch(query: query)
        case .command(let commandId):
            viewModelClient.openCommand(commandId: commandId)
        }
        router.clearPendingRoute()
    }

    private func handleStateUpdate(_ updated: SearchScreenState) {
        rawState = updated
        state = makeMapper().map(state: updated)
    }

    private func makeMapper() -> SearchSceneStateMapper {
        SearchSceneStateMapper(client: viewModelClient)
    }
}

typealias SearchSceneModel = SearchSceneViewModel

@MainActor
private struct SearchSceneStateMapper {
    let client: SearchViewModelClient

    func map(state: SearchScreenState) -> SearchSceneState {
        let loadingToast = state.toasts.last(where: isAnimatedLoadingToast)
        let fullscreenNodes = state.fullscreenContent?.content.map { $0 as AnyObject } ?? []
        let fullscreenFocusedActions = state.fullscreenContent.map { fullscreen in
            focusedPluginActions(
                fullscreenNodes,
                focusedItemId: fullscreen.focusedItemId,
                query: fullscreen.searchFieldState.query
            )
        } ?? []
        let suppressHostActions = state.fullscreenContent != nil && contentSuppressesHostActions(fullscreenNodes)
        let actionHintMode = state.fullscreenContent.map { _ in
            contentActionPanelHintMode(fullscreenNodes)
        } ?? .full
        let dockActions = resolveDockActions(
            state: state,
            fullscreenFocusedActions: fullscreenFocusedActions,
            suppressHostActions: suppressHostActions
        )
        let contextActions = resolveContextActions(
            state: state,
            fullscreenFocusedActions: fullscreenFocusedActions,
            suppressHostActions: suppressHostActions
        )
        let alertModel = state.alerts.first.map { alert in
            let dismissButton = alert.dismissAction.map { dismissAction in
                AlertDismissButtonModel(
                    title: client.resolveText(dismissAction.title),
                    role: alertRole(dismissAction.style),
                    action: {
                        client.dismissAlert(alert)
                    }
                )
            }
            return AlertModel(
                id: "\(ObjectIdentifier(alert))",
                title: client.resolveText(alert.title),
                message: client.resolveText(alert.message),
                confirmTitle: client.resolveText(alert.confirmAction.title),
                onConfirm: {
                    client.confirmAlert(alert)
                },
                dismissButton: dismissButton
            )
        }
        let aliasEditorModel = mapAliasEditor(state.overlayState.aliasEditor)
        let fullscreen = state.fullscreenContent.map { fullscreen in
            FullscreenModel(
                nodes: PluginNodeMapper(
                    client: client,
                    query: fullscreen.searchFieldState.query,
                    resultId: fullscreen.resultId,
                    focusedItemId: fullscreen.focusedItemId,
                    activeContextSourceId: state.overlayState.activeContextSourceId,
                    isContextMenuPresented: state.overlayState.showContextActions
                ).mapNodes(
                    fullscreen.content.map { $0 as AnyObject },
                    path: "fullscreen"
                )
            )
        }

        return SearchSceneState(
            query: activeQuery(state: state),
            placeholder: placeholder(state: state),
            selectionName: selectionName(activeSearchFieldState(state: state).selection),
            resultsBody: mapResultsBody(state: state),
            fullscreen: fullscreen,
            loadingStatusMessage: loadingToast.map { toast in
                client.resolveText(toast.toast.message)
            },
            actions: dockActions,
            contextActions: contextActions,
            actionTitle: actionHintMode == .full ? primaryActionTitle(actions: dockActions) : nil,
            showsActionToggle: actionHintMode != .hidden && !dockActions.isEmpty,
            showsActionsPanel: state.overlayState.showActions,
            showsContextMenu: state.overlayState.showContextActions,
            activeContextSourceId: state.overlayState.activeContextSourceId,
            showsBackButton: state.fullscreenContent != nil,
            exitBackspaceCount: Int(state.fullscreenContent?.exitBackspaceCount ?? 0),
            toasts: state.toasts
                .filter { toast in !isAnimatedLoadingToast(toast) }
                .map { toast in
                ToastModel(
                    id: toast.toastId,
                    message: client.resolveText(toast.toast.message),
                    style: toastStyle(toast),
                    onDismiss: {
                        client.hideToast(toast.toastId)
                    }
                )
            },
            alert: alertModel,
            aliasEditor: aliasEditorModel
        )
    }

    private func resolveDockActions(
        state: SearchScreenState,
        fullscreenFocusedActions: [ApiPluginCommandListAction],
        suppressHostActions: Bool
    ) -> [OverlayActionModel] {
        if suppressHostActions {
            return []
        }
        if let fullscreen = state.fullscreenContent, !fullscreenFocusedActions.isEmpty {
            return fullscreenFocusedActions.enumerated().map { index, action in
                overlayAction(
                    id: "fullscreen.action.\(index).\(fullscreen.resultId.commandName)",
                    action: action
                ) {
                    client.enterCallback(
                        resultId: fullscreen.resultId,
                        callback: action.callback,
                        updateUsage: false
                    )
                }
            }
        }
        return state.overlayState.focusedActions.enumerated().map { index, action in
            overlayAction(
                id: "action.\(index).\(action.resultId.commandName)",
                action: action.action
            ) {
                client.enterAction(action)
            }
        }
    }

    private func resolveContextActions(
        state: SearchScreenState,
        fullscreenFocusedActions: [ApiPluginCommandListAction],
        suppressHostActions: Bool
    ) -> [OverlayActionModel] {
        if state.overlayState.showContextActions {
            return state.overlayState.contextActions.enumerated().map { index, action in
                overlayAction(
                    id: "context.action.\(index).\(action.resultId.commandName)",
                    action: action.action
                ) {
                    client.enterAction(action)
                }
            }
        }
        return []
    }

    private func mapAliasEditor(_ aliasEditor: SearchAliasEditorState?) -> AliasEditorSheetModel? {
        guard let aliasEditor else { return nil }
        let id = "alias-editor:\(aliasEditor.resultId.commandName)"
        let subjectTitle = aliasEditor.title.map { client.resolveText($0) }
        let error = aliasEditor.error.map { client.resolveText($0) }
        let showsRemove = aliasEditor.existingAlias != nil
        return AliasEditorSheetModel(
            id: id,
            title: showsRemove ? "Edit Alias" : "Add Alias",
            subjectTitle: subjectTitle,
            input: aliasEditor.input,
            error: error,
            showsRemove: showsRemove,
            onInputChanged: { value in
                client.updateAliasEditorInput(value)
            },
            onSave: {
                client.saveAliasEditor()
            },
            onRemove: showsRemove ? {
                client.removeAlias()
            } : nil,
            onDismiss: {
                client.dismissAliasEditor()
            }
        )
    }

    private func isAnimatedLoadingToast(_ toast: ApiNotificationEventShowToast) -> Bool {
        enumName(toast.toast.style).lowercased() == "animated"
    }

    private func mapResultsBody(state: SearchScreenState) -> SearchResultsBodyState {
        guard state.fullscreenContent == nil else { return .empty }
        if let results = state.resultsContent?.searchResults?.results {
            return .results(
                results.enumerated().map { index, result in
                    mapResult(
                        result,
                        index: index,
                        query: state.searchFieldState.query,
                        focusedIndex: state.resultsContent?.focusedItemIndex?.intValue,
                        isContextMenuPresented: state.overlayState.showContextActions,
                        activeContextSourceId: state.overlayState.activeContextSourceId
                    )
                }
            )
        }
        if state.resultsContent?.isSearching == true && state.resultsContent?.searchResults == nil {
            return .loading
        }
        return .empty
    }

    private func mapResult(
        _ result: any ApiSearchResultSetSearchResult,
        index: Int,
        query: String,
        focusedIndex: Int?,
        isContextMenuPresented: Bool,
        activeContextSourceId: String?
    ) -> SearchResultRowModel {
        let liveContent = searchResultContent(result)
        let isLiveResult = SearchInteropBridge.shared.searchResultIsLive(result: result)
        let detailNodes: [PluginNodeViewData]
        if !liveContent.isEmpty {
            detailNodes = PluginNodeMapper(
                client: client,
                query: query,
                resultId: result.resultId,
                focusedItemId: nil,
                activeContextSourceId: activeContextSourceId,
                isContextMenuPresented: isContextMenuPresented
            ).mapNodes(
                liveContent.map { $0 as AnyObject },
                path: "live.\(index)"
            )
        } else {
            detailNodes = []
        }

        let resolvedTitle = client.resolveText(result.listEntry.title)
        let resolvedSubtitle = client.resolveText(result.listEntry.description_)
        let resolvedTrailingText = client.resolveText(result.listEntry.trailingText)
        let alias = result.listEntry.alias
        let commandName = result.resultId.commandName
        let title: String
        let subtitle: String
        let trailingText: String

        if !isLiveResult {
            title = resolvedTitle.isEmpty ? fallbackTitle(for: commandName) : resolvedTitle
            subtitle = resolvedSubtitle.isEmpty ? fallbackSubtitle(for: commandName) : resolvedSubtitle
            trailingText = resolvedTrailingText
        } else {
            title = resolvedTitle
            subtitle = resolvedSubtitle
            trailingText = resolvedTrailingText
        }

        let iconAsset = client.resolveIcon(result.listEntry.icon) ?? (isLiveResult ? nil : fallbackIcon(for: commandName))
        let showsListEntry = iconAsset != nil || !title.isEmpty || !subtitle.isEmpty || !trailingText.isEmpty
        let contextSourceId = resultContextSourceId(result.resultId)
        let isContextMenuActive = activeContextSourceId == contextSourceId

        return SearchResultRowModel(
            id: "\(isLiveResult ? "live" : "result").\(index).\(result.resultId.commandName)",
            iconAsset: iconAsset,
            title: title,
            alias: alias,
            subtitle: subtitle,
            trailingText: trailingText,
            showsListEntry: showsListEntry,
            titleMatches: searchResultTitleMatches(result),
            subtitleMatches: searchResultDescriptionMatches(result),
            detailNodes: detailNodes,
            isFocused: focusedIndex == index,
            contextSourceId: contextSourceId,
            isContextMenuPresented: isContextMenuPresented,
            isContextMenuActive: isContextMenuActive,
            onSelect: {
                client.enter(result.resultId)
            },
            onLongPress: {
                client.showResultContextActions(
                    resultId: result.resultId,
                    sourceId: contextSourceId
                )
            }
        )
    }

    private func fallbackTitle(for commandName: String) -> String {
        switch commandName {
        case "CalculatorCommand":
            return "Calculator"
        default:
            return ""
        }
    }

    private func fallbackSubtitle(for commandName: String) -> String {
        switch commandName {
        case "CalculatorCommand":
            return "Type an expression"
        default:
            return ""
        }
    }

    private func fallbackIcon(for commandName: String) -> PluginAsset? {
        switch commandName {
        case "CalculatorCommand":
            return .builtinName("Calculate")
        default:
            return nil
        }
    }

    private func overlayAction(
        id: String,
        action: ApiPluginCommandListAction,
        onSelect: @escaping () -> Void
    ) -> OverlayActionModel {
        OverlayActionModel(
            id: id,
            title: client.resolveText(action.title),
            iconAsset: client.resolveIcon(action.icon),
            style: action.style == .destructive ? .destructive : .normal,
            onSelect: onSelect
        )
    }

    private func overlayAction(
        id: String,
        action: SearchPanelAction,
        onSelect: @escaping () -> Void
    ) -> OverlayActionModel {
        OverlayActionModel(
            id: id,
            title: client.resolveText(action.title),
            iconAsset: client.resolveIcon(action.icon),
            style: action.destructive ? .destructive : .normal,
            onSelect: onSelect
        )
    }

    private func activeQuery(state: SearchScreenState) -> String {
        if let fullscreen = state.fullscreenContent {
            return fullscreen.searchFieldState.query
        }
        return state.searchFieldState.query
    }

    private func placeholder(state: SearchScreenState) -> String {
        if let placeholder = state.fullscreenContent?.placeholder {
            return client.resolveText(placeholder)
        }
        return "Search commands"
    }

    private func activeSearchFieldState(state: SearchScreenState) -> SearchQueryState {
        state.fullscreenContent?.searchFieldState ?? state.searchFieldState
    }

    private func primaryActionTitle(actions: [OverlayActionModel]) -> String? {
        actions.first?.title
    }

    private func primaryActionTitle(actions: [ActionUiModel]) -> String? {
        guard let action = actions.first(where: \.action.primary) ?? actions.first else {
            return nil
        }
        return client.resolveText(action.action.title)
    }

    private func toastStyle(_ toast: ApiNotificationEventShowToast) -> ToastStyle {
        switch enumName(toast.toast.style).lowercased() {
        case "success":
            return .success
        case "failure":
            return .failure
        default:
            return .info
        }
    }

    private func alertRole(_ role: ApiNotificationEventAlertAction.Style?) -> AlertButtonRole {
        switch enumName(role).lowercased() {
        case "cancel":
            return .cancel
        case "destructive":
            return .destructive
        default:
            return .normal
        }
    }

    private func selectionName(_ selection: ApiSearchFieldSelection) -> String {
        switch selection {
        case .cursoratstart:
            return "cursorAtStart"
        case .selectall:
            return "selectAll"
        default:
            return "cursorAtEnd"
        }
    }
}

private func enumName(_ object: AnyObject?) -> String {
    guard let object else { return "" }
    if let enumObject = object as? KotlinEnum<AnyObject> {
        return enumObject.name
    }
    if let name = object.value(forKey: "name") as? String {
        return name
    }
    return String(describing: object)
}

private func focusedPluginActions(
    _ nodes: [AnyObject],
    focusedItemId: Any?,
    query: String
) -> [ApiPluginCommandListAction] {
    let apiNodes = nodes.compactMap { $0 as? ApiPluginRayNodeData }
    return SearchInteropBridge.shared.focusedActions(
        nodes: apiNodes,
        focusedItemValue: focusedItemIdValue(focusedItemId),
        query: query
    )
}

private func contentSuppressesHostActions(_ nodes: [AnyObject]) -> Bool {
    SearchInteropBridge.shared.suppressesHostActions(
        nodes: nodes.compactMap { $0 as? ApiPluginRayNodeData }
    )
}

private func contentActionPanelHintMode(_ nodes: [AnyObject]) -> ActionPanelHintModeModel {
    switch SearchInteropBridge.shared.actionPanelHintMode(
        nodes: nodes.compactMap { $0 as? ApiPluginRayNodeData }
    ).lowercased() {
    case "hidden":
        return .hidden
    case "menuonly":
        return .menuOnly
    default:
        return .full
    }
}

private func searchResultTitleMatches(_ result: any ApiSearchResultSetSearchResult) -> [HighlightMatch] {
    buildHighlightMatches(SearchInteropBridge.shared.searchResultTitleMatches(result: result))
}

private func searchResultDescriptionMatches(_ result: any ApiSearchResultSetSearchResult) -> [HighlightMatch] {
    buildHighlightMatches(SearchInteropBridge.shared.searchResultDescriptionMatches(result: result))
}

private func searchResultContent(_ result: any ApiSearchResultSetSearchResult) -> [ApiPluginRayNodeData] {
    SearchInteropBridge.shared.searchResultContent(result: result)
}

private func buildHighlightMatches(_ rawMatches: [NSNumber]) -> [HighlightMatch] {
    var matches: [HighlightMatch] = []
    var index = 0
    while index + 1 < rawMatches.count {
        matches.append(
            HighlightMatch(
                start: rawMatches[index].intValue,
                end: rawMatches[index + 1].intValue
            )
        )
        index += 2
    }
    return matches
}

private func resultContextSourceId(_ resultId: ApiSearchResultId) -> String {
    "search-result:\(pluginIdValue(resultId.pluginId)):\(resultId.commandName):\(focusedItemIdValue(resultId.itemId))"
}

private func pluginIdValue(_ value: Any?) -> String {
    if let raw = value as? String {
        return raw
    }
    if let raw = value as? NSString {
        return raw as String
    }
    guard let object = value as AnyObject? else { return "" }
    if object.responds(to: NSSelectorFromString("id")),
       let raw = object.value(forKey: "id") as? String {
        return raw
    }
    if object.responds(to: NSSelectorFromString("value")),
       let raw = object.value(forKey: "value") as? String {
        return raw
    }
    return String(describing: object)
}

private func focusedItemIdValue(_ value: Any?) -> String {
    if let raw = value as? String {
        return raw
    }
    if let raw = value as? NSString {
        return raw as String
    }
    guard let object = value as AnyObject? else { return "" }
    if object.responds(to: NSSelectorFromString("value")),
       let raw = object.value(forKey: "value") as? String {
        return raw
    }
    return String(describing: object)
}
