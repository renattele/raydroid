import SwiftUI
import UIKit

@MainActor
struct SearchSceneView: View {
    @State private var model: SearchSceneViewModel
    @State private var router: AppRouter

    @MainActor
    init(container: RaydroidAppContainer) {
        _model = State(
            initialValue: SearchSceneViewModel(
                viewModelClient: container.searchViewModelClient,
                router: container.router
            )
        )
        _router = State(initialValue: container.router)
    }

    @MainActor
    init(
        model: SearchSceneViewModel,
        router: AppRouter
    ) {
        _model = State(initialValue: model)
        _router = State(initialValue: router)
    }

    var body: some View {
        let shouldFocusSearchDock = model.state.aliasEditor == nil && !model.state.showsBackButton
        let showsContextMenu = model.state.showsContextMenu

        NavigationStack {
            ZStack {
                RaydroidBackground()

                VStack(spacing: 0) {
                    ZStack(alignment: .top) {
                        if let fullscreen = model.state.fullscreen {
                            FullscreenContentView(
                                fullscreen: fullscreen,
                                showsContextMenu: showsContextMenu,
                                onScrollStart: {
                                    model.send(.hideActions)
                                }
                            )
                        } else {
                            SearchResultsView(
                                resultsBody: model.state.resultsBody,
                                showsContextMenu: showsContextMenu,
                                onScrollStart: {
                                    model.send(.hideActions)
                                }
                            )
                        }

                        OverlayChrome(
                            actions: model.state.actions,
                            toasts: model.state.toasts,
                            showsActionsPanel: model.state.showsActionsPanel
                        )
                        .contextMenuLayer(
                            showsContextMenu: showsContextMenu,
                            isActive: false
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                        .padding(.trailing, 16)
                        .padding(.bottom, 72)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                    if let loadingStatusMessage = model.state.loadingStatusMessage {
                        LoadingStatusRow(message: loadingStatusMessage)
                            .padding(.horizontal, 16)
                            .padding(.bottom, 6)
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                    }
                }

                SearchDock(
                    query: model.state.query,
                    placeholder: model.state.placeholder,
                    selectionName: model.state.selectionName,
                    desiredFocus: shouldFocusSearchDock,
                    retainFocusWhenBlurred: shouldFocusSearchDock,
                    showsBackButton: model.state.showsBackButton,
                    exitBackspaceCount: model.state.exitBackspaceCount,
                    actionTitle: model.state.actionTitle,
                    showsActionToggle: model.state.showsActionToggle,
                    showsActions: model.state.showsActionsPanel,
                    onQueryChange: { query, selectionName in
                        model.send(.queryChanged(query, selectionName))
                    },
                    onSubmit: {
                        model.send(.submit)
                    },
                    onBackspaceOnEmpty: {
                        model.send(.backspaceOnEmpty)
                    },
                    onMoveFocusUp: {
                        model.send(model.state.showsBackButton ? .moveFocusPrevious : .moveFocusNext)
                    },
                    onMoveFocusDown: {
                        model.send(model.state.showsBackButton ? .moveFocusNext : .moveFocusPrevious)
                    },
                    onBack: {
                        model.send(.closeFullscreen)
                    },
                    onToggleActions: {
                        model.send(.toggleActions)
                    }
                )
                .contextMenuLayer(
                    showsContextMenu: showsContextMenu,
                    isActive: false
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 10)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
            }
            .overlayPreferenceValue(ContextMenuAnchorPreferenceKey.self) { anchors in
                if model.state.showsContextMenu,
                   let activeContextSourceId = model.state.activeContextSourceId,
                   let anchorRect = anchors[activeContextSourceId] {
                    AnchoredContextActionsOverlay(
                        actions: model.state.contextActions,
                        anchorRect: CGRect(
                            x: anchorRect.minX,
                            y: anchorRect.minY,
                            width: anchorRect.width,
                            height: anchorRect.height
                        ),
                        onDismiss: {
                            model.send(.hideActions)
                        }
                    )
                }
            }
            .navigationTitle("Raydroid")
            .toolbarTitleDisplayMode(.inline)
            .onAppear {
                model.send(.appear)
            }
            .onDisappear {
                model.send(.disappear)
            }
            .onChange(of: router.pendingRoute) { _, route in
                if route != nil {
                    model.send(.route(route))
                }
            }
            .alert(item: Binding(
                get: { model.state.alert },
                set: { _ in }
            )) { alert in
                buildAlert(for: alert)
            }
            .sheet(
                item: Binding(
                    get: { model.state.aliasEditor },
                    set: { aliasEditor in
                        if aliasEditor == nil {
                            model.state.aliasEditor?.onDismiss()
                        }
                    }
                )
            ) { aliasEditor in
                AliasEditorSheetView(editor: aliasEditor)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
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

private struct AliasEditorSheetView: View {
    let editor: AliasEditorSheetModel
    @State private var input: String

    init(editor: AliasEditorSheetModel) {
        self.editor = editor
        _input = State(initialValue: editor.input)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(editor.title)
                        .font(.title3.weight(.semibold))
                    Text("Create a short keyboard-friendly shortcut for this result.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                if let subjectTitle = editor.subjectTitle, !subjectTitle.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("For")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(subjectTitle)
                            .font(.headline)
                            .foregroundStyle(.primary)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18))
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Alias")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextField(
                        "e.g. yt",
                        text: Binding(
                            get: { input },
                            set: { value in
                                input = value
                                editor.onInputChanged(value)
                            }
                        )
                    )
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 18))

                    Text("ASCII only. No spaces.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                if let error = editor.error, !error.isEmpty {
                    Text(error)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 12)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: editor.onDismiss)
                        .foregroundStyle(.secondary)
                }
                if let onRemove = editor.onRemove {
                    ToolbarItem(placement: .bottomBar) {
                        Button("Remove", action: onRemove)
                            .foregroundStyle(.red)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        editor.onInputChanged(input)
                        editor.onSave()
                    }
                    .disabled(input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationBackground(.thinMaterial)
        .onChange(of: editor.input) { _, updatedInput in
            if input != updatedInput {
                input = updatedInput
            }
        }
    }
}

private struct LoadingStatusRow: View {
    let message: String

    var body: some View {
        HStack(spacing: 10) {
            ProgressView()
                .controlSize(.small)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial, in: Capsule())
        .overlay {
            Capsule()
                .stroke(Color(uiColor: .separator).opacity(0.18), lineWidth: 1)
        }
    }
}

private struct SearchResultsView: View {
    let resultsBody: SearchResultsBodyState
    let showsContextMenu: Bool
    let onScrollStart: () -> Void

    var body: some View {
        switch resultsBody {
        case .results(let results):
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(results.reversed()) { result in
                        SearchResultRow(result: result)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 112)
            }
            .defaultScrollAnchor(.bottom)
            .scrollDismissesKeyboard(.never)
            .simultaneousGesture(
                DragGesture(minimumDistance: 1)
                    .onChanged { _ in
                        if showsContextMenu {
                            onScrollStart()
                        }
                    }
            )
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
            if result.showsListEntry {
                ResultListRow(
                    icon: result.iconAsset,
                    title: result.title,
                    alias: result.alias,
                    subtitle: result.subtitle,
                    trailingText: result.trailingText,
                    titleMatches: result.titleMatches,
                    subtitleMatches: result.subtitleMatches
                )
            }

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
        .contextMenuAnchor(sourceId: result.contextSourceId)
        .contextMenuPressable(
            isContextMenuPresented: result.isContextMenuPresented,
            isContextMenuActive: result.isContextMenuActive,
            onTap: result.onSelect,
            onLongPress: result.onLongPress
        )
    }
}

private struct ResultListRow: View {
    let icon: PluginAsset?
    let title: String
    let alias: String?
    let subtitle: String
    let trailingText: String
    let titleMatches: [HighlightMatch]
    let subtitleMatches: [HighlightMatch]

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            PluginIconView(asset: icon, tint: .primary, size: 20)
                .frame(width: 28, height: 28)
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    HighlightedText(text: title, matches: titleMatches)
                        .font(.headline)
                    Spacer(minLength: 0)
                    if let alias, !alias.isEmpty {
                        AliasBadge(alias: alias)
                    }
                    if !trailingText.isEmpty {
                        Text(trailingText)
                            .font(.headline.monospacedDigit())
                            .foregroundStyle(.primary)
                    }
                }
                if !subtitle.isEmpty {
                    HighlightedText(text: subtitle, matches: subtitleMatches)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}

private struct AliasBadge: View {
    let alias: String

    var body: some View {
        Text(alias)
            .font(.caption.weight(.medium))
            .foregroundStyle(Color(uiColor: .label))
            .lineLimit(1)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                Color.accentColor.opacity(0.16),
                in: RoundedRectangle(cornerRadius: 8, style: .continuous)
            )
    }
}

private struct FullscreenContentView: View {
    let fullscreen: FullscreenModel
    let showsContextMenu: Bool
    let onScrollStart: () -> Void

    var body: some View {
        ScrollView {
            PluginNodeListView(
                nodes: fullscreen.nodes,
                showsContextButtons: false
            )
                .foregroundStyle(.primary)
                .padding(.horizontal, 16)
                .padding(.top, 20)
                .padding(.bottom, 112)
        }
        .scrollDismissesKeyboard(.never)
        .simultaneousGesture(
            DragGesture(minimumDistance: 1)
                .onChanged { _ in
                    if showsContextMenu {
                        onScrollStart()
                    }
                }
        )
    }
}

private struct OverlayChrome: View {
    let actions: [OverlayActionModel]
    let toasts: [ToastModel]
    let showsActionsPanel: Bool
    private var actionIds: [String] { actions.map(\.id) }
    private var visibleToasts: [ToastModel] { Array(toasts.suffix(3)) }

    var body: some View {
        VStack(alignment: .trailing, spacing: 10) {
            ForEach(visibleToasts) { toast in
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

private struct AnchoredContextActionsOverlay: View {
    let actions: [OverlayActionModel]
    let anchorRect: CGRect
    let onDismiss: () -> Void

    @State private var measuredMenuHeight: CGFloat = 0
    @State private var hasAppeared = false

    var body: some View {
        GeometryReader { proxy in
            let overlaySize = proxy.size
            let popupWidth = min(anchorRect.width, 280)
            let estimatedHeight = min(CGFloat(actions.count) * 46 + 20, 220)
            let popupHeight = measuredMenuHeight > 0 ? measuredMenuHeight : estimatedHeight
            let verticalGap: CGFloat = 14
            let attachmentOffset: CGFloat = 12
            let spaceBelow = overlaySize.height - anchorRect.maxY - 12
            let spaceAbove = anchorRect.minY - 12
            let prefersBelow = anchorRect.midY <= overlaySize.height * 0.55
            let showBelow = if prefersBelow {
                spaceBelow >= popupHeight + verticalGap || spaceBelow >= spaceAbove
            } else {
                !(spaceAbove >= popupHeight + verticalGap || spaceAbove > spaceBelow)
            }
            let x = min(
                max(anchorRect.minX, 12),
                max(12, overlaySize.width - popupWidth - 12)
            )
            let y = showBelow
                ? min(anchorRect.maxY + verticalGap - attachmentOffset, overlaySize.height - popupHeight - 12)
                : max(12, anchorRect.minY - popupHeight - verticalGap + attachmentOffset)

            ZStack(alignment: .topLeading) {
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture(perform: onDismiss)

                VStack(alignment: .leading, spacing: 8) {
                    ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                        if index > 0, actions[index - 1].groupTitle != action.groupTitle {
                            Divider()
                                .padding(.horizontal, 12)
                                .padding(.vertical, 2)
                        }
                        Button(action: action.onSelect) {
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
                            .padding(.horizontal, 12)
                            .frame(height: 38)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(6)
                .frame(width: popupWidth, alignment: .leading)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color(uiColor: .separator).opacity(0.28), lineWidth: 1)
                }
                .background {
                    GeometryReader { menuProxy in
                        Color.clear
                            .onAppear {
                                measuredMenuHeight = menuProxy.size.height
                            }
                            .onChange(of: menuProxy.size.height) { _, newHeight in
                                measuredMenuHeight = newHeight
                            }
                    }
                }
                .shadow(color: .black.opacity(0.18), radius: 18, y: 10)
                .offset(x: x, y: y)
                .opacity(hasAppeared ? 1 : 0)
                .scaleEffect(hasAppeared ? 1 : 0.96, anchor: .topLeading)
                .offset(y: hasAppeared ? 0 : (showBelow ? -6 : 6))
            }
        }
        .ignoresSafeArea()
        .zIndex(5)
        .onAppear {
            hasAppeared = false
            withAnimation(.spring(response: 0.26, dampingFraction: 0.82)) {
                hasAppeared = true
            }
        }
        .onDisappear {
            hasAppeared = false
        }
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
                }
                .frame(maxWidth: .infinity, minHeight: 38, alignment: .leading)
                .contentShape(.rect)
                .glassEffect(
                    action.style == .destructive
                        ? .regular.tint(.red.opacity(0.14)).interactive()
                        : .regular.interactive(),
                    in: .rect(cornerRadius: 16)
                )
                .glassEffectID(action.id, in: actionGlassNamespace)
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
            .frame(maxWidth: .infinity, minHeight: 38, alignment: .leading)
            .contentShape(.rect)
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
                .stroke(Color(uiColor: .separator).opacity(0.28), lineWidth: 1)
        }
    }

    private func actionsStack<Row: View>(
        @ViewBuilder row: @escaping (_ index: Int, _ action: OverlayActionModel, _ isVisible: Bool) -> Row
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                let isVisible = visibleActionIds.contains(action.id)
                if index > 0, actions[index - 1].groupTitle != action.groupTitle {
                    Divider()
                        .padding(.horizontal, 12)
                        .padding(.vertical, 2)
                }
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
                _ = withAnimation(.spring(response: 0.24, dampingFraction: 0.88)) {
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
    let query: String
    let placeholder: String
    let selectionName: String
    let desiredFocus: Bool
    let retainFocusWhenBlurred: Bool
    let showsBackButton: Bool
    let exitBackspaceCount: Int
    let actionTitle: String?
    let showsActionToggle: Bool
    let showsActions: Bool
    let onQueryChange: (String, String) -> Void
    let onSubmit: () -> Void
    let onBackspaceOnEmpty: () -> Void
    let onMoveFocusUp: () -> Void
    let onMoveFocusDown: () -> Void
    let onBack: () -> Void
    let onToggleActions: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            ZStack(alignment: .leading) {
                SearchInputField(
                    text: query,
                    placeholder: placeholder,
                    selectionName: selectionName,
                    desiredFocus: desiredFocus,
                    retainFocusWhenBlurred: retainFocusWhenBlurred,
                    canSubmit: !(actionTitle?.isEmpty ?? true),
                    onTextChange: onQueryChange,
                    onSubmit: onSubmit,
                    onBackspaceOnEmpty: onBackspaceOnEmpty,
                    onMoveFocusUp: onMoveFocusUp,
                    onMoveFocusDown: onMoveFocusDown
                )
                    .padding(.leading, searchFieldLeadingPadding)
                    .padding(.trailing, 14)
                    .frame(maxWidth: .infinity)
                    .frame(height: 36)
                    .clipped()
                    .layoutPriority(1)

                HStack(spacing: 0) {
                    FullscreenBackButton(
                        isVisible: showsBackButton,
                        exitBackspaceCount: exitBackspaceCount,
                        onBack: onBack
                    )
                    Spacer(minLength: 0)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .layoutPriority(1)
            .animation(dockAnimation, value: showsBackButton)
            .animation(dockAnimation, value: exitBackspaceCount)

            SearchInlineActions(
                title: actionTitle,
                showsActionToggle: showsActionToggle,
                showsActions: showsActions,
                onSubmit: onSubmit,
                onToggleActions: onToggleActions
            )
        }
        .frame(maxWidth: .infinity)
        .padding(.leading, 16)
        .padding(.trailing, 8)
        .padding(.vertical, 6)
        .frame(minHeight: 48)
        .background(Color(uiColor: .systemBackground).opacity(0.96), in: Capsule())
        .overlay {
            Capsule()
                .stroke(Color(uiColor: .separator).opacity(0.4), lineWidth: 1)
        }
        .shadow(color: Color.black.opacity(0.14), radius: 16, y: 6)
    }

    private var dockAnimation: Animation {
        .bouncy(duration: 0.34, extraBounce: 0.18)
    }

    private var searchFieldLeadingPadding: CGFloat {
        14 + backButtonInsetWidth
    }

    private var backButtonInsetWidth: CGFloat {
        guard showsBackButton else { return 0 }
        switch exitBackspaceCount {
        case 0: return 40
        case 1: return 32
        default: return 0
        }
    }
}

private struct FullscreenBackButton: View {
    let isVisible: Bool
    let exitBackspaceCount: Int
    let onBack: () -> Void
    @State private var hasAppeared = false

    var body: some View {
        Button(action: onBack) {
            Image(systemName: "chevron.backward")
                .font(.system(size: iconSize, weight: .bold))
                .foregroundStyle(Color.accentColor)
                .frame(width: buttonWidth, height: 44)
                .opacity(iconOpacity)
                .offset(x: iconOffsetX)
        }
        .buttonStyle(.raydroidLiquidGlassProminent)
        .disabled(!isInteractive)
        .frame(width: slotWidth, height: 44, alignment: .center)
        .clipped()
        .onAppear {
            hasAppeared = true
        }
        .animation(animation, value: hasAppeared)
        .animation(animation, value: isVisible)
        .animation(animation, value: exitBackspaceCount)
    }

    private var slotWidth: CGFloat {
        guard isExpanded else { return 0 }
        switch exitBackspaceCount {
        case 0: return 48
        case 1: return 40
        default: return 0
        }
    }

    private var buttonWidth: CGFloat {
        guard isExpanded else { return 0 }
        switch exitBackspaceCount {
        case 0: return 40
        case 1: return 32
        default: return 0
        }
    }

    private var iconSize: CGFloat {
        guard isExpanded else { return 0 }
        switch exitBackspaceCount {
        case 0: return 22
        case 1: return 18
        default: return 0
        }
    }

    private var iconOpacity: Double {
        isExpanded && isInteractive ? 1 : 0
    }

    private var iconOffsetX: CGFloat {
        isExpanded ? 0 : -8
    }

    private var isExpanded: Bool {
        hasAppeared && isVisible
    }

    private var isInteractive: Bool {
        isVisible && exitBackspaceCount < 2
    }

    private var animation: Animation {
        .bouncy(duration: 0.34, extraBounce: 0.18)
    }
}

private struct SearchInlineActions: View {
    let title: String?
    let showsActionToggle: Bool
    let showsActions: Bool
    let onSubmit: () -> Void
    let onToggleActions: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            if let title, !title.isEmpty {
                HStack(spacing: 6) {
                    Text(title)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .frame(maxWidth: 92, alignment: .trailing)

                    Button(action: onSubmit) {
                        Image(systemName: "return")
                            .font(.system(size: 11, weight: .semibold))
                            .frame(width: 30, height: 30)
                    }
                    .buttonStyle(.raydroidLiquidGlassAccessory)
                    .accessibilityLabel("Run \(title)")
                }
            }

            if showsActionToggle {
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
        router: AppRouter()
    )
}

#Preview("Fullscreen Scene") {
    SearchSceneView(
        model: .preview(state: PreviewData.fullscreenSceneState),
        router: AppRouter()
    )
}

#Preview("Results List") {
    SearchResultsView(
        resultsBody: PreviewData.searchResultsBody,
        showsContextMenu: false,
        onScrollStart: {}
    )
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
        isVisible: true,
        exitBackspaceCount: 1,
        onBack: {}
    )
    .padding()
    .background(RaydroidBackground())
}

#Preview("Inline Search Actions") {
    SearchInlineActions(
        title: "Run Command",
        showsActionToggle: true,
        showsActions: true,
        onSubmit: {},
        onToggleActions: {}
    )
    .padding()
    .background(RaydroidBackground())
}

private struct PreviewSearchDock: View {
    var body: some View {
        SearchDock(
            query: "calc",
            placeholder: "Search commands",
            selectionName: "cursorAtEnd",
            desiredFocus: false,
            retainFocusWhenBlurred: false,
            showsBackButton: true,
            exitBackspaceCount: 1,
            actionTitle: "Run Command",
            showsActionToggle: true,
            showsActions: true,
            onQueryChange: { _, _ in },
            onSubmit: {},
            onBackspaceOnEmpty: {},
            onMoveFocusUp: {},
            onMoveFocusDown: {},
            onBack: {},
            onToggleActions: {}
        )
    }
}
