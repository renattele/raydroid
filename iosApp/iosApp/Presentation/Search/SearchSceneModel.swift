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
    var actionTitle: String?
    var showsActionToggle: Bool
    var showsActionsPanel: Bool
    var showsBackButton: Bool
    var exitBackspaceCount: Int
    var toasts: [ToastModel]
    var alert: AlertModel?

    static let empty = SearchSceneState(
        query: "",
        placeholder: "Search commands",
        selectionName: "cursorAtEnd",
        resultsBody: .empty,
        fullscreen: nil,
        loadingStatusMessage: nil,
        actions: [],
        actionTitle: nil,
        showsActionToggle: false,
        showsActionsPanel: false,
        showsBackButton: false,
        exitBackspaceCount: 0,
        toasts: [],
        alert: nil
    )
}

struct SearchResultRowModel: Identifiable {
    let id: String
    let iconAsset: PluginAsset?
    let title: String
    let subtitle: String
    let titleMatches: [HighlightMatch]
    let subtitleMatches: [HighlightMatch]
    let detailNodes: [PluginNodeViewData]
    let isFocused: Bool
    let onSelect: () -> Void
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

enum SearchSceneIntent {
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
    case dismissAlert
    case confirmAlert
    case dismissToast(String)
}

@MainActor
@Observable
final class SearchSceneViewModel {
    var state: SearchSceneState

    private let storeClient: SearchStoreClient
    private let router: AppRouter
    private var rawState: SearchUiState
    private var observation: SearchStoreObservation?
    private var hasStarted = false

    init(storeClient: SearchStoreClient, router: AppRouter) {
        self.storeClient = storeClient
        self.router = router
        self.rawState = storeClient.currentState
        self.state = SearchSceneState.empty
        self.state = makeMapper().map(state: rawState)
    }

    func send(_ intent: SearchSceneIntent) {
        switch intent {
        case .appear:
            start()
            applyRoute(router.pendingRoute)
        case .disappear:
            stop()
        case .route(let route):
            applyRoute(route)
        case .queryChanged(let query, let selectionName):
            storeClient.updateQuery(query, selectionName: selectionName)
        case .submit:
            storeClient.submit(resultId: nil)
        case .closeFullscreen:
            storeClient.closeFullscreen()
        case .backspaceOnEmpty:
            storeClient.backspaceOnEmpty()
        case .toggleActions:
            storeClient.toggleActions()
        case .moveFocusNext:
            storeClient.moveFocusNext()
        case .moveFocusPrevious:
            storeClient.moveFocusPrevious()
        case .dismissAlert:
            dismissAlert()
        case .confirmAlert:
            confirmAlert()
        case .dismissToast(let toastId):
            storeClient.hideToast(toastId)
        }
    }

    func dismissAlert() {
        guard let alert = rawState.alerts.first else { return }
        storeClient.dismissAlert(alert)
    }

    func confirmAlert() {
        guard let alert = rawState.alerts.first else { return }
        storeClient.confirmAlert(alert)
    }

    private func start() {
        guard !hasStarted else { return }
        hasStarted = true
        storeClient.start()
        observation = storeClient.watch { [weak self] updated in
            self?.handleStateUpdate(updated)
        }
    }

    private func stop() {
        observation?.cancel()
        observation = nil
        hasStarted = false
        storeClient.stop()
    }

    private func applyRoute(_ route: AppRoute?) {
        guard let route else { return }
        switch route {
        case .search(let query):
            storeClient.openSearch(query: query)
        case .command(let commandId):
            storeClient.openCommand(commandId: commandId)
        }
        router.clearPendingRoute()
    }

    private func handleStateUpdate(_ updated: SearchUiState) {
        rawState = updated
        state = makeMapper().map(state: updated)
    }

    private func makeMapper() -> SearchSceneStateMapper {
        SearchSceneStateMapper(client: storeClient)
    }
}

typealias SearchSceneModel = SearchSceneViewModel

@MainActor
private struct SearchSceneStateMapper {
    let client: SearchStoreClient

    func map(state: SearchUiState) -> SearchSceneState {
        let loadingToast = state.toasts.last(where: isAnimatedLoadingToast)
        let fullscreenNodes = state.fullscreen?.content.map { $0 as AnyObject } ?? []
        let fullscreenFocusedActions = state.fullscreen.map { fullscreen in
            focusedPluginActions(
                fullscreenNodes,
                focusedItemId: fullscreen.focusedItemId,
                query: fullscreen.searchFieldState.query
            )
        } ?? []
        let suppressHostActions = state.fullscreen != nil && contentSuppressesHostActions(fullscreenNodes)
        let actionHintMode = state.fullscreen.map { _ in
            contentActionPanelHintMode(fullscreenNodes)
        } ?? .full
        let dockActions = resolveDockActions(
            state: state,
            fullscreenFocusedActions: fullscreenFocusedActions,
            suppressHostActions: suppressHostActions
        )
        let overlayActions = resolveOverlayActions(
            state: state,
            fullscreenFocusedActions: fullscreenFocusedActions,
            suppressHostActions: suppressHostActions
        )
        let fullscreen = state.fullscreen.map { fullscreen in
            FullscreenModel(
                nodes: PluginNodeMapper(
                    client: client,
                    query: fullscreen.searchFieldState.query,
                    resultId: fullscreen.resultId,
                    focusedItemId: fullscreen.focusedItemId
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
            actions: overlayActions,
            actionTitle: actionHintMode == .full ? primaryActionTitle(actions: dockActions) : nil,
            showsActionToggle: actionHintMode != .hidden && !dockActions.isEmpty,
            showsActionsPanel: state.showActions || state.showContextActions,
            showsBackButton: state.fullscreen != nil,
            exitBackspaceCount: Int(state.fullscreen?.exitBackspaceCount ?? 0),
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
            alert: state.alerts.first.map { alert in
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
        )
    }

    private func resolveDockActions(
        state: SearchUiState,
        fullscreenFocusedActions: [ApiPluginCommandListAction],
        suppressHostActions: Bool
    ) -> [OverlayActionModel] {
        if suppressHostActions {
            return []
        }
        if let fullscreen = state.fullscreen, !fullscreenFocusedActions.isEmpty {
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
        return state.focusedActions.enumerated().map { index, action in
            overlayAction(
                id: "action.\(index).\(action.resultId.commandName)",
                action: action.action
            ) {
                client.enterAction(action)
            }
        }
    }

    private func resolveOverlayActions(
        state: SearchUiState,
        fullscreenFocusedActions: [ApiPluginCommandListAction],
        suppressHostActions: Bool
    ) -> [OverlayActionModel] {
        if state.showContextActions {
            return state.contextActions.enumerated().map { index, action in
                overlayAction(
                    id: "context.action.\(index).\(action.resultId.commandName)",
                    action: action.action
                ) {
                    client.enterAction(action)
                }
            }
        }
        return resolveDockActions(
            state: state,
            fullscreenFocusedActions: fullscreenFocusedActions,
            suppressHostActions: suppressHostActions
        )
    }

    private func isAnimatedLoadingToast(_ toast: ApiNotificationEventShowToast) -> Bool {
        enumName(toast.toast.style).lowercased() == "animated"
    }

    private func mapResultsBody(state: SearchUiState) -> SearchResultsBodyState {
        guard state.fullscreen == nil else { return .empty }
        if let results = state.searchResults?.results {
            return .results(
                results.enumerated().map { index, result in
                    mapResult(
                        result,
                        index: index,
                        query: state.searchFieldState.query,
                        focusedIndex: state.focusedItemIndex?.intValue
                    )
                }
            )
        }
        if state.isSearching {
            return .loading
        }
        return .empty
    }

    private func mapResult(
        _ result: any ApiSearchResultSetSearchResult,
        index: Int,
        query: String,
        focusedIndex: Int?
    ) -> SearchResultRowModel {
        let detailNodes: [PluginNodeViewData]
        let resultKind: String

        if let live = result as? ApiPluginRuntimeCoordinatorContentItem {
            resultKind = "live"
            detailNodes = PluginNodeMapper(
                client: client,
                query: query,
                resultId: live.resultId,
                focusedItemId: nil
            ).mapNodes(
                live.presentation.content.map { $0 as AnyObject },
                path: "live.\(index)"
            )
        } else {
            resultKind = "result"
            detailNodes = []
        }

        return SearchResultRowModel(
            id: "\(resultKind).\(index).\(result.resultId.commandName)",
            iconAsset: client.resolveIcon(result.listEntry.icon),
            title: client.resolveText(result.listEntry.title),
            subtitle: client.resolveText(result.listEntry.description_),
            titleMatches: searchResultTitleMatches(result),
            subtitleMatches: searchResultDescriptionMatches(result),
            detailNodes: detailNodes,
            isFocused: focusedIndex == index,
            onSelect: {
                client.enter(result.resultId)
            }
        )
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

    private func activeQuery(state: SearchUiState) -> String {
        if let fullscreen = state.fullscreen {
            return fullscreen.searchFieldState.query
        }
        return state.searchFieldState.query
    }

    private func placeholder(state: SearchUiState) -> String {
        if let placeholder = state.fullscreen?.placeholder {
            return client.resolveText(placeholder)
        }
        return "Search commands"
    }

    private func activeSearchFieldState(state: SearchUiState) -> SearchQueryState {
        state.fullscreen?.searchFieldState ?? state.searchFieldState
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
