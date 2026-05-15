import Foundation
import Observation
import RaydroidShared

typealias SearchQueryState = SearchFieldState

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
    var actions: [OverlayActionModel]
    var actionTitle: String?
    var showsActionsPanel: Bool
    var showsBackButton: Bool
    var exitBackspaceCount: Int
    var toasts: [ToastModel]
    var alert: AlertModel?

    static let empty = SearchSceneState(
        query: "",
        placeholder: "Search commands",
        selectionName: "",
        resultsBody: .empty,
        fullscreen: nil,
        actions: [],
        actionTitle: nil,
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

@MainActor
@Observable
final class SearchSceneModel {
    var state: SearchSceneState

    private let storeClient: SearchStoreClient
    private var rawState: SearchUiState
    private var observation: SearchStoreObservation?

    init(storeClient: SearchStoreClient) {
        self.storeClient = storeClient
        self.rawState = storeClient.currentState
        self.state = SearchSceneState.empty
        self.state = makeMapper().map(state: rawState)
    }

    func start() {
        storeClient.start()
        observation = storeClient.watch { [weak self] updated in
            self?.handleStateUpdate(updated)
        }
    }

    func stop() {
        observation?.cancel()
        observation = nil
        storeClient.stop()
    }

    func updateQuery(_ query: String) {
        if rawState.fullscreen != nil {
            storeClient.updateFullscreenQuery(query)
        } else {
            storeClient.updateRootQuery(query)
        }
    }

    func submit() {
        storeClient.submit()
    }

    func closeFullscreen() {
        storeClient.closeFullscreen()
    }

    func backspaceOnEmpty() {
        storeClient.backspaceOnEmpty()
    }

    func toggleActions() {
        storeClient.toggleActions()
    }

    func dismissAlert() {
        guard let alert = rawState.alerts.first else { return }
        storeClient.dismissAlert(alert)
    }

    func confirmAlert() {
        guard let alert = rawState.alerts.first else { return }
        storeClient.confirmAlert(alert)
    }

    func hideToast(_ toastId: String) {
        storeClient.hideToast(toastId)
    }

    func applyRoute(_ route: AppRoute?, router: AppRouter) {
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

@MainActor
private struct SearchSceneStateMapper {
    let client: SearchStoreClient

    func map(state: SearchUiState) -> SearchSceneState {
        let activeActions = state.showContextActions ? state.contextActions : state.focusedActions
        let fullscreen = state.fullscreen.map { fullscreen in
            FullscreenModel(
                nodes: PluginNodeMapper(
                    client: client,
                    query: fullscreen.searchFieldState.text,
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
            selectionName: enumName(activeSearchFieldState(state: state).selection),
            resultsBody: mapResultsBody(state: state),
            fullscreen: fullscreen,
            actions: activeActions.enumerated().map { index, action in
                OverlayActionModel(
                    id: "action.\(index).\(action.resultId.commandName)",
                    title: client.resolveText(action.action.title),
                    iconAsset: client.resolveIcon(action.action.icon),
                    style: action.action.style == .destructive ? .destructive : .normal,
                    onSelect: {
                        client.enterAction(action)
                    }
                )
            },
            actionTitle: primaryActionTitle(actions: activeActions),
            showsActionsPanel: state.showActions || state.showContextActions,
            showsBackButton: state.fullscreen != nil,
            exitBackspaceCount: Int(state.fullscreen?.exitBackspaceCount ?? 0),
            toasts: state.toasts.map { toast in
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

    private func mapResultsBody(state: SearchUiState) -> SearchResultsBodyState {
        guard state.fullscreen == nil else { return .empty }
        if let results = state.searchResults?.results {
            return .results(
                results.enumerated().map { index, result in
                    mapResult(
                        result,
                        index: index,
                        query: state.searchFieldState.text,
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
            titleMatches: [],
            subtitleMatches: [],
            detailNodes: detailNodes,
            isFocused: focusedIndex == index,
            onSelect: {
                client.enter(result.resultId)
            }
        )
    }

    private func activeQuery(state: SearchUiState) -> String {
        if let fullscreen = state.fullscreen {
            return fullscreen.searchFieldState.text
        }
        return state.searchFieldState.text
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
