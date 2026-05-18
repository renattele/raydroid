import SwiftUI
import UIKit

struct HighlightMatch {
    let start: Int
    let end: Int
}

enum ContextMenuCoordinateSpace {
    static let name = "search-scene-context-menu"
}

struct ContextMenuAnchorPreferenceKey: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]

    static func reduce(value: inout [String : CGRect], nextValue: () -> [String : CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, new in new })
    }
}

private struct ContextMenuLayerModifier: ViewModifier {
    let showsContextMenu: Bool
    let isActive: Bool
    let blurRadius: CGFloat
    let inactiveOpacity: Double

    func body(content: Content) -> some View {
        content
            .blur(radius: showsContextMenu && !isActive ? blurRadius : 0)
            .opacity(showsContextMenu && !isActive ? inactiveOpacity : 1)
            .zIndex(isActive ? 4 : 0)
            .animation(.spring(response: 0.26, dampingFraction: 0.82), value: showsContextMenu)
            .animation(.spring(response: 0.26, dampingFraction: 0.82), value: isActive)
    }
}

private struct ContextMenuPressableModifier: ViewModifier {
    let sourceId: String?
    let isContextMenuPresented: Bool
    let isContextMenuActive: Bool
    let onTap: () -> Void
    let onLongPress: () -> Void

    @GestureState private var isPressing = false

    func body(content: Content) -> some View {
        content
            .contextMenuAnchor(sourceId: sourceId)
            .contextMenuLayer(
                showsContextMenu: isContextMenuPresented,
                isActive: isContextMenuActive
            )
            .scaleEffect(isContextMenuActive ? 1.06 : (isPressing ? 1.02 : 1))
            .animation(.spring(response: 0.24, dampingFraction: 0.72), value: isPressing)
            .animation(.spring(response: 0.28, dampingFraction: 0.74), value: isContextMenuActive)
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .updating($isPressing) { _, state, _ in
                        state = true
                    }
            )
            .onTapGesture(perform: onTap)
            .onLongPressGesture(minimumDuration: 0.45, maximumDistance: 18, perform: onLongPress)
    }
}

struct RaydroidBackground: View {
    var body: some View {
        LinearGradient(
            colors: [
                Color(uiColor: .systemBackground),
                Color(uiColor: .secondarySystemBackground),
                Color(uiColor: .tertiarySystemBackground)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()
    }
}

struct MarkdownText: View {
    let markdown: String

    var body: some View {
        if let attributed = try? AttributedString(markdown: markdown) {
            Text(attributed)
                .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            Text(markdown)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct HighlightedText: View {
    let text: String
    let matches: [HighlightMatch]

    var body: some View {
        Text(attributed)
    }

    private var attributed: AttributedString {
        var value = AttributedString(text)
        for match in matches {
            guard !text.isEmpty else { continue }
            let lower = max(min(match.start, text.count - 1), 0)
            let upper = max(min(match.end, text.count - 1), 0)
            guard lower <= upper else { continue }
            let start = value.characters.index(value.startIndex, offsetBy: lower)
            let end = value.characters.index(value.startIndex, offsetBy: upper + 1)
            value[start..<end].foregroundColor = .accentColor
            value[start..<end].font = .body.weight(.semibold)
        }
        return value
    }
}

struct FlowLayout<Data: RandomAccessCollection, Content: View>: View where Data.Element: Identifiable {
    let items: Data
    let content: (Data.Element) -> Content

    init(items: Data, @ViewBuilder content: @escaping (Data.Element) -> Content) {
        self.items = items
        self.content = content
    }

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 80), spacing: 8)],
            alignment: .leading,
            spacing: 8
        ) {
            ForEach(items) { item in
                content(item)
            }
        }
    }
}

struct PluginIconView: View {
    let asset: PluginAsset?
    let tint: Color?
    let size: CGFloat

    var body: some View {
        Group {
            switch asset {
            case .remoteURL(let value):
                AsyncImage(url: URL(string: value)) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    ProgressView()
                }
            case .base64(let value):
                if let image = imageFromBase64(value) {
                    Image(uiImage: image).resizable().scaledToFit()
                }
            case .binary(let value):
                if let image = UIImage(data: value) {
                    Image(uiImage: image).resizable().scaledToFit()
                }
            case .builtinName(let value):
                Image(systemName: sfSymbol(for: value))
                    .resizable()
                    .scaledToFit()
                    .symbolRenderingMode(.hierarchical)
                    .foregroundStyle(tint ?? .primary)
            case .none:
                Image(systemName: "questionmark.circle")
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: size, height: size)
        .foregroundStyle(tint ?? .primary)
    }
}

struct PluginImageView: View {
    let asset: PluginAsset?

    var body: some View {
        Group {
            switch asset {
            case .remoteURL(let value):
                AsyncImage(url: URL(string: value)) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    ProgressView()
                }
            case .base64(let value):
                if let image = imageFromBase64(value) {
                    Image(uiImage: image).resizable().scaledToFill()
                } else {
                    Color.secondary.opacity(0.12)
                }
            case .binary(let value):
                if let image = UIImage(data: value) {
                    Image(uiImage: image).resizable().scaledToFill()
                } else {
                    Color.secondary.opacity(0.12)
                }
            case .builtinName(let value):
                Image(systemName: sfSymbol(for: value))
                    .resizable()
                    .scaledToFit()
                    .padding(20)
                    .foregroundStyle(.secondary)
            case .none:
                Color.secondary.opacity(0.12)
            }
        }
    }
}

struct SearchInputField: UIViewRepresentable {
    let text: String
    let placeholder: String
    let selectionName: String
    let desiredFocus: Bool
    let retainFocusWhenBlurred: Bool
    let canSubmit: Bool
    let onTextChange: (String, String) -> Void
    let onSubmit: () -> Void
    let onBackspaceOnEmpty: () -> Void
    let onMoveFocusUp: () -> Void
    let onMoveFocusDown: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(
            retainFocusWhenBlurred: retainFocusWhenBlurred,
            onTextChange: onTextChange,
            onSubmit: onSubmit,
            onBackspaceOnEmpty: onBackspaceOnEmpty,
            onMoveFocusUp: onMoveFocusUp,
            onMoveFocusDown: onMoveFocusDown
        )
    }

    func makeUIView(context: Context) -> BackspaceAwareTextField {
        let textField = BackspaceAwareTextField()
        textField.delegate = context.coordinator
        textField.placeholder = placeholder
        textField.autocapitalizationType = .none
        textField.autocorrectionType = .no
        textField.spellCheckingType = .no
        textField.smartDashesType = .no
        textField.smartQuotesType = .no
        textField.borderStyle = .none
        textField.backgroundColor = .clear
        textField.font = .preferredFont(forTextStyle: .body)
        textField.adjustsFontForContentSizeCategory = true
        applyVisibleTextStyling(to: textField)
        textField.clearButtonMode = .never
        textField.backspaceDelegate = context.coordinator
        textField.mutationHandler = { [weak coordinator = context.coordinator] field in
            coordinator?.textFieldDidMutate(field)
        }
        textField.addTarget(
            context.coordinator,
            action: #selector(Coordinator.textDidChange(_:)),
            for: .editingChanged
        )
        textField.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        textField.setContentHuggingPriority(.defaultLow, for: .horizontal)
        return textField
    }

    func updateUIView(_ uiView: BackspaceAwareTextField, context: Context) {
        context.coordinator.onTextChange = onTextChange
        context.coordinator.onSubmit = onSubmit
        context.coordinator.onBackspaceOnEmpty = onBackspaceOnEmpty
        context.coordinator.onMoveFocusUp = onMoveFocusUp
        context.coordinator.onMoveFocusDown = onMoveFocusDown
        context.coordinator.retainFocusWhenBlurred = retainFocusWhenBlurred
        context.coordinator.desiredFocus = desiredFocus
        if uiView.text != text {
            uiView.text = text
        }
        applyVisibleTextStyling(to: uiView)
        uiView.returnKeyType = canSubmit ? .go : .search
        if uiView.placeholder != placeholder {
            uiView.placeholder = placeholder
        }
        uiView.attributedPlaceholder = NSAttributedString(
            string: placeholder,
            attributes: [
                .foregroundColor: UIColor.secondaryLabel.withAlphaComponent(0.7),
                .font: UIFont.preferredFont(forTextStyle: .body)
            ]
        )
        let selectionSignature = selectionName.lowercased() + "\u{1F}" + text
        if uiView.window != nil,
           uiView.markedTextRange == nil,
           context.coordinator.lastAppliedSelectionSignature != selectionSignature {
            context.coordinator.isApplyingSelection = true
            applySelection(selectionName, to: uiView)
            context.coordinator.lastAppliedSelectionSignature = selectionSignature
            context.coordinator.isApplyingSelection = false
        }
        syncFocus(uiView, coordinator: context.coordinator)
    }

    private func applyVisibleTextStyling(to textField: UITextField) {
        let font = UIFont.preferredFont(forTextStyle: .body)
        textField.font = font
        textField.textColor = .label
        textField.tintColor = .systemBlue
        textField.defaultTextAttributes = [
            .foregroundColor: UIColor.label,
            .font: font
        ]
    }

    private func applySelection(_ selectionName: String, to textField: UITextField) {
        guard textField.window != nil else { return }
        let beginning = textField.beginningOfDocument
        switch selectionName.lowercased() {
        case "cursoratstart":
            if let range = textField.textRange(from: beginning, to: beginning) {
                textField.selectedTextRange = range
            }
        case "selectall":
            if let end = textField.position(from: beginning, offset: text.count),
               let range = textField.textRange(from: beginning, to: end) {
                textField.selectedTextRange = range
            }
        default:
            if let end = textField.position(from: beginning, offset: text.count),
               let range = textField.textRange(from: end, to: end) {
                textField.selectedTextRange = range
            }
        }
    }

    private func syncFocus(_ textField: BackspaceAwareTextField, coordinator: Coordinator) {
        guard textField.window != nil else { return }
        if coordinator.desiredFocus {
            guard !textField.isFirstResponder else { return }
            DispatchQueue.main.async {
                if coordinator.desiredFocus, textField.window != nil {
                    textField.becomeFirstResponder()
                }
            }
        }
    }

    final class Coordinator: NSObject, UITextFieldDelegate, BackspaceAwareTextFieldDelegate {
        var desiredFocus: Bool = false
        var retainFocusWhenBlurred: Bool
        var onTextChange: (String, String) -> Void
        var onSubmit: () -> Void
        var onBackspaceOnEmpty: () -> Void
        var onMoveFocusUp: () -> Void
        var onMoveFocusDown: () -> Void
        var lastAppliedSelectionSignature: String?
        var isApplyingSelection = false

        init(
            retainFocusWhenBlurred: Bool,
            onTextChange: @escaping (String, String) -> Void,
            onSubmit: @escaping () -> Void,
            onBackspaceOnEmpty: @escaping () -> Void,
            onMoveFocusUp: @escaping () -> Void,
            onMoveFocusDown: @escaping () -> Void
        ) {
            self.retainFocusWhenBlurred = retainFocusWhenBlurred
            self.onTextChange = onTextChange
            self.onSubmit = onSubmit
            self.onBackspaceOnEmpty = onBackspaceOnEmpty
            self.onMoveFocusUp = onMoveFocusUp
            self.onMoveFocusDown = onMoveFocusDown
        }

        @objc func textDidChange(_ textField: UITextField) {
            let nextValue = textField.text ?? ""
            onTextChange(nextValue, selectionName(in: textField))
        }

        func textFieldDidMutate(_ textField: UITextField) {
            onTextChange(textField.text ?? "", selectionName(in: textField))
        }

        func textField(
            _ textField: UITextField,
            shouldChangeCharactersIn range: NSRange,
            replacementString string: String
        ) -> Bool {
            DispatchQueue.main.async { [weak self, weak textField] in
                guard let self, let textField else { return }
                self.onTextChange(textField.text ?? "", self.selectionName(in: textField))
            }
            return true
        }

        func textFieldDidEndEditing(_ textField: UITextField) {
            guard retainFocusWhenBlurred, desiredFocus else { return }
            DispatchQueue.main.async {
                textField.becomeFirstResponder()
            }
        }

        func textFieldShouldReturn(_ textField: UITextField) -> Bool {
            onSubmit()
            return false
        }

        func textFieldDidChangeSelection(_ textField: UITextField) {
            guard !isApplyingSelection else { return }
            onTextChange(textField.text ?? "", selectionName(in: textField))
        }

        func textFieldDidBackspaceOnEmpty() {
            onBackspaceOnEmpty()
        }

        func textFieldDidPressMoveFocusUp() {
            onMoveFocusUp()
        }

        func textFieldDidPressMoveFocusDown() {
            onMoveFocusDown()
        }

        private func selectionName(in textField: UITextField) -> String {
            guard let selectedRange = textField.selectedTextRange else {
                return "cursorAtEnd"
            }
            let beginning = textField.beginningOfDocument
            let start = textField.offset(from: beginning, to: selectedRange.start)
            let end = textField.offset(from: beginning, to: selectedRange.end)
            let textLength = textField.text?.count ?? 0
            if textLength == 0 {
                return "cursorAtEnd"
            }
            if start == 0, end == 0 {
                return "cursorAtStart"
            }
            if start == 0, end == textLength, textLength > 0 {
                return "selectAll"
            }
            return "cursorAtEnd"
        }
    }
}

protocol BackspaceAwareTextFieldDelegate: AnyObject {
    func textFieldDidBackspaceOnEmpty()
    func textFieldDidPressMoveFocusUp()
    func textFieldDidPressMoveFocusDown()
}

final class BackspaceAwareTextField: UITextField {
    private let contentInsets = UIEdgeInsets(top: 6, left: 0, bottom: 6, right: 0)

    weak var backspaceDelegate: BackspaceAwareTextFieldDelegate?
    var mutationHandler: ((BackspaceAwareTextField) -> Void)?

    override var keyCommands: [UIKeyCommand]? {
        [
            UIKeyCommand(
                input: UIKeyCommand.inputUpArrow,
                modifierFlags: [],
                action: #selector(handleMoveFocusUp)
            ),
            UIKeyCommand(
                input: UIKeyCommand.inputDownArrow,
                modifierFlags: [],
                action: #selector(handleMoveFocusDown)
            )
        ]
    }

    override func deleteBackward() {
        if (text ?? "").isEmpty {
            backspaceDelegate?.textFieldDidBackspaceOnEmpty()
        }
        super.deleteBackward()
        mutationHandler?(self)
    }

    override func insertText(_ text: String) {
        super.insertText(text)
        mutationHandler?(self)
    }

    override func paste(_ sender: Any?) {
        super.paste(sender)
        mutationHandler?(self)
    }

    @objc private func handleMoveFocusUp() {
        backspaceDelegate?.textFieldDidPressMoveFocusUp()
    }

    @objc private func handleMoveFocusDown() {
        backspaceDelegate?.textFieldDidPressMoveFocusDown()
    }

    override func textRect(forBounds bounds: CGRect) -> CGRect {
        insetBounds(bounds)
    }

    override func editingRect(forBounds bounds: CGRect) -> CGRect {
        insetBounds(bounds)
    }

    override func placeholderRect(forBounds bounds: CGRect) -> CGRect {
        insetBounds(bounds)
    }

    private func insetBounds(_ bounds: CGRect) -> CGRect {
        bounds.inset(by: contentInsets)
    }
}

struct RaydroidGlassSurface: ViewModifier {
    let cornerRadius: CGFloat
    let interactive: Bool

    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(
                    interactive ? .regular.interactive() : .regular,
                    in: .rect(cornerRadius: cornerRadius)
                )
        } else {
            content
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: cornerRadius))
                .overlay {
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .stroke(Color(uiColor: .separator).opacity(0.32), lineWidth: 1)
                }
        }
    }
}

extension View {
    func raydroidGlassSurface(cornerRadius: CGFloat, interactive: Bool = false) -> some View {
        modifier(RaydroidGlassSurface(cornerRadius: cornerRadius, interactive: interactive))
    }

    func contextMenuAnchor(sourceId: String?) -> some View {
        background(
            GeometryReader { proxy in
                Color.clear.preference(
                    key: ContextMenuAnchorPreferenceKey.self,
                    value: sourceId.map { [$0: proxy.frame(in: .named(ContextMenuCoordinateSpace.name))] } ?? [:]
                )
            }
        )
    }

    func contextMenuLayer(
        showsContextMenu: Bool,
        isActive: Bool,
        blurRadius: CGFloat = 12,
        inactiveOpacity: Double = 0.5
    ) -> some View {
        modifier(
            ContextMenuLayerModifier(
                showsContextMenu: showsContextMenu,
                isActive: isActive,
                blurRadius: blurRadius,
                inactiveOpacity: inactiveOpacity
            )
        )
    }

    func contextMenuPressable(
        sourceId: String?,
        isContextMenuPresented: Bool,
        isContextMenuActive: Bool,
        onTap: @escaping () -> Void,
        onLongPress: @escaping () -> Void
    ) -> some View {
        modifier(
            ContextMenuPressableModifier(
                sourceId: sourceId,
                isContextMenuPresented: isContextMenuPresented,
                isContextMenuActive: isContextMenuActive,
                onTap: onTap,
                onLongPress: onLongPress
            )
        )
    }
}

struct RaydroidGlassButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            configuration.label
                .buttonStyle(.glass)
        } else {
            configuration.label
                .foregroundStyle(.primary)
                .background(.thinMaterial, in: Circle())
                .opacity(configuration.isPressed ? 0.72 : 1)
        }
    }
}

extension ButtonStyle where Self == RaydroidGlassButtonStyle {
    static var raydroidGlass: RaydroidGlassButtonStyle { RaydroidGlassButtonStyle() }
}

struct RaydroidLiquidGlassProminentButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            configuration.label
                .buttonStyle(.glassProminent)
        } else {
            configuration.label
                .foregroundStyle(Color.accentColor)
                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .opacity(configuration.isPressed ? 0.72 : 1)
        }
    }
}

extension ButtonStyle where Self == RaydroidLiquidGlassProminentButtonStyle {
    static var raydroidLiquidGlassProminent: RaydroidLiquidGlassProminentButtonStyle {
        RaydroidLiquidGlassProminentButtonStyle()
    }
}

struct RaydroidLiquidGlassAccessoryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            configuration.label
                .buttonStyle(.glass)
        } else {
            configuration.label
                .foregroundStyle(.primary)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .opacity(configuration.isPressed ? 0.72 : 1)
        }
    }
}

extension ButtonStyle where Self == RaydroidLiquidGlassAccessoryButtonStyle {
    static var raydroidLiquidGlassAccessory: RaydroidLiquidGlassAccessoryButtonStyle {
        RaydroidLiquidGlassAccessoryButtonStyle()
    }
}

func pluginFont(_ name: String) -> Font {
    switch name.lowercased() {
    case "extrasmall":
        return .caption2
    case "small":
        return .subheadline
    case "large":
        return .headline
    case "extralarge":
        return .title3
    default:
        return .body
    }
}

func pluginColor(_ name: String) -> Color {
    switch name.lowercased() {
    case "primary":
        return .accentColor
    case "primarycontainer":
        return Color.accentColor.opacity(0.18)
    case "onprimary":
        return .white
    case "onprimarycontainer":
        return .accentColor
    case "secondary":
        return .secondary
    case "onsecondary":
        return .white
    case "secondarycontainer":
        return Color(uiColor: .secondarySystemBackground)
    case "onsecondarycontainer":
        return .primary
    case "error":
        return .red
    case "errorcontainer":
        return Color.red.opacity(0.14)
    case "onerror":
        return .white
    case "onerrorcontainer":
        return .red
    case "tertiarycontainer":
        return Color.orange.opacity(0.18)
    case "ontertiary":
        return .white
    case "ontertiarycontainer":
        return .orange
    case "surface", "surfacedim":
        return Color(uiColor: .systemBackground)
    case "onsurface":
        return .primary
    case "onsurfacevariant", "outlinevariant":
        return .secondary
    case "outline":
        return Color(uiColor: .separator)
    case "tertiary":
        return .orange
    case "surfacebright", "surfacecontainer", "surfacecontainerlow", "surfacecontainerhigh", "surfacecontainerhighest", "surfacecontainerlowest":
        return Color(uiColor: .secondarySystemBackground)
    case "inverseonsurface":
        return Color(uiColor: .systemBackground)
    case "inversesurface":
        return .primary
    case "inverseprimary":
        return .accentColor
    case "scrim":
        return .black.opacity(0.35)
    case "primaryfixed", "primaryfixeddim":
        return Color.accentColor.opacity(0.18)
    case "onprimaryfixed", "onprimaryfixedvariant":
        return .accentColor
    case "secondaryfixed", "secondaryfixeddim":
        return Color(uiColor: .secondarySystemBackground)
    case "onsecondaryfixed", "onsecondaryfixedvariant":
        return .primary
    case "tertiaryfixed", "tertiaryfixeddim":
        return Color.orange.opacity(0.18)
    case "ontertiaryfixed", "ontertiaryfixedvariant":
        return .orange
    case "transparent":
        return .clear
    default:
        return .primary
    }
}

func pluginSpacing(_ name: String) -> CGFloat {
    switch name.lowercased() {
    case "zero":
        return 0
    case "minimal":
        return 2
    case "border":
        return 4
    case "extrasmall":
        return 6
    case "small":
        return 8
    case "large":
        return 16
    case "extralarge":
        return 20
    default:
        return 12
    }
}

func pluginIconSize(_ name: String) -> CGFloat {
    switch name.lowercased() {
    case "extrasmall":
        return 12
    case "small":
        return 16
    case "large":
        return 24
    case "extralarge":
        return 30
    default:
        return 20
    }
}

func pluginCornerRadius(_ name: String) -> CGFloat {
    switch name.lowercased() {
    case "extrasmall":
        return 4
    case "small":
        return 8
    case "medium":
        return 12
    case "large":
        return 16
    case "extralarge":
        return 22
    case "full":
        return 999
    default:
        return 0
    }
}

func pluginGridAspectRatio(_ name: String) -> CGFloat {
    switch name.lowercased() {
    case "threetotwo":
        return 3.0 / 2.0
    case "twotothree":
        return 2.0 / 3.0
    case "fourtothree":
        return 4.0 / 3.0
    case "threetofour":
        return 3.0 / 4.0
    case "sixteentonine":
        return 16.0 / 9.0
    case "ninetosixteen":
        return 9.0 / 16.0
    default:
        return 1
    }
}

func pluginBoxAlignment(_ name: String) -> Alignment {
    switch name.lowercased() {
    case "topstart":
        return .topLeading
    case "topcenter":
        return .top
    case "topend":
        return .topTrailing
    case "centerstart":
        return .leading
    case "centerend":
        return .trailing
    case "bottomstart":
        return .bottomLeading
    case "bottomcenter":
        return .bottom
    case "bottomend":
        return .bottomTrailing
    default:
        return .center
    }
}

func sfSymbol(for builtinName: String) -> String {
    switch builtinName {
    case "Clear":
        return "xmark.circle"
    case "Calculate":
        return "function"
    case "Check":
        return "checkmark"
    case "Delete":
        return "trash"
    case "Edit":
        return "pencil"
    case "ArrowRight", "ArrowForward":
        return "arrow.right"
    case "ArrowDropUp":
        return "chevron.up"
    case "GridView":
        return "square.grid.2x2"
    case "LocationCity":
        return "building.2"
    case "Save":
        return "square.and.arrow.down"
    case "Search":
        return "magnifyingglass"
    case "StickyNote2":
        return "note.text"
    case "Tune":
        return "slider.horizontal.3"
    case "Article":
        return "doc.text"
    case "Thunderstorm":
        return "cloud.bolt.rain"
    case "WaterDrop":
        return "drop"
    case "AcUnit":
        return "snowflake"
    case "WbCloudy":
        return "cloud"
    case "DeviceThermostat":
        return "thermometer"
    case "WbSunny":
        return "sun.max"
    case "Help", "HelpOutline":
        return "questionmark.circle"
    default:
        return "questionmark.circle"
    }
}

func imageFromBase64(_ string: String) -> UIImage? {
    guard let data = Data(base64Encoded: string) else { return nil }
    return UIImage(data: data)
}

let isoDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.calendar = Calendar(identifier: .gregorian)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter
}()

#Preview("Background") {
    RaydroidBackground()
}

#Preview("Markdown Text") {
    MarkdownText(markdown: "Use **bold**, _italic_, and `code` in plugin output.")
        .padding()
}

#Preview("Highlighted Text") {
    HighlightedText(
        text: "Calculator command",
        matches: [HighlightMatch(start: 0, end: 3), HighlightMatch(start: 11, end: 17)]
    )
    .padding()
}

#Preview("Flow Layout") {
    FlowLayout(items: [
        PreviewChip(id: "one", text: "search"),
        PreviewChip(id: "two", text: "plugin"),
        PreviewChip(id: "three", text: "swiftui")
    ]) { tag in
        Text(tag.text)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color.secondary.opacity(0.12), in: Capsule())
    }
    .padding()
}

#Preview("Plugin Icon") {
    PluginIconView(
        asset: .builtinName("Calculate"),
        tint: .accentColor,
        size: 28
    )
    .padding()
}

#Preview("Plugin Image") {
    PluginImageView(asset: .builtinName("GridView"))
        .frame(width: 180, height: 120)
        .background(Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
        .padding()
}

#Preview("Glass Buttons") {
    VStack(spacing: 16) {
        Button("Refresh") {}
            .buttonStyle(.raydroidGlass)
        Button("Primary") {}
            .buttonStyle(.raydroidLiquidGlassProminent)
        Button("Accessory") {}
            .buttonStyle(.raydroidLiquidGlassAccessory)
    }
    .padding()
}

private struct PreviewChip: Identifiable {
    let id: String
    let text: String
}
