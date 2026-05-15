import Foundation
import RaydroidShared

typealias SearchUiState = SearchScreenState
typealias ActionUiModel = FocusedCommandAction

enum PluginAsset: Equatable {
    case remoteURL(String)
    case base64(String)
    case binary(Data)
    case builtinName(String)
}

enum PluginFormValueDraft: Equatable {
    case text(String)
    case boolean(Bool)
    case date(String?)
}

final class SearchStoreObservation {
    private let cancelBlock: () -> Void

    init(cancelBlock: @escaping () -> Void) {
        self.cancelBlock = cancelBlock
    }

    func cancel() {
        cancelBlock()
    }
}

private final class SearchStoreStateCollector: NSObject, Kotlinx_coroutines_coreFlowCollector {
    private let onState: @MainActor (SearchScreenState) -> Void

    init(onState: @escaping @MainActor (SearchScreenState) -> Void) {
        self.onState = onState
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let state = value as? SearchScreenState {
            Task { @MainActor in
                onState(state)
            }
        }
        completionHandler(nil)
    }
}

@MainActor
protocol SearchStoreClient {
    var currentState: SearchUiState { get }

    func start()
    func stop()
    func watch(_ observer: @escaping (SearchUiState) -> Void) -> SearchStoreObservation
    func openSearch(query: String)
    func openCommand(commandId: String)
    func updateRootQuery(_ query: String)
    func updateFullscreenQuery(_ query: String)
    func submit()
    func submitForm(callback: ApiPluginFormSubmitCallback, values: [String: PluginFormValueDraft])
    func closeFullscreen()
    func toggleActions()
    func hideActions()
    func backspaceOnEmpty()
    func enter(_ resultId: ApiSearchResultId?)
    func enterAction(_ action: ActionUiModel)
    func enterCallback(resultId: ApiSearchResultId, callback: ApiPluginCommandCallback, updateUsage: Bool)
    func focusPluginItem(_ itemId: Any)
    func enterPluginItem(_ itemId: Any)
    func showContextActions(resultId: ApiSearchResultId, actions: [ApiPluginCommandListAction])
    func dismissAlert(_ alert: ApiNotificationEventAlert)
    func confirmAlert(_ alert: ApiNotificationEventAlert)
    func hideToast(_ toastId: String)
    func resolveText(_ text: ApiPluginUiText?) -> String
    func resolveIcon(_ icon: ApiPluginIcon?) -> PluginAsset?
    func resolveImage(_ image: ApiPluginImage?) -> PluginAsset?
}

@MainActor
final class KmpSearchStoreClient: SearchStoreClient {
    private let store: SearchStore
    private var watchTask: Task<Void, Never>?

    init(store: SearchStore = RaydroidBootstrapKt.CreateSearchStore()) {
        self.store = store
    }

    var currentState: SearchUiState {
        store.currentState()
    }

    func start() {
        store.start()
    }

    func stop() {
        watchTask?.cancel()
        watchTask = nil
    }

    func watch(_ observer: @escaping (SearchUiState) -> Void) -> SearchStoreObservation {
        watchTask?.cancel()
        observer(store.currentState())
        let collector = SearchStoreStateCollector(onState: observer)
        let flow = store.state
        watchTask = Task {
            await withCheckedContinuation { continuation in
                flow.collect(collector: collector) { _ in
                    continuation.resume()
                }
            }
        }
        return SearchStoreObservation { [weak self] in
            self?.watchTask?.cancel()
            self?.watchTask = nil
        }
    }

    func openSearch(query: String) {
        store.openSearch(query: query)
    }

    func openCommand(commandId: String) {
        store.openCommand(query: commandId)
    }

    func updateRootQuery(_ query: String) {
        store.currentState().eventSink(SearchScreenEventQueryChanged(query: query))
    }

    func updateFullscreenQuery(_ query: String) {
        store.currentState().eventSink(SearchScreenEventQueryChanged(query: query))
    }

    func submit() {
        store.currentState().eventSink(SearchScreenEventEnter(resultId: nil))
    }

    func submitForm(callback: ApiPluginFormSubmitCallback, values: [String: PluginFormValueDraft]) {
        let builder = PluginFormValuesBuilder()
        for (key, value) in values {
            switch value {
            case .text(let text):
                builder.putText(key: key, value: text)
            case .boolean(let value):
                builder.putBoolean(key: key, value: value)
            case .date(let value):
                builder.putDate(key: key, value: value)
            }
        }
        callback.invoke(values: builder.build()) { _ in }
    }

    func closeFullscreen() {
        store.currentState().eventSink(SearchScreenEventCloseFullscreen.shared)
    }

    func toggleActions() {
        store.toggleActions()
    }

    func hideActions() {
        store.hideActions()
    }

    func backspaceOnEmpty() {
        store.currentState().eventSink(SearchScreenEventBackspaceOnEmpty.shared)
    }

    func enter(_ resultId: ApiSearchResultId?) {
        store.currentState().eventSink(SearchScreenEventEnter(resultId: resultId))
    }

    func enterAction(_ action: ActionUiModel) {
        store.currentState().eventSink(SearchScreenEventEnterAction(action: action))
    }

    func enterCallback(resultId: ApiSearchResultId, callback: ApiPluginCommandCallback, updateUsage: Bool = false) {
        store.currentState().eventSink(
            SearchScreenEventEnterCallback(
                resultId: resultId,
                callback: callback,
                updateUsage: updateUsage
            )
        )
    }

    func focusPluginItem(_ itemId: Any) {
        store.currentState().eventSink(SearchScreenEventFocusPluginItem(itemId: itemId))
    }

    func enterPluginItem(_ itemId: Any) {
        store.currentState().eventSink(SearchScreenEventEnterPluginItem(itemId: itemId))
    }

    func showContextActions(resultId: ApiSearchResultId, actions: [ApiPluginCommandListAction]) {
        store.currentState().eventSink(
            SearchScreenEventShowContextActions(
                resultId: resultId,
                actions: actions
            )
        )
    }

    func dismissAlert(_ alert: ApiNotificationEventAlert) {
        store.currentState().eventSink(SearchScreenEventDismissAlert(alert: alert))
    }

    func confirmAlert(_ alert: ApiNotificationEventAlert) {
        store.currentState().eventSink(SearchScreenEventConfirmAlert(alert: alert))
    }

    func hideToast(_ toastId: String) {
        guard let toast = store.currentState().toasts.first(where: { $0.toastId == toastId }) else {
            return
        }
        store.currentState().eventSink(SearchScreenEventHideToast(toast: toast))
    }

    func resolveText(_ text: ApiPluginUiText?) -> String {
        guard let text else { return "" }
        return resolver.resolveText(text: text)
    }

    func resolveIcon(_ icon: ApiPluginIcon?) -> PluginAsset? {
        guard let icon, let asset = resolver.resolveIcon(icon: icon) else { return nil }
        return makeAsset(from: asset)
    }

    func resolveImage(_ image: ApiPluginImage?) -> PluginAsset? {
        guard let image, let asset = resolver.resolveImage(image: image) else { return nil }
        return makeAsset(from: asset)
    }

    private var resolver: PluginResourceResolver {
        PluginResourceResolver(
            plugins: store.currentState().plugins,
            language: Locale.current.language.languageCode?.identifier ?? "en"
        )
    }

    private func makeAsset(from asset: ResolvedPluginAsset) -> PluginAsset? {
        switch asset {
        case let remote as ResolvedPluginAssetRemoteUrl:
            return .remoteURL(remote.url)
        case let base64 as ResolvedPluginAssetBase64Data:
            return .base64(base64.base64)
        case let binary as ResolvedPluginAssetBinaryData:
            return .binary(Data(binary.bytes))
        case let builtin as ResolvedPluginAssetBuiltinName:
            return .builtinName(builtin.name)
        default:
            return nil
        }
    }
}

private extension Data {
    init(_ bytes: KotlinByteArray) {
        var raw = [UInt8](repeating: 0, count: Int(bytes.size))
        for index in 0..<Int(bytes.size) {
            raw[index] = UInt8(bitPattern: bytes.get(index: Int32(index)))
        }
        self.init(raw)
    }
}
