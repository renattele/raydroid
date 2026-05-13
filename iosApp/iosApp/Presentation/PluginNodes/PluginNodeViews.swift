import SwiftUI

struct PluginNodeListView: View {
    let nodes: [PluginNodeViewData]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(nodes) { node in
                PluginNodeView(node: node)
            }
        }
    }
}

struct PluginNodeView: View {
    let node: PluginNodeViewData

    var body: some View {
        switch node {
        case .text(let data):
            Text(data.text)
                .font(pluginFont(data.fontName))
                .foregroundStyle(pluginColor(data.colorName))
                .frame(maxWidth: .infinity, alignment: .leading)
        case .icon(let data):
            let size = pluginIconSize(data.sizeName)
            PluginIconView(
                asset: data.asset,
                tint: data.tintName.isEmpty ? nil : pluginColor(data.tintName),
                size: size
            )
            .frame(width: size, height: size)
        case .image(let data):
            PluginImageView(asset: data.asset)
                .frame(
                    width: data.width.map(CGFloat.init),
                    height: data.height.map(CGFloat.init)
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius: pluginCornerRadius(data.shapeName)
                    )
                )
        case .box(let data):
            ZStack(alignment: pluginBoxAlignment(data.alignmentName)) {
                PluginNodeListView(nodes: data.children)
            }
        case .orientedBox(let data):
            if data.orientationName.lowercased().contains("horizontal") {
                HStack(alignment: .center, spacing: pluginSpacing(data.spacingName)) {
                    ForEach(data.children) { child in
                        PluginNodeView(node: child)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: pluginSpacing(data.spacingName)) {
                    ForEach(data.children) { child in
                        PluginNodeView(node: child)
                    }
                }
            }
        case .detail(let data):
            VStack(alignment: .leading, spacing: 12) {
                if !data.title.isEmpty {
                    Text(data.title)
                        .font(.headline)
                }
                MarkdownText(markdown: data.markdown)
                ForEach(data.metadata) { item in
                    DetailMetadataView(item: item)
                }
            }
            .padding(14)
            .raydroidGlassSurface(cornerRadius: 18, interactive: false)
        case .list(let data):
            ListNodeSectionView(data: data)
        case .grid(let data):
            GridNodeSectionView(data: data)
        case .form(let data):
            FormNodeView(data: data)
        case .unsupported(let data):
            UnsupportedNodeView(data: data)
        }
    }
}

private struct ListNodeSectionView: View {
    let data: ListNodeViewData

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if data.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, alignment: .center)
            } else if visibleItemCount == 0, let emptyState = data.emptyState {
                EmptyStateView(data: emptyState)
            } else {
                ForEach(data.sections) { section in
                    PluginListSectionView(data: section)
                }
            }
        }
    }

    private var visibleItemCount: Int {
        data.sections.reduce(0) { partialResult, section in
            partialResult + section.items.count
        }
    }
}

private struct GridNodeSectionView: View {
    let data: GridNodeViewData

    var body: some View {
        let columns = Array(
            repeating: GridItem(.flexible(), spacing: 10),
            count: data.columnCount
        )
        VStack(alignment: .leading, spacing: 12) {
            if data.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, alignment: .center)
            } else if visibleItemCount == 0, let emptyState = data.emptyState {
                EmptyStateView(data: emptyState)
            } else {
                ForEach(data.sections) { section in
                    if !section.title.isEmpty {
                        Text(section.title)
                            .font(.headline)
                    }
                    LazyVGrid(columns: columns, spacing: 10) {
                        ForEach(section.items) { item in
                            GridItemView(
                                data: item,
                                aspectRatioName: data.aspectRatioName
                            )
                        }
                    }
                }
            }
        }
    }

    private var visibleItemCount: Int {
        data.sections.reduce(0) { partialResult, section in
            partialResult + section.items.count
        }
    }
}

private struct PluginListSectionView: View {
    let data: PluginListSectionViewData

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if !data.title.isEmpty {
                Text(data.title)
                    .font(.headline)
            }
            ForEach(data.items) { item in
                PluginListItemView(data: item)
            }
        }
    }
}

private struct PluginListItemView: View {
    let data: PluginListItemViewData

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 12) {
                PluginIconView(
                    asset: data.iconAsset,
                    tint: .primary,
                    size: 18
                )
                .frame(width: 22, height: 22)
                VStack(alignment: .leading, spacing: 4) {
                    Text(data.title)
                        .font(.headline)
                    if !data.subtitle.isEmpty {
                        Text(data.subtitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                if data.showsContextButton, let onShowContextActions = data.onShowContextActions {
                    Button(action: onShowContextActions) {
                        Image(systemName: "ellipsis.circle")
                            .font(.system(size: 18))
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.secondary)
                }
            }

            if !data.content.isEmpty {
                PluginNodeListView(nodes: data.content)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(data.isFocused ? Color.accentColor.opacity(0.12) : Color.secondary.opacity(0.08))
        )
        .overlay {
            RoundedRectangle(cornerRadius: 16)
                .stroke(data.isFocused ? Color.accentColor.opacity(0.3) : Color.clear, lineWidth: 1)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: data.onTap)
        .onLongPressGesture {
            data.onShowContextActions?()
        }
    }
}

private struct GridItemView: View {
    let data: PluginGridItemViewData
    let aspectRatioName: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            PluginImageView(asset: data.imageAsset)
                .frame(maxWidth: .infinity)
                .aspectRatio(pluginGridAspectRatio(aspectRatioName), contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            Text(data.title)
                .font(.headline)
            if !data.subtitle.isEmpty {
                Text(data.subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(data.isFocused ? Color.accentColor.opacity(0.12) : Color.secondary.opacity(0.08))
        )
        .overlay {
            RoundedRectangle(cornerRadius: 18)
                .stroke(data.isFocused ? Color.accentColor.opacity(0.3) : Color.clear, lineWidth: 1)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: data.onTap)
        .onLongPressGesture {
            data.onShowContextActions?()
        }
    }
}

private struct EmptyStateView: View {
    let data: EmptyStateViewData

    var body: some View {
        VStack(spacing: 8) {
            PluginIconView(asset: data.iconAsset, tint: .secondary, size: 24)
                .frame(width: 28, height: 28)
            Text(data.title)
                .font(.headline)
            if !data.description.isEmpty {
                Text(data.description)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .raydroidGlassSurface(cornerRadius: 18, interactive: false)
    }
}

private struct DetailMetadataView: View {
    let item: DetailMetadataViewData

    var body: some View {
        switch item {
        case .tagList(let data):
            VStack(alignment: .leading, spacing: 8) {
                if !data.title.isEmpty {
                    Text(data.title)
                        .font(.subheadline.weight(.medium))
                }
                FlowLayout(items: data.tags) { tag in
                    Text(tag.text)
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color.secondary.opacity(0.12), in: Capsule())
                }
            }
        case .link(let data):
            VStack(alignment: .leading, spacing: 4) {
                if !data.title.isEmpty {
                    Text(data.title)
                        .font(.subheadline.weight(.medium))
                }
                if let url = URL(string: data.target), !data.target.isEmpty {
                    Link(data.text, destination: url)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                } else {
                    Text(data.text)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        case .label(let data):
            HStack(spacing: 8) {
                PluginIconView(asset: data.iconAsset, tint: .secondary, size: 14)
                    .frame(width: 16, height: 16)
                Text(data.title)
                    .font(.subheadline.weight(.medium))
                if !data.text.isEmpty {
                    Text(data.text)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        case .separator:
            Divider()
        }
    }
}

private struct FormNodeView: View {
    let data: FormNodeViewData

    @State private var textValues: [String: String] = [:]
    @State private var booleanValues: [String: Bool] = [:]
    @State private var dateValues: [String: String?] = [:]
    @State private var activeDateFieldId: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if data.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, alignment: .center)
            }
            if !data.title.isEmpty {
                Text(data.title)
                    .font(.headline)
            }
            ForEach(data.fields) { field in
                fieldView(field)
            }
            if let submitTitle = data.submitTitle {
                Button {
                    data.onSubmit?(pluginValues())
                } label: {
                    Text(submitTitle)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!data.isSubmitEnabled || data.onSubmit == nil)
            }
        }
        .padding(14)
        .raydroidGlassSurface(cornerRadius: 18, interactive: false)
        .onAppear(perform: initializeDefaults)
    }

    @ViewBuilder
    private func fieldView(_ field: PluginFormFieldViewData) -> some View {
        switch field {
        case .text(let data):
            textFieldView(data)
        case .checkbox(let data):
            Toggle(
                data.title.isEmpty ? data.id : data.title,
                isOn: bindingBoolean(for: data.id, defaultValue: data.defaultValue)
            )
        case .dropdown(let data):
            dropdownFieldView(data)
        case .date(let data):
            dateFieldView(data)
        case .description(let data):
            Text(data.text)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        case .separator:
            Divider()
        }
    }

    @ViewBuilder
    private func textFieldView(_ data: TextFormFieldViewData) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if !data.title.isEmpty {
                Text(data.title)
                    .font(.subheadline.weight(.medium))
            }
            if data.isMultiline {
                TextEditor(text: bindingText(for: data.id, defaultValue: data.defaultValue))
                    .frame(minHeight: 80)
                    .padding(8)
                    .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
            } else if data.isPassword {
                SecureField(data.placeholder, text: bindingText(for: data.id, defaultValue: data.defaultValue))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
            } else {
                TextField(data.placeholder, text: bindingText(for: data.id, defaultValue: data.defaultValue))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    @ViewBuilder
    private func dropdownFieldView(_ data: DropdownFormFieldViewData) -> some View {
        let selectedValue = bindingText(for: data.id, defaultValue: data.defaultValue)
        let selectedOption = data.options.first { option in
            option.value == selectedValue.wrappedValue
        }
        VStack(alignment: .leading, spacing: 6) {
            if !data.title.isEmpty {
                Text(data.title)
                    .font(.subheadline.weight(.medium))
            }
            Menu {
                ForEach(data.options) { option in
                    Button {
                        selectedValue.wrappedValue = option.value
                    } label: {
                        Label(
                            option.title,
                            systemImage: sfSymbol(for: builtinName(from: option.iconAsset))
                        )
                    }
                }
            } label: {
                HStack {
                    Text(selectedOption?.title ?? "Select")
                        .foregroundStyle(.primary)
                    Spacer()
                    Image(systemName: "chevron.up.chevron.down")
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    @ViewBuilder
    private func dateFieldView(_ data: DateFormFieldViewData) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if !data.title.isEmpty {
                Text(data.title)
                    .font(.subheadline.weight(.medium))
            }
            Button {
                activeDateFieldId = data.id
            } label: {
                HStack {
                    Text(currentDateValue(for: data.id, defaultValue: data.defaultValue) ?? "Select date")
                    Spacer()
                    Image(systemName: "calendar")
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
        .sheet(
            isPresented: Binding(
                get: { activeDateFieldId == data.id },
                set: { if !$0 { activeDateFieldId = nil } }
            )
        ) {
            DatePickerSheet(
                initialValue: currentDateValue(for: data.id, defaultValue: data.defaultValue),
                onSave: { selected in
                    dateValues[data.id] = selected
                    activeDateFieldId = nil
                },
                onCancel: {
                    activeDateFieldId = nil
                }
            )
        }
    }

    private func initializeDefaults() {
        guard textValues.isEmpty, booleanValues.isEmpty, dateValues.isEmpty else { return }
        for field in data.fields {
            switch field {
            case .text(let data):
                textValues[data.id] = data.defaultValue
            case .checkbox(let data):
                booleanValues[data.id] = data.defaultValue
            case .dropdown(let data):
                textValues[data.id] = data.defaultValue
            case .date(let data):
                dateValues[data.id] = data.defaultValue
            case .description, .separator:
                break
            }
        }
    }

    private func pluginValues() -> [String: PluginFormValueDraft] {
        var values: [String: PluginFormValueDraft] = [:]
        for field in data.fields {
            switch field {
            case .text(let data):
                values[data.id] = .text(textValues[data.id] ?? data.defaultValue)
            case .checkbox(let data):
                values[data.id] = .boolean(booleanValues[data.id] ?? data.defaultValue)
            case .dropdown(let data):
                values[data.id] = .text(textValues[data.id] ?? data.defaultValue)
            case .date(let data):
                values[data.id] = .date(currentDateValue(for: data.id, defaultValue: data.defaultValue))
            case .description, .separator:
                break
            }
        }
        return values
    }

    private func currentDateValue(for id: String, defaultValue: String?) -> String? {
        flattened(dateValues[id]) ?? defaultValue
    }

    private func bindingText(for id: String, defaultValue: String) -> Binding<String> {
        Binding(
            get: { textValues[id] ?? defaultValue },
            set: { textValues[id] = $0 }
        )
    }

    private func bindingBoolean(for id: String, defaultValue: Bool) -> Binding<Bool> {
        Binding(
            get: { booleanValues[id] ?? defaultValue },
            set: { booleanValues[id] = $0 }
        )
    }
}

private struct DatePickerSheet: View {
    let onSave: (String?) -> Void
    let onCancel: () -> Void
    @State private var selectedDate: Date

    init(initialValue: String?, onSave: @escaping (String?) -> Void, onCancel: @escaping () -> Void) {
        self.onSave = onSave
        self.onCancel = onCancel
        _selectedDate = State(
            initialValue: initialValue.flatMap(isoDateFormatter.date(from:)) ?? .now
        )
    }

    var body: some View {
        NavigationStack {
            VStack {
                DatePicker(
                    "Date",
                    selection: $selectedDate,
                    displayedComponents: .date
                )
                .datePickerStyle(.graphical)
                .padding()
                Spacer()
            }
            .navigationTitle("Select date")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(isoDateFormatter.string(from: selectedDate))
                    }
                }
            }
        }
    }
}

private struct UnsupportedNodeView: View {
    let data: UnsupportedNodeViewData

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(data.title)
                .font(.headline)
            Text(data.subtitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .raydroidGlassSurface(cornerRadius: 18, interactive: false)
    }
}

private func builtinName(from asset: PluginAsset?) -> String {
    guard case .builtinName(let builtinName) = asset else {
        return "Help"
    }
    return builtinName
}

private func flattened<T>(_ value: T??) -> T? {
    switch value {
    case .some(.some(let wrapped)):
        return wrapped
    default:
        return nil
    }
}

#Preview("Plugin Node List") {
    ScrollView {
        PluginNodeListView(nodes: PreviewData.pluginNodes)
            .padding()
    }
    .background(RaydroidBackground())
}

#Preview("List Node") {
    PluginNodeView(node: .list(PreviewData.sampleListNode))
        .padding()
        .background(RaydroidBackground())
}

#Preview("Grid Node") {
    PluginNodeView(node: .grid(PreviewData.sampleGridNode))
        .padding()
        .background(RaydroidBackground())
}

#Preview("Form Node") {
    PluginNodeView(node: .form(PreviewData.sampleFormNode))
        .padding()
        .background(RaydroidBackground())
}
