import Foundation
import RaydroidShared
import SwiftUI

enum PreviewRuntime {
    static var isActive: Bool {
        let environment = ProcessInfo.processInfo.environment
        return environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
            || environment["XCODE_RUNNING_FOR_PLAYGROUNDS"] == "1"
    }
}

@MainActor
final class PreviewSearchViewModelClient: SearchViewModelClient {
    var currentState: SearchScreenState {
        if PreviewRuntime.isActive {
            RaydroidBootstrapKt.InitKoin()
        }
        return RaydroidBootstrapKt.CreateSearchViewModel().currentState()
    }

    func start() {}
    func stop() {}

    func watch(_ observer: @escaping (SearchScreenState) -> Void) -> SearchViewModelObservation {
        observer(currentState)
        return SearchViewModelObservation(cancelBlock: {})
    }

    func updateQuery(_ query: String, selectionName: String) {}
    func openSearch(query: String) {}
    func openCommand(commandId: String) {}
    func submit(resultId: ApiSearchResultId?) {}
    func submitForm(callback: ApiPluginFormSubmitCallback, values: [String : PluginFormValueDraft]) {}
    func closeFullscreen() {}
    func toggleActions() {}
    func hideActions() {}
    func backspaceOnEmpty() {}
    func moveFocusNext() {}
    func moveFocusPrevious() {}
    func enter(_ resultId: ApiSearchResultId?) {}
    func enterAction(_ action: ActionUiModel) {}
    func enterCallback(resultId: ApiSearchResultId, callback: ApiPluginCommandCallback, updateUsage: Bool) {}
    func focusPluginItem(_ itemId: Any) {}
    func enterPluginItem(_ itemId: Any) {}
    func showContextActions(resultId: ApiSearchResultId, actions: [ApiPluginCommandListAction]) {}
    func dismissAlert(_ alert: ApiNotificationEventAlert) {}
    func confirmAlert(_ alert: ApiNotificationEventAlert) {}
    func hideToast(_ toastId: String) {}
    func resolveText(_ text: ApiPluginUiText?) -> String { "" }
    func resolveIcon(_ icon: ApiPluginIcon?) -> PluginAsset? { nil }
    func resolveImage(_ image: ApiPluginImage?) -> PluginAsset? { nil }
}

struct PreviewSpotlightIndexing: SpotlightIndexing {
    func indexCommands() async {}
}

@MainActor
enum PreviewData {
    static let pluginNodes: [PluginNodeViewData] = [
        .text(
            TextNodeViewData(
                id: "text",
                text: "Calculator result",
                fontName: "large",
                colorName: "primary"
            )
        ),
        .orientedBox(
            OrientedBoxNodeViewData(
                id: "row",
                orientationName: "horizontal",
                spacingName: "small",
                children: [
                    .icon(
                        IconNodeViewData(
                            id: "row.icon",
                            asset: .builtinName("Calculate"),
                            tintName: "primary",
                            sizeName: "large"
                        )
                    ),
                    .text(
                        TextNodeViewData(
                            id: "row.text",
                            text: "2 + 2 = 4",
                            fontName: "medium",
                            colorName: "onsurface"
                        )
                    )
                ]
            )
        ),
        .detail(
            DetailNodeViewData(
                id: "detail",
                title: "Command Details",
                markdown: "Use **Raydroid** to run plugin commands with rich output.",
                metadata: [
                    .label(
                        LabelMetadataViewData(
                            id: "detail.label",
                            title: "Category",
                            text: "Utilities",
                            iconAsset: .builtinName("GridView")
                        )
                    ),
                    .tagList(
                        TagListMetadataViewData(
                            id: "detail.tags",
                            title: "Tags",
                            tags: [
                                TagChipViewData(id: "tag.one", text: "plugins"),
                                TagChipViewData(id: "tag.two", text: "search"),
                                TagChipViewData(id: "tag.three", text: "ios")
                            ]
                        )
                    ),
                    .link(
                        LinkMetadataViewData(
                            id: "detail.link",
                            title: "Docs",
                            text: "Open reference",
                            target: "https://example.com"
                        )
                    )
                ]
            )
        ),
        .list(sampleListNode),
        .grid(sampleGridNode),
        .form(sampleFormNode)
    ]

    static let sampleListNode = ListNodeViewData(
        id: "list",
        isLoading: false,
        emptyState: nil,
        sections: [
            PluginListSectionViewData(
                id: "list.section",
                title: "Recent Commands",
                items: [
                    PluginListItemViewData(
                        id: "list.item.one",
                        title: "Calculator",
                        subtitle: "Evaluate quick expressions",
                        iconAsset: .builtinName("Calculate"),
                        content: [
                            .text(
                                TextNodeViewData(
                                    id: "list.item.one.note",
                                    text: "Tap to open fullscreen output.",
                                    fontName: "small",
                                    colorName: "secondary"
                                )
                            )
                        ],
                        isFocused: true,
                        showsContextButton: true,
                        onTap: {},
                        onShowContextActions: {}
                    ),
                    PluginListItemViewData(
                        id: "list.item.two",
                        title: "Clipboard",
                        subtitle: "Read and write clipboard content",
                        iconAsset: .builtinName("Clear"),
                        content: [],
                        isFocused: false,
                        showsContextButton: false,
                        onTap: {},
                        onShowContextActions: nil
                    )
                ]
            )
        ]
    )

    static let sampleGridNode = GridNodeViewData(
        id: "grid",
        isLoading: false,
        emptyState: nil,
        columnCount: 2,
        aspectRatioName: "sixteentonine",
        sections: [
            PluginGridSectionViewData(
                id: "grid.section",
                title: "Visual Commands",
                items: [
                    PluginGridItemViewData(
                        id: "grid.one",
                        title: "Launcher",
                        subtitle: "Browse installed apps",
                        imageAsset: .builtinName("GridView"),
                        isFocused: true,
                        onTap: {},
                        onShowContextActions: {}
                    ),
                    PluginGridItemViewData(
                        id: "grid.two",
                        title: "Settings",
                        subtitle: "Adjust preferences",
                        imageAsset: .builtinName("Tune"),
                        isFocused: false,
                        onTap: {},
                        onShowContextActions: nil
                    )
                ]
            )
        ]
    )

    static let sampleFormNode = FormNodeViewData(
        id: "form",
        title: "Connect Plugin",
        isLoading: false,
        fields: [
            .text(
                TextFormFieldViewData(
                    id: "server",
                    title: "Server",
                    placeholder: "https://api.example.com",
                    defaultValue: "",
                    isRequired: true,
                    isPassword: false,
                    isMultiline: false
                )
            ),
            .text(
                TextFormFieldViewData(
                    id: "token",
                    title: "Token",
                    placeholder: "API token",
                    defaultValue: "",
                    isRequired: true,
                    isPassword: true,
                    isMultiline: false
                )
            ),
            .checkbox(
                CheckboxFormFieldViewData(
                    id: "remember",
                    title: "Remember credentials",
                    isRequired: false,
                    defaultValue: true
                )
            ),
            .dropdown(
                DropdownFormFieldViewData(
                    id: "environment",
                    title: "Environment",
                    options: [
                        DropdownOptionViewData(
                            id: "env.dev",
                            value: "dev",
                            title: "Development",
                            iconAsset: .builtinName("Tune")
                        ),
                        DropdownOptionViewData(
                            id: "env.prod",
                            value: "prod",
                            title: "Production",
                            iconAsset: .builtinName("GridView")
                        )
                    ],
                    isRequired: false,
                    defaultValue: "dev"
                )
            ),
            .date(
                DateFormFieldViewData(
                    id: "expiry",
                    title: "Expiry",
                    isRequired: false,
                    defaultValue: "2026-05-08"
                )
            ),
            .description(
                DescriptionFormFieldViewData(
                    id: "description",
                    text: "Credentials stay on device and sync through the plugin host."
                )
            )
        ],
        submitTitle: "Connect",
        submitIconAsset: .builtinName("Save"),
        submitStyle: .filled,
        isSubmitEnabledByPlugin: true,
        requireChanges: true,
        unchangedView: EmptyStateViewData(
            id: "form.unchanged",
            iconAsset: .builtinName("Edit"),
            title: "No changes yet",
            description: "Update any field to enable the action."
        ),
        onSubmit: { _ in }
    )

    static let searchResultsBody: SearchResultsBodyState = .results([
        SearchResultRowModel(
            id: "result.one",
            iconAsset: .builtinName("Calculate"),
            title: "Calculator",
            subtitle: "Evaluate expressions and inspect structured output",
            titleMatches: [HighlightMatch(start: 0, end: 3)],
            subtitleMatches: [],
            detailNodes: [
                .text(
                    TextNodeViewData(
                        id: "result.one.detail",
                        text: "Inline live content preview.",
                        fontName: "small",
                        colorName: "secondary"
                    )
                )
            ],
            isFocused: true,
            onSelect: {}
        ),
        SearchResultRowModel(
            id: "result.two",
            iconAsset: .builtinName("GridView"),
            title: "Apps",
            subtitle: "Search installed applications",
            titleMatches: [],
            subtitleMatches: [],
            detailNodes: [],
            isFocused: false,
            onSelect: {}
        )
    ])

    static let fullscreenModel = FullscreenModel(nodes: pluginNodes)

    static let sceneState = SearchSceneState(
        query: "calc",
        placeholder: "Search commands",
        selectionName: "cursorAtEnd",
        resultsBody: searchResultsBody,
        fullscreen: nil,
        loadingStatusMessage: nil,
        actions: [
            OverlayActionModel(
                id: "action.run",
                title: "Run Command",
                iconAsset: .builtinName("ArrowForward"),
                style: .normal,
                onSelect: {}
            ),
            OverlayActionModel(
                id: "action.delete",
                title: "Forget Recent Result",
                iconAsset: .builtinName("Clear"),
                style: .destructive,
                onSelect: {}
            )
        ],
        actionTitle: "Run Command",
        showsActionToggle: true,
        showsActionsPanel: true,
        showsBackButton: false,
        exitBackspaceCount: 0,
        toasts: [
            ToastModel(
                id: "toast.success",
                message: "Plugin cache refreshed.",
                style: .success,
                onDismiss: {}
            )
        ],
        alert: AlertModel(
            id: "alert",
            title: "Overwrite Plugin?",
            message: "This replaces the cached plugin artifact.",
            confirmTitle: "Overwrite",
            onConfirm: {},
            dismissButton: AlertDismissButtonModel(
                title: "Cancel",
                role: .cancel,
                action: {}
            )
        )
    )

    static let fullscreenSceneState = SearchSceneState(
        query: "2+2",
        placeholder: "Search commands",
        selectionName: "cursorAtEnd",
        resultsBody: .empty,
        fullscreen: fullscreenModel,
        loadingStatusMessage: nil,
        actions: [
            OverlayActionModel(
                id: "action.copy",
                title: "Copy Result",
                iconAsset: .builtinName("ArrowForward"),
                style: .normal,
                onSelect: {}
            )
        ],
        actionTitle: "Copy Result",
        showsActionToggle: true,
        showsActionsPanel: true,
        showsBackButton: true,
        exitBackspaceCount: 1,
        toasts: [],
        alert: nil
    )
}

@MainActor
extension SearchSceneModel {
    static func preview(state: SearchSceneState) -> SearchSceneModel {
        let model = SearchSceneModel(
            viewModelClient: PreviewSearchViewModelClient(),
            router: AppRouter()
        )
        model.state = state
        return model
    }
}
