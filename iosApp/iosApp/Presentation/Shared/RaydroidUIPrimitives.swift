import SwiftUI
import UIKit

struct HighlightMatch {
    let start: Int
    let end: Int
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
    @Binding var text: String
    let placeholder: String
    let selectionName: String
    @Binding var isFocused: Bool
    let canSubmit: Bool
    let onSubmit: () -> Void
    let onBackspaceOnEmpty: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(
            text: $text,
            isFocused: $isFocused,
            onSubmit: onSubmit,
            onBackspaceOnEmpty: onBackspaceOnEmpty
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
        textField.textColor = .label
        textField.tintColor = .systemBlue
        textField.clearButtonMode = .whileEditing
        textField.backspaceDelegate = context.coordinator
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
        context.coordinator.onSubmit = onSubmit
        context.coordinator.onBackspaceOnEmpty = onBackspaceOnEmpty
        if uiView.text != text {
            uiView.text = text
        }
        uiView.textColor = .label
        uiView.tintColor = .systemBlue
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
        if isFocused {
            if uiView.window != nil, !uiView.isFirstResponder {
                uiView.becomeFirstResponder()
            }
        } else if uiView.isFirstResponder {
            uiView.resignFirstResponder()
        }
        let selectionSignature = selectionName.lowercased() + "\u{1F}" + text
        if uiView.window != nil,
           uiView.markedTextRange == nil,
           context.coordinator.lastAppliedSelectionSignature != selectionSignature {
            applySelection(selectionName, to: uiView)
            context.coordinator.lastAppliedSelectionSignature = selectionSignature
        }
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

    final class Coordinator: NSObject, UITextFieldDelegate, BackspaceAwareTextFieldDelegate {
        @Binding var text: String
        @Binding var isFocused: Bool
        var onSubmit: () -> Void
        var onBackspaceOnEmpty: () -> Void
        var lastAppliedSelectionSignature: String?

        init(
            text: Binding<String>,
            isFocused: Binding<Bool>,
            onSubmit: @escaping () -> Void,
            onBackspaceOnEmpty: @escaping () -> Void
        ) {
            _text = text
            _isFocused = isFocused
            self.onSubmit = onSubmit
            self.onBackspaceOnEmpty = onBackspaceOnEmpty
        }

        @objc func textDidChange(_ textField: UITextField) {
            let nextValue = textField.text ?? ""
            if text != nextValue {
                text = nextValue
            }
        }

        func textFieldDidBeginEditing(_ textField: UITextField) {
            if !isFocused {
                isFocused = true
            }
        }

        func textFieldDidEndEditing(_ textField: UITextField) {
            if isFocused {
                isFocused = false
            }
        }

        func textFieldShouldReturn(_ textField: UITextField) -> Bool {
            onSubmit()
            return false
        }

        func textFieldDidBackspaceOnEmpty() {
            onBackspaceOnEmpty()
        }
    }
}

protocol BackspaceAwareTextFieldDelegate: AnyObject {
    func textFieldDidBackspaceOnEmpty()
}

final class BackspaceAwareTextField: UITextField {
    override var intrinsicContentSize: CGSize {
        let size = super.intrinsicContentSize
        return CGSize(width: size.width, height: 30)
    }

    weak var backspaceDelegate: BackspaceAwareTextFieldDelegate?

    override func deleteBackward() {
        if (text ?? "").isEmpty {
            backspaceDelegate?.textFieldDidBackspaceOnEmpty()
        }
        super.deleteBackward()
    }

    override func textRect(forBounds bounds: CGRect) -> CGRect {
        centeredTextRect(for: bounds)
    }

    override func editingRect(forBounds bounds: CGRect) -> CGRect {
        centeredTextRect(for: bounds)
    }

    override func placeholderRect(forBounds bounds: CGRect) -> CGRect {
        centeredTextRect(for: bounds)
    }

    private func centeredTextRect(for bounds: CGRect) -> CGRect {
        let targetHeight = min(bounds.height, 30)
        let originY = bounds.origin.y + max(0, (bounds.height - targetHeight) / 2)
        return CGRect(
            x: bounds.origin.x,
            y: originY,
            width: bounds.width,
            height: targetHeight
        )
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
                        .stroke(.white.opacity(0.24), lineWidth: 1)
                }
        }
    }
}

extension View {
    func raydroidGlassSurface(cornerRadius: CGFloat, interactive: Bool = false) -> some View {
        modifier(RaydroidGlassSurface(cornerRadius: cornerRadius, interactive: interactive))
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
    case "ArrowRight", "ArrowForward":
        return "arrow.right"
    case "ArrowDropUp":
        return "chevron.up"
    case "GridView":
        return "square.grid.2x2"
    case "Tune":
        return "slider.horizontal.3"
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
