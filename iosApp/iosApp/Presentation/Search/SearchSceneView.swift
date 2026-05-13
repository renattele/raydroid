import SwiftUI
import UIKit

@MainActor
struct SearchSceneView: View {
    @State private var model: SearchSceneModel
    @State private var router: AppRouter
    private let spotlightIndexing: any SpotlightIndexing
    @FocusState private var searchFocused: Bool

    @MainActor
    init(container: RaydroidAppContainer) {
        _model = State(initialValue: SearchSceneModel(storeClient: container.searchStoreClient))
        _router = State(initialValue: container.router)
        self.spotlightIndexing = container.spotlightIndexing
    }

    @MainActor
    init(
        model: SearchSceneModel,
        router: AppRouter,
        spotlightIndexing: any SpotlightIndexing
    ) {
        _model = State(initialValue: model)
        _router = State(initialValue: router)
        self.spotlightIndexing = spotlightIndexing
    }

    var body: some View {
        NavigationStack {
            ZStack {
                RaydroidBackground()

                VStack(spacing: 0) {
                    ZStack(alignment: .top) {
                        if let fullscreen = model.state.fullscreen {
                            FullscreenContentView(fullscreen: fullscreen)
                        } else {
                            SearchResultsView(resultsBody: model.state.resultsBody)
                        }

                        OverlayChrome(
                            actions: model.state.actions,
                            toasts: model.state.toasts,
                            showsActionsPanel: model.state.showsActionsPanel
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                        .padding(.trailing, 16)
                        .padding(.bottom, 16)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                    SearchDock(
                        query: Binding(
                            get: { model.state.query },
                            set: model.updateQuery
                        ),
                        placeholder: model.state.placeholder,
                        focused: $searchFocused,
                        showsBackButton: model.state.showsBackButton,
                        exitBackspaceCount: model.state.exitBackspaceCount,
                        actionTitle: model.state.actionTitle,
                        showsActions: model.state.showsActionsPanel,
                        onSubmit: model.submit,
                        onBack: model.closeFullscreen,
                        onToggleActions: model.toggleActions
                    )
                    .padding(.horizontal, 16)
                    .padding(.bottom, 10)
                }
            }
            .navigationTitle("Raydroid")
            .toolbarTitleDisplayMode(.inline)
            .onAppear {
                keepSearchFocused()
            }
            .task {
                keepSearchFocused()
                model.start()
                await spotlightIndexing.indexCommands()
                if router.pendingRoute != nil {
                    model.applyRoute(router.pendingRoute, router: router)
                    keepSearchFocused()
                }
            }
            .onDisappear {
                model.stop()
            }
            .onChange(of: router.pendingRoute) { _, route in
                if route != nil {
                    model.applyRoute(route, router: router)
                    keepSearchFocused()
                }
            }
            .onChange(of: searchFocused) { _, focused in
                if !focused {
                    keepSearchFocused()
                }
            }
            .alert(item: Binding(
                get: { model.state.alert },
                set: { _ in }
            )) { alert in
                buildAlert(for: alert)
            }
        }
    }

    private func keepSearchFocused() {
        Task { @MainActor in
            searchFocused = true
        }
    }

    private func buildAlert(for alert: AlertModel) -> Alert {
        let title = Text(alert.title)
        let message = Text(alert.message)
        let confirmTitle = Text(alert.confirmTitle)
        if let dismissButton = alert.dismissButton {
            return Alert(
                title: title,
                message: message,
                primaryButton: .default(confirmTitle, action: alert.onConfirm),
                secondaryButton: dismissButtonView(dismissButton)
            )
        }
        return Alert(
            title: title,
            message: message,
            dismissButton: .default(confirmTitle, action: alert.onConfirm)
        )
    }

    private func dismissButtonView(_ button: AlertDismissButtonModel) -> Alert.Button {
        let title = Text(button.title)
        switch button.role {
        case .cancel:
            return .cancel(title, action: button.action)
        case .destructive:
            return .destructive(title, action: button.action)
        case .normal:
            return .default(title, action: button.action)
        }
    }
}

private struct SearchResultsView: View {
    let resultsBody: SearchResultsBodyState

    var body: some View {
        switch resultsBody {
        case .results(let results):
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(results) { result in
                        SearchResultRow(result: result)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 32)
            }
            .scrollDismissesKeyboard(.never)
        case .loading:
            VStack {
                ProgressView()
                Text("Searching...")
                    .foregroundStyle(.secondary)
            }
        case .empty:
            ContentUnavailableView("No results", systemImage: "magnifyingglass")
        }
    }
}

private struct SearchResultRow: View {
    let result: SearchResultRowModel

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ResultListRow(
                icon: result.iconAsset,
                title: result.title,
                subtitle: result.subtitle,
                titleMatches: result.titleMatches,
                subtitleMatches: result.subtitleMatches
            )

            if !result.detailNodes.isEmpty {
                PluginNodeListView(nodes: result.detailNodes)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .raydroidGlassSurface(cornerRadius: 20, interactive: true)
        .overlay {
            RoundedRectangle(cornerRadius: 20)
                .stroke(result.isFocused ? Color.accentColor.opacity(0.45) : Color.clear, lineWidth: 1.5)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: result.onSelect)
    }
}

private struct ResultListRow: View {
    let icon: PluginAsset?
    let title: String
    let subtitle: String
    let titleMatches: [HighlightMatch]
    let subtitleMatches: [HighlightMatch]

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            PluginIconView(asset: icon, tint: .primary, size: 20)
                .frame(width: 28, height: 28)
            VStack(alignment: .leading, spacing: 4) {
                HighlightedText(text: title, matches: titleMatches)
                    .font(.headline)
                if !subtitle.isEmpty {
                    HighlightedText(text: subtitle, matches: subtitleMatches)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}

private struct FullscreenContentView: View {
    let fullscreen: FullscreenModel

    var body: some View {
        ScrollView {
            PluginNodeListView(nodes: fullscreen.nodes)
                .foregroundStyle(.primary)
                .padding(.horizontal, 16)
                .padding(.top, 20)
                .padding(.bottom, 32)
        }
        .scrollDismissesKeyboard(.never)
    }
}

private struct OverlayChrome: View {
    let actions: [OverlayActionModel]
    let toasts: [ToastModel]
    let showsActionsPanel: Bool
    private var actionIds: [String] { actions.map(\.id) }

    var body: some View {
        VStack(alignment: .trailing, spacing: 10) {
            ForEach(toasts) { toast in
                ToastView(toast: toast)
            }
            if showsActionsPanel && !actions.isEmpty {
                ActionsOverlay(actions: actions)
                    .transition(.asymmetric(
                        insertion: .opacity
                            .combined(with: .move(edge: .bottom))
                            .combined(with: .scale(scale: 0.94, anchor: .bottomTrailing)),
                        removal: .opacity
                            .combined(with: .move(edge: .bottom))
                            .combined(with: .scale(scale: 0.98, anchor: .bottomTrailing))
                    ))
                    .zIndex(1)
            }
        }
        .animation(.spring(response: 0.28, dampingFraction: 0.82), value: showsActionsPanel)
        .animation(.spring(response: 0.3, dampingFraction: 0.86), value: actionIds)
    }
}

private struct ActionsOverlay: View {
    let actions: [OverlayActionModel]
    @State private var visibleActionIds: Set<String> = []
    @Namespace private var actionGlassNamespace
    private var actionIds: [String] { actions.map(\.id) }

    var body: some View {
        Group {
            if #available(iOS 26.0, *) {
                liquidGlassBody
            } else {
                fallbackBody
            }
        }
        .onAppear(perform: animateVisibleActions)
        .onChange(of: actionIds) { _, _ in
            animateVisibleActions()
        }
    }

    @available(iOS 26.0, *)
    private var liquidGlassBody: some View {
        GlassEffectContainer(spacing: 10) {
            actionsStack { index, action, isVisible in
                Button(action: action.onSelect) {
                    actionContent(action)
                        .padding(.horizontal, 12)
                        .frame(height: 38)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .glassEffect(
                            action.style == .destructive
                                ? .regular.tint(.red.opacity(0.14)).interactive()
                                : .regular.interactive(),
                            in: .rect(cornerRadius: 16)
                        )
                        .glassEffectID(action.id, in: actionGlassNamespace)
                }
                .buttonStyle(.plain)
                .opacity(isVisible ? 1 : 0)
                .offset(y: isVisible ? 0 : 10)
                .scaleEffect(isVisible ? 1 : 0.92, anchor: .bottomTrailing)
                .animation(
                    .spring(response: 0.34, dampingFraction: 0.72, blendDuration: 0.08)
                    .delay(Double(index) * 0.035),
                    value: visibleActionIds
                )
            }
        }
        .padding(6)
        .frame(width: 252, alignment: .leading)
    }

    private var fallbackBody: some View {
        actionsStack { _, action, isVisible in
            Button(action: action.onSelect) {
                actionContent(action)
                    .padding(.horizontal, 12)
                    .frame(height: 38)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)
            .opacity(isVisible ? 1 : 0)
            .offset(x: isVisible ? 0 : 12)
            .scaleEffect(isVisible ? 1 : 0.98, anchor: .trailing)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .padding(6)
        .frame(width: 252, alignment: .leading)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(.white.opacity(0.22), lineWidth: 1)
        }
    }

    private func actionsStack<Row: View>(
        @ViewBuilder row: @escaping (_ index: Int, _ action: OverlayActionModel, _ isVisible: Bool) -> Row
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                let isVisible = visibleActionIds.contains(action.id)
                row(index, action, isVisible)
                .transition(.asymmetric(
                    insertion: .opacity
                        .combined(with: .move(edge: .bottom))
                        .combined(with: .scale(scale: 0.92, anchor: .bottomTrailing)),
                    removal: .opacity
                        .combined(with: .scale(scale: 0.96, anchor: .bottomTrailing))
                ))
            }
        }
    }

    private func actionContent(_ action: OverlayActionModel) -> some View {
        HStack(spacing: 10) {
            PluginIconView(
                asset: action.iconAsset,
                tint: action.style == .destructive ? .red : .primary,
                size: 16
            )
            .frame(width: 18, height: 18)
            Text(action.title)
                .font(.subheadline)
                .foregroundStyle(action.style == .destructive ? .red : .primary)
                .lineLimit(1)
        }
    }

    private func animateVisibleActions() {
        visibleActionIds.formIntersection(actionIds)
        for (index, id) in actionIds.enumerated() {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: UInt64(index) * 25_000_000)
                guard actionIds.contains(id) else { return }
                withAnimation(.spring(response: 0.24, dampingFraction: 0.88)) {
                    visibleActionIds.insert(id)
                }
            }
        }
    }
}

private struct ToastView: View {
    let toast: ToastModel

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: iconName)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(color)
            Text(toast.message)
                .font(.subheadline)
                .foregroundStyle(.primary)
            Button(action: toast.onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .semibold))
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .raydroidGlassSurface(cornerRadius: 16, interactive: false)
    }

    private var color: Color {
        switch toast.style {
        case .success:
            return .green
        case .failure:
            return .red
        case .info:
            return .accentColor
        }
    }

    private var iconName: String {
        switch toast.style {
        case .success:
            return "checkmark.circle.fill"
        case .failure:
            return "exclamationmark.triangle.fill"
        case .info:
            return "sparkles"
        }
    }
}

private struct SearchDock: View {
    @Binding var query: String
    let placeholder: String
    let focused: FocusState<Bool>.Binding
    let showsBackButton: Bool
    let exitBackspaceCount: Int
    let actionTitle: String?
    let showsActions: Bool
    let onSubmit: () -> Void
    let onBack: () -> Void
    let onToggleActions: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            HStack(spacing: 8) {
                if showsBackButton {
                    FullscreenBackButton(
                        exitBackspaceCount: exitBackspaceCount,
                        onBack: onBack
                    )
                }

                TextField(placeholder, text: $query)
                    .focused(focused)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel((actionTitle?.isEmpty ?? true) ? .search : .go)
                    .onSubmit(onSubmit)
                    .font(.body)
                    .foregroundStyle(.primary)
                    .tint(.blue)
                    .frame(maxWidth: .infinity)
                    .frame(height: 30)
                    .layoutPriority(1)

                SearchInlineActions(
                    title: actionTitle,
                    showsActions: showsActions,
                    onSubmit: onSubmit,
                    onToggleActions: onToggleActions
                )
            }
            .padding(.leading, showsBackButton ? 6 : 16)
            .padding(.trailing, 8)
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(.white, in: Capsule())
            .overlay {
                Capsule()
                    .stroke(Color.black.opacity(0.08), lineWidth: 1)
            }
            .shadow(color: .black.opacity(0.04), radius: 8, y: 2)
            .layoutPriority(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.leading, 8)
        .padding(.trailing, 6)
        .padding(.vertical, 6)
        .frame(minHeight: 48)
    }
}

private struct FullscreenBackButton: View {
    let exitBackspaceCount: Int
    let onBack: () -> Void

    var body: some View {
        Button(action: onBack) {
            Image(systemName: "chevron.backward")
                .font(.system(size: iconSize, weight: .bold))
                .foregroundStyle(Color.accentColor)
                .frame(width: buttonWidth, height: 44)
                .opacity(exitBackspaceCount >= 2 ? 0 : 1)
        }
        .buttonStyle(.raydroidLiquidGlassProminent)
        .disabled(exitBackspaceCount >= 2)
        .frame(width: slotWidth, height: 44, alignment: .center)
        .clipped()
        .animation(.easeInOut(duration: 0.18), value: exitBackspaceCount)
    }

    private var slotWidth: CGFloat {
        switch exitBackspaceCount {
        case 0: return 52
        case 1: return 44
        default: return 0
        }
    }

    private var buttonWidth: CGFloat {
        switch exitBackspaceCount {
        case 0: return 44
        case 1: return 36
        default: return 0
        }
    }

    private var iconSize: CGFloat {
        switch exitBackspaceCount {
        case 0: return 18
        case 1: return 16
        default: return 0
        }
    }
}

private struct SearchInlineActions: View {
    let title: String?
    let showsActions: Bool
    let onSubmit: () -> Void
    let onToggleActions: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            if let title, !title.isEmpty {
                Button(action: onSubmit) {
                    HStack(spacing: 6) {
                        Text(title)
                            .font(.subheadline.weight(.semibold))
                            .lineLimit(1)
                            .truncationMode(.tail)
                            .minimumScaleFactor(0.82)

                        Image(systemName: "arrow.turn.down.left")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .padding(.horizontal, 12)
                    .frame(maxWidth: 150)
                    .frame(height: 36)
                }
                .buttonStyle(.raydroidLiquidGlassAccessory)
                .accessibilityLabel("Run \(title)")

                if showsActions {
                    Button(action: onToggleActions) {
                        Image(systemName: "chevron.down")
                            .font(.system(size: 14, weight: .semibold))
                            .frame(width: 36, height: 36)
                    }
                    .buttonStyle(.raydroidLiquidGlassProminent)
                    .accessibilityLabel("Hide actions")
                    .transition(.scale(scale: 0.86).combined(with: .opacity))
                } else {
                    Button(action: onToggleActions) {
                        Image(systemName: "chevron.up")
                            .font(.system(size: 14, weight: .semibold))
                            .frame(width: 36, height: 36)
                    }
                    .buttonStyle(.raydroidLiquidGlassAccessory)
                    .accessibilityLabel("Show actions")
                    .transition(.scale(scale: 0.86).combined(with: .opacity))
                }
            }
        }
        .fixedSize(horizontal: true, vertical: false)
        .animation(.spring(response: 0.22, dampingFraction: 0.82), value: showsActions)
    }
}

private struct GlassButtonCluster<Content: View>: View {
    let spacing: CGFloat
    let content: Content

    init(spacing: CGFloat, @ViewBuilder content: () -> Content) {
        self.spacing = spacing
        self.content = content()
    }

    var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: spacing) {
                content
            }
        } else {
            content
        }
    }
}

#Preview("Search Scene") {
    SearchSceneView(
        model: .preview(state: PreviewData.sceneState),
        router: AppRouter(),
        spotlightIndexing: PreviewSpotlightIndexing()
    )
}

#Preview("Fullscreen Scene") {
    SearchSceneView(
        model: .preview(state: PreviewData.fullscreenSceneState),
        router: AppRouter(),
        spotlightIndexing: PreviewSpotlightIndexing()
    )
}

#Preview("Results List") {
    SearchResultsView(resultsBody: PreviewData.searchResultsBody)
        .padding()
        .background(RaydroidBackground())
}

#Preview("Action Overlay") {
    OverlayChrome(
        actions: PreviewData.sceneState.actions,
        toasts: PreviewData.sceneState.toasts,
        showsActionsPanel: PreviewData.sceneState.showsActionsPanel
    )
    .padding()
    .background(RaydroidBackground())
}

#Preview("Search Dock") {
    PreviewSearchDock()
        .padding()
        .background(RaydroidBackground())
}

#Preview("Back Button") {
    FullscreenBackButton(
        exitBackspaceCount: 1,
        onBack: {}
    )
    .padding()
    .background(RaydroidBackground())
}

#Preview("Inline Search Actions") {
    SearchInlineActions(
        title: "Run Command",
        showsActions: true,
        onSubmit: {},
        onToggleActions: {}
    )
    .padding()
    .background(RaydroidBackground())
}

private struct PreviewSearchDock: View {
    @State private var query = "calc"
    @FocusState private var focused: Bool

    var body: some View {
        SearchDock(
            query: $query,
            placeholder: "Search commands",
            focused: $focused,
            showsBackButton: true,
            exitBackspaceCount: 1,
            actionTitle: "Run Command",
            showsActions: true,
            onSubmit: {},
            onBack: {},
            onToggleActions: {}
        )
    }
}
