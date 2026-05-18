import Foundation
import RaydroidShared

enum PluginActionStyle {
    case normal
    case destructive
}

struct PluginContextActionModel: Identifiable {
    let id: String
    let title: String
    let iconAsset: PluginAsset?
    let style: PluginActionStyle
}

enum PluginNodeViewData: Identifiable {
    case text(TextNodeViewData)
    case icon(IconNodeViewData)
    case image(ImageNodeViewData)
    case box(BoxNodeViewData)
    case orientedBox(OrientedBoxNodeViewData)
    case detail(DetailNodeViewData)
    case editableText(EditableTextNodeViewData)
    case list(ListNodeViewData)
    case grid(GridNodeViewData)
    case form(FormNodeViewData)
    case unsupported(UnsupportedNodeViewData)

    var id: String {
        switch self {
        case .text(let data): data.id
        case .icon(let data): data.id
        case .image(let data): data.id
        case .box(let data): data.id
        case .orientedBox(let data): data.id
        case .detail(let data): data.id
        case .editableText(let data): data.id
        case .list(let data): data.id
        case .grid(let data): data.id
        case .form(let data): data.id
        case .unsupported(let data): data.id
        }
    }
}

struct TextNodeViewData {
    let id: String
    let text: String
    let fontName: String
    let colorName: String
}

struct IconNodeViewData {
    let id: String
    let asset: PluginAsset?
    let tintName: String
    let sizeName: String
}

struct ImageNodeViewData {
    let id: String
    let asset: PluginAsset?
    let width: Int?
    let height: Int?
    let shapeName: String
}

struct BoxNodeViewData {
    let id: String
    let alignmentName: String
    let children: [PluginNodeViewData]
}

struct OrientedBoxNodeViewData {
    let id: String
    let orientationName: String
    let spacingName: String
    let children: [PluginNodeViewData]
}

struct DetailNodeViewData {
    let id: String
    let title: String
    let markdown: String
    let metadata: [DetailMetadataViewData]
}

struct EditableTextNodeViewData {
    let id: String
    let fieldId: String
    let value: String
    let selection: Int
    let displayValue: String
    let displayFormatterName: String
    let placeholder: String
    let isMultiline: Bool
    let maxLines: Int
    let autoScrollToEnd: Bool
    let onChange: ((String, Int) -> Void)?
}

enum DetailMetadataViewData: Identifiable {
    case label(LabelMetadataViewData)
    case link(LinkMetadataViewData)
    case tagList(TagListMetadataViewData)
    case separator(SeparatorMetadataViewData)

    var id: String {
        switch self {
        case .label(let data): data.id
        case .link(let data): data.id
        case .tagList(let data): data.id
        case .separator(let data): data.id
        }
    }
}

struct LabelMetadataViewData {
    let id: String
    let title: String
    let text: String
    let iconAsset: PluginAsset?
}

struct LinkMetadataViewData {
    let id: String
    let title: String
    let text: String
    let target: String
}

struct TagListMetadataViewData {
    let id: String
    let title: String
    let tags: [TagChipViewData]
}

struct TagChipViewData: Identifiable {
    let id: String
    let text: String
}

struct SeparatorMetadataViewData {
    let id: String
}

struct EmptyStateViewData {
    let id: String
    let iconAsset: PluginAsset?
    let title: String
    let description: String
}

enum ActionPanelHintModeModel {
    case full
    case menuOnly
    case hidden
}

enum FormSubmitStyleModel {
    case filled
    case tonal
}

struct ListNodeViewData {
    let id: String
    let isLoading: Bool
    let emptyState: EmptyStateViewData?
    let sections: [PluginListSectionViewData]
}

struct PluginListSectionViewData: Identifiable {
    let id: String
    let title: String
    let items: [PluginListItemViewData]
}

struct PluginListItemViewData: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let iconAsset: PluginAsset?
    let content: [PluginNodeViewData]
    let isFocused: Bool
    let contextSourceId: String?
    let isContextMenuPresented: Bool
    let isContextMenuActive: Bool
    let showsContextButton: Bool
    let onTap: () -> Void
    let onShowContextActions: (() -> Void)?
}

struct GridNodeViewData {
    let id: String
    let isLoading: Bool
    let emptyState: EmptyStateViewData?
    let columnCount: Int
    let aspectRatioName: String
    let sections: [PluginGridSectionViewData]
}

struct PluginGridSectionViewData: Identifiable {
    let id: String
    let title: String
    let items: [PluginGridItemViewData]
}

struct PluginGridItemViewData: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let imageAsset: PluginAsset?
    let isFocused: Bool
    let contextSourceId: String?
    let isContextMenuPresented: Bool
    let isContextMenuActive: Bool
    let onTap: () -> Void
    let onShowContextActions: (() -> Void)?
}

struct FormNodeViewData {
    let id: String
    let title: String
    let isLoading: Bool
    let fields: [PluginFormFieldViewData]
    let submitTitle: String?
    let submitIconAsset: PluginAsset?
    let submitStyle: FormSubmitStyleModel
    let isSubmitEnabledByPlugin: Bool
    let requireChanges: Bool
    let unchangedView: EmptyStateViewData?
    let onSubmit: (([String: PluginFormValueDraft]) -> Void)?
}

enum PluginFormFieldViewData: Identifiable {
    case text(TextFormFieldViewData)
    case checkbox(CheckboxFormFieldViewData)
    case dropdown(DropdownFormFieldViewData)
    case date(DateFormFieldViewData)
    case description(DescriptionFormFieldViewData)
    case separator(UnsupportedFormFieldViewData)

    var id: String {
        switch self {
        case .text(let data): data.id
        case .checkbox(let data): data.id
        case .dropdown(let data): data.id
        case .date(let data): data.id
        case .description(let data): data.id
        case .separator(let data): data.id
        }
    }
}

struct TextFormFieldViewData {
    let id: String
    let title: String
    let placeholder: String
    let defaultValue: String
    let isRequired: Bool
    let isPassword: Bool
    let isMultiline: Bool
}

struct CheckboxFormFieldViewData {
    let id: String
    let title: String
    let isRequired: Bool
    let defaultValue: Bool
}

struct DropdownFormFieldViewData {
    let id: String
    let title: String
    let options: [DropdownOptionViewData]
    let isRequired: Bool
    let defaultValue: String
}

struct DropdownOptionViewData: Identifiable {
    let id: String
    let value: String
    let title: String
    let iconAsset: PluginAsset?
}

struct DateFormFieldViewData {
    let id: String
    let title: String
    let isRequired: Bool
    let defaultValue: String?
}

struct DescriptionFormFieldViewData {
    let id: String
    let text: String
}

struct UnsupportedFormFieldViewData {
    let id: String
}

struct UnsupportedNodeViewData {
    let id: String
    let title: String
    let subtitle: String
}

@MainActor
struct PluginNodeMapper {
    let client: SearchViewModelClient
    let query: String
    let resultId: ApiSearchResultId?
    let focusedItemId: Any?
    let activeContextSourceId: String?
    let isContextMenuPresented: Bool

    init(
        client: SearchViewModelClient,
        query: String,
        resultId: ApiSearchResultId?,
        focusedItemId: Any?,
        activeContextSourceId: String? = nil,
        isContextMenuPresented: Bool = false
    ) {
        self.client = client
        self.query = query
        self.resultId = resultId
        self.focusedItemId = focusedItemId
        self.activeContextSourceId = activeContextSourceId
        self.isContextMenuPresented = isContextMenuPresented
    }

    func mapNodes(_ nodes: [AnyObject], path: String) -> [PluginNodeViewData] {
        nodes.enumerated().map { index, node in
            mapNode(node, path: "\(path).\(index)")
        }
    }

    private func mapNode(_ node: AnyObject, path: String) -> PluginNodeViewData {
        switch nodeKind(node) {
        case "text":
            return .text(
                TextNodeViewData(
                    id: path,
                    text: client.resolveText(nodeText(node)),
                    fontName: nodeFontSize(node),
                    colorName: nodeColor(node)
                )
            )
        case "icon":
            return .icon(
                IconNodeViewData(
                    id: path,
                    asset: client.resolveIcon(nodeIcon(node)),
                    tintName: nodeColor(node),
                    sizeName: nodeIconSize(node)
                )
            )
        case "image":
            return .image(
                ImageNodeViewData(
                    id: path,
                    asset: client.resolveImage(nodeImage(node)),
                    width: nodeImageWidth(node),
                    height: nodeImageHeight(node),
                    shapeName: nodeShape(node)
                )
            )
        case "box":
            return .box(
                BoxNodeViewData(
                    id: path,
                    alignmentName: nodeAlignment(node),
                    children: mapNodes(nodeChildren(node), path: path)
                )
            )
        case "orientedBox":
            return .orientedBox(
                OrientedBoxNodeViewData(
                    id: path,
                    orientationName: nodeOrientation(node),
                    spacingName: nodeSpacing(node),
                    children: mapNodes(nodeChildren(node), path: path)
                )
            )
        case "detail":
            return .detail(
                DetailNodeViewData(
                    id: path,
                    title: client.resolveText(detailNavigationTitle(node)),
                    markdown: detailMarkdown(node),
                    metadata: detailMetadata(node).enumerated().map { index, item in
                        mapMetadata(item, path: "\(path).metadata.\(index)")
                    }
                )
            )
        case "editableText":
            return .editableText(
                EditableTextNodeViewData(
                    id: path,
                    fieldId: editableTextId(node),
                    value: editableTextValue(node),
                    selection: editableTextSelection(node),
                    displayValue: editableTextDisplayValue(node),
                    displayFormatterName: editableTextDisplayFormatter(node),
                    placeholder: client.resolveText(editableTextPlaceholder(node)),
                    isMultiline: editableTextMultiline(node),
                    maxLines: editableTextMaxLines(node),
                    autoScrollToEnd: editableTextAutoScrollToEnd(node),
                    onChange: editableTextOnChange(node).map { callback in
                        { value, selection in
                            client.submitForm(
                                callback: callback,
                                values: [
                                    editableTextId(node): .text(value),
                                    "\(editableTextId(node)):selection": .text(String(selection))
                                ]
                            )
                        }
                    }
                )
            )
        case "list":
            return .list(mapListNode(node, path: path))
        case "grid":
            return .grid(mapGridNode(node, path: path))
        case "form":
            return .form(mapFormNode(node, path: path))
        default:
            return .unsupported(
                UnsupportedNodeViewData(
                    id: path,
                    title: "Unsupported node",
                    subtitle: runtimeClassName(node)
                )
            )
        }
    }

    private func mapMetadata(_ item: AnyObject, path: String) -> DetailMetadataViewData {
        switch detailMetadataKind(item) {
        case "label":
            return .label(
                LabelMetadataViewData(
                    id: path,
                    title: client.resolveText(detailMetadataTitle(item)),
                    text: client.resolveText(detailMetadataText(item)),
                    iconAsset: client.resolveIcon(detailMetadataIcon(item))
                )
            )
        case "link":
            return .link(
                LinkMetadataViewData(
                    id: path,
                    title: client.resolveText(detailMetadataTitle(item)),
                    text: client.resolveText(detailMetadataText(item)),
                    target: detailMetadataTarget(item)
                )
            )
        case "tagList":
            return .tagList(
                TagListMetadataViewData(
                    id: path,
                    title: client.resolveText(detailMetadataTitle(item)),
                    tags: detailMetadataTags(item).enumerated().map { index, tag in
                        TagChipViewData(
                            id: "\(path).tag.\(index)",
                            text: client.resolveText(detailTagText(tag))
                        )
                    }
                )
            )
        default:
            return .separator(SeparatorMetadataViewData(id: path))
        }
    }

    private func mapListNode(_ node: AnyObject, path: String) -> ListNodeViewData {
        let sections = listSections(node)
        return ListNodeViewData(
            id: path,
            isLoading: listIsLoading(node),
            emptyState: listEmptyView(node).map { emptyView in
                EmptyStateViewData(
                    id: "\(path).empty",
                    iconAsset: client.resolveIcon(emptyViewIcon(emptyView)),
                    title: client.resolveText(emptyViewTitle(emptyView)),
                    description: client.resolveText(emptyViewDescription(emptyView))
                )
            },
            sections: sections.enumerated().map { index, section in
                PluginListSectionViewData(
                    id: "\(path).section.\(index)",
                    title: client.resolveText(sectionTitle(section)),
                    items: filteredItems(in: section).enumerated().map { itemIndex, item in
                        mapListItem(
                            item,
                            path: "\(path).section.\(index).item.\(itemIndex)"
                        )
                    }
                )
            }
        )
    }

    private func mapGridNode(_ node: AnyObject, path: String) -> GridNodeViewData {
        let sections = listSections(node)
        return GridNodeViewData(
            id: path,
            isLoading: listIsLoading(node),
            emptyState: listEmptyView(node).map { emptyView in
                EmptyStateViewData(
                    id: "\(path).empty",
                    iconAsset: client.resolveIcon(emptyViewIcon(emptyView)),
                    title: client.resolveText(emptyViewTitle(emptyView)),
                    description: client.resolveText(emptyViewDescription(emptyView))
                )
            },
            columnCount: max(gridColumns(node) ?? 2, 1),
            aspectRatioName: gridAspectRatio(node),
            sections: sections.enumerated().map { index, section in
                PluginGridSectionViewData(
                    id: "\(path).section.\(index)",
                    title: client.resolveText(sectionTitle(section)),
                    items: filteredItems(in: section).enumerated().map { itemIndex, item in
                        mapGridItem(
                            item,
                            path: "\(path).section.\(index).item.\(itemIndex)"
                        )
                    }
                )
            }
        )
    }

    private func mapListItem(_ item: AnyObject, path: String) -> PluginListItemViewData {
        let modifier = listItemModifier(item)
        let actions = modifier?.actions ?? []
        let itemId = listItemId(item)
        let currentResultId = resultId
        let contextSourceId = itemId.map { "plugin-item:\(commandItemIdValue($0))" } ?? path
        return PluginListItemViewData(
            id: path,
            title: client.resolveText(listItemTitle(item)),
            subtitle: client.resolveText(listItemSubtitle(item)),
            iconAsset: client.resolveIcon(listItemIcon(item)),
            content: mapNodes(listItemContent(item), path: "\(path).content"),
            isFocused: commandItemIdValue(itemId) == commandItemIdValue(focusedItemId),
            contextSourceId: contextSourceId,
            isContextMenuPresented: isContextMenuPresented,
            isContextMenuActive: activeContextSourceId == contextSourceId,
            showsContextButton: currentResultId != nil && !actions.isEmpty,
            onTap: {
                if let itemId {
                    client.focusPluginItem(itemId)
                }
                if let currentResultId,
                   let primaryAction = actions.first(where: \.primary) ?? actions.first {
                    client.enterCallback(
                        resultId: currentResultId,
                        callback: primaryAction.callback,
                        updateUsage: false
                    )
                } else if let itemId {
                    client.enterPluginItem(itemId)
                }
            },
            onShowContextActions: (currentResultId != nil && !actions.isEmpty) ? {
                {
                    client.showContextActions(
                        resultId: currentResultId!,
                        sourceId: contextSourceId,
                        actions: actions
                    )
                }
            }() : nil
        )
    }

    private func mapGridItem(_ item: AnyObject, path: String) -> PluginGridItemViewData {
        let actions = listItemModifier(item)?.actions ?? []
        let itemId = listItemId(item)
        let currentResultId = resultId
        let contextSourceId = itemId.map { "plugin-item:\(commandItemIdValue($0))" } ?? path
        return PluginGridItemViewData(
            id: path,
            title: client.resolveText(listItemTitle(item)),
            subtitle: client.resolveText(listItemSubtitle(item)),
            imageAsset: client.resolveImage(gridItemImage(item)),
            isFocused: commandItemIdValue(itemId) == commandItemIdValue(focusedItemId),
            contextSourceId: contextSourceId,
            isContextMenuPresented: isContextMenuPresented,
            isContextMenuActive: activeContextSourceId == contextSourceId,
            onTap: {
                if let itemId {
                    client.focusPluginItem(itemId)
                }
                if let currentResultId,
                   let primaryAction = actions.first(where: \.primary) ?? actions.first {
                    client.enterCallback(
                        resultId: currentResultId,
                        callback: primaryAction.callback,
                        updateUsage: false
                    )
                } else if let itemId {
                    client.enterPluginItem(itemId)
                }
            },
            onShowContextActions: (currentResultId != nil && !actions.isEmpty) ? {
                {
                    client.showContextActions(
                        resultId: currentResultId!,
                        sourceId: contextSourceId,
                        actions: actions
                    )
                }
            }() : nil
        )
    }

    private func mapFormNode(_ node: AnyObject, path: String) -> FormNodeViewData {
        let fields = formFields(node)
        let submit = formSubmit(node)
        return FormNodeViewData(
            id: path,
            title: client.resolveText(formNavigationTitle(node)),
            isLoading: listIsLoading(node),
            fields: fields.enumerated().map { index, field in
                mapFormField(field, path: "\(path).field.\(index)")
            },
            submitTitle: submit.map { client.resolveText(formSubmitTitle($0)) },
            submitIconAsset: submit.flatMap { client.resolveIcon(formSubmitIcon($0)) },
            submitStyle: submit.map(formSubmitStyleModel) ?? .filled,
            isSubmitEnabledByPlugin: submit.map(formSubmitEnabled) ?? false,
            requireChanges: formRequireChanges(node),
            unchangedView: formUnchangedView(node).map { emptyView in
                EmptyStateViewData(
                    id: "\(path).unchanged",
                    iconAsset: client.resolveIcon(emptyViewIcon(emptyView)),
                    title: client.resolveText(emptyViewTitle(emptyView)),
                    description: client.resolveText(emptyViewDescription(emptyView))
                )
            },
            onSubmit: submit.flatMap { submit in
                formSubmitCallback(submit).map { callback in
                    { values in
                        client.submitForm(callback: callback, values: values)
                    }
                }
            }
        )
    }

    private func mapFormField(_ field: AnyObject, path: String) -> PluginFormFieldViewData {
        let fieldId = formFieldId(field)
        switch formFieldKind(field) {
        case "textField":
            return .text(
                TextFormFieldViewData(
                    id: fieldId,
                    title: client.resolveText(formFieldTitle(field)),
                    placeholder: client.resolveText(formFieldPlaceholder(field)),
                    defaultValue: formFieldDefaultText(field),
                    isRequired: formFieldRequired(field),
                    isPassword: formFieldPassword(field),
                    isMultiline: formFieldMultiline(field)
                )
            )
        case "checkbox":
            return .checkbox(
                CheckboxFormFieldViewData(
                    id: fieldId,
                    title: client.resolveText(formFieldTitle(field)),
                    isRequired: formFieldRequired(field),
                    defaultValue: formFieldDefaultBoolean(field)
                )
            )
        case "dropdown":
            let options = formFieldOptions(field)
            return .dropdown(
                DropdownFormFieldViewData(
                    id: fieldId,
                    title: client.resolveText(formFieldTitle(field)),
                    options: options.enumerated().map { index, option in
                        DropdownOptionViewData(
                            id: "\(path).option.\(index)",
                            value: formOptionValue(option),
                            title: client.resolveText(formOptionTitle(option)),
                            iconAsset: client.resolveIcon(formOptionIcon(option))
                        )
                    },
                    isRequired: formFieldRequired(field),
                    defaultValue: formFieldDefaultOption(field)
                        ?? options.first.map(formOptionValue)
                        ?? ""
                )
            )
        case "datePicker":
            return .date(
                DateFormFieldViewData(
                    id: fieldId,
                    title: client.resolveText(formFieldTitle(field)),
                    isRequired: formFieldRequired(field),
                    defaultValue: formFieldDefaultDate(field)
                )
            )
        case "description":
            return .description(
                DescriptionFormFieldViewData(
                    id: fieldId,
                    text: client.resolveText(formFieldText(field))
                )
            )
        default:
            return .separator(UnsupportedFormFieldViewData(id: fieldId))
        }
    }

    private func filteredItems(in section: AnyObject) -> [AnyObject] {
        let items = sectionItems(section)
        guard !query.isEmpty else { return items }
        let needle = query.lowercased()
        return items.filter { item in
            let title = client.resolveText(listItemTitle(item))
            let subtitle = client.resolveText(listItemSubtitle(item))
            let keywords = listItemKeywords(item).joined(separator: " ")
            return title.lowercased().contains(needle)
                || subtitle.lowercased().contains(needle)
                || keywords.lowercased().contains(needle)
        }
    }
}

private func runtimeClassName(_ object: AnyObject) -> String {
    NSStringFromClass(type(of: object))
}

private func nodeKind(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeKind(node: node)
}

private func nodeChildren(_ object: AnyObject) -> [AnyObject] {
    guard let node = object as? ApiPluginRayNodeData else { return [] }
    return SearchInteropBridge.shared.nodeChildren(node: node).map { $0 as AnyObject }
}

private func nodeText(_ object: AnyObject) -> ApiPluginUiText? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.nodeText(node: node)
}

private func nodeFontSize(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeFontSize(node: node)
}

private func nodeColor(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeColor(node: node)
}

private func nodeIcon(_ object: AnyObject) -> ApiPluginIcon? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.nodeIcon(node: node)
}

private func nodeIconSize(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeIconSize(node: node)
}

private func nodeImage(_ object: AnyObject) -> ApiPluginImage? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.nodeImage(node: node)
}

private func nodeImageWidth(_ object: AnyObject) -> Int? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.nodeImageWidth(node: node)?.intValue
}

private func nodeImageHeight(_ object: AnyObject) -> Int? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.nodeImageHeight(node: node)?.intValue
}

private func nodeShape(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeShape(node: node)
}

private func nodeAlignment(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeAlignment(node: node)
}

private func nodeOrientation(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeOrientation(node: node)
}

private func nodeSpacing(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.nodeSpacing(node: node)
}

private func detailMarkdown(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.detailMarkdown(node: node)
}

private func detailNavigationTitle(_ object: AnyObject) -> ApiPluginUiText? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.detailNavigationTitle(node: node)
}

private func detailMetadata(_ object: AnyObject) -> [AnyObject] {
    guard let node = object as? ApiPluginRayNodeData else { return [] }
    return SearchInteropBridge.shared.detailMetadata(node: node).map { $0 as AnyObject }
}

private func editableTextId(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.editableTextId(node: node)
}

private func editableTextValue(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.editableTextValue(node: node)
}

private func editableTextSelection(_ object: AnyObject) -> Int {
    guard let node = object as? ApiPluginRayNodeData else { return 0 }
    return Int(SearchInteropBridge.shared.editableTextSelection(node: node))
}

private func editableTextDisplayValue(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.editableTextDisplayValue(node: node) ?? ""
}

private func editableTextDisplayFormatter(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.editableTextDisplayFormatter(node: node)
}

private func editableTextPlaceholder(_ object: AnyObject) -> ApiPluginUiText? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.editableTextPlaceholder(node: node)
}

private func editableTextMultiline(_ object: AnyObject) -> Bool {
    guard let node = object as? ApiPluginRayNodeData else { return true }
    return SearchInteropBridge.shared.editableTextMultiline(node: node)
}

private func editableTextMaxLines(_ object: AnyObject) -> Int {
    guard let node = object as? ApiPluginRayNodeData else { return 8 }
    return Int(SearchInteropBridge.shared.editableTextMaxLines(node: node))
}

private func editableTextAutoScrollToEnd(_ object: AnyObject) -> Bool {
    guard let node = object as? ApiPluginRayNodeData else { return false }
    return SearchInteropBridge.shared.editableTextAutoScrollToEnd(node: node)
}

private func editableTextOnChange(_ object: AnyObject) -> ApiPluginFormSubmitCallback? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.editableTextOnChange(node: node)
}

private func detailMetadataKind(_ object: AnyObject) -> String {
    SearchInteropBridge.shared.detailMetadataKind(item: object)
}

private func detailMetadataTitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.detailMetadataTitle(item: object)
}

private func detailMetadataText(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.detailMetadataText(item: object)
}

private func detailMetadataIcon(_ object: AnyObject) -> ApiPluginIcon? {
    SearchInteropBridge.shared.detailMetadataIcon(item: object)
}

private func detailMetadataTarget(_ object: AnyObject) -> String {
    SearchInteropBridge.shared.detailMetadataTarget(item: object)
}

private func detailMetadataTags(_ object: AnyObject) -> [AnyObject] {
    SearchInteropBridge.shared.detailMetadataTags(item: object).map { $0 as AnyObject }
}

private func detailTagText(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.detailTagText(tag: object)
}

private func listSections(_ object: AnyObject) -> [AnyObject] {
    guard let node = object as? ApiPluginRayNodeData else { return [] }
    return SearchInteropBridge.shared.listSections(node: node).map { $0 as AnyObject }
}

private func listEmptyView(_ object: AnyObject) -> AnyObject? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.listEmptyView(node: node) as AnyObject?
}

private func listIsLoading(_ object: AnyObject) -> Bool {
    guard let node = object as? ApiPluginRayNodeData else { return false }
    return SearchInteropBridge.shared.listIsLoading(node: node)
}

private func gridColumns(_ object: AnyObject) -> Int? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.gridColumns(node: node)?.intValue
}

private func gridAspectRatio(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "" }
    return SearchInteropBridge.shared.gridAspectRatio(node: node)
}

private func sectionTitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.sectionTitle(section: object)
}

private func sectionItems(_ object: AnyObject) -> [AnyObject] {
    SearchInteropBridge.shared.sectionItems(section: object).map { $0 as AnyObject }
}

private func listItemId(_ object: AnyObject) -> Any? {
    SearchInteropBridge.shared.listItemId(item: object)
}

private func listItemTitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.listItemTitle(item: object)
}

private func listItemSubtitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.listItemSubtitle(item: object)
}

private func listItemIcon(_ object: AnyObject) -> ApiPluginIcon? {
    SearchInteropBridge.shared.listItemIcon(item: object)
}

private func listItemKeywords(_ object: AnyObject) -> [String] {
    SearchInteropBridge.shared.listItemKeywords(item: object)
}

private func listItemModifier(_ object: AnyObject) -> ApiPluginRayModifier? {
    SearchInteropBridge.shared.listItemModifier(item: object)
}

private func listItemContent(_ object: AnyObject) -> [AnyObject] {
    SearchInteropBridge.shared.listItemContent(item: object).map { $0 as AnyObject }
}

private func gridItemImage(_ object: AnyObject) -> ApiPluginImage? {
    SearchInteropBridge.shared.gridItemImage(item: object)
}

private func emptyViewTitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.emptyViewTitle(emptyView: object)
}

private func emptyViewDescription(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.emptyViewDescription(emptyView: object)
}

private func emptyViewIcon(_ object: AnyObject) -> ApiPluginIcon? {
    SearchInteropBridge.shared.emptyViewIcon(emptyView: object)
}

private func formFieldKind(_ object: AnyObject) -> String {
    SearchInteropBridge.shared.formFieldKind(field: object)
}

private func formFields(_ object: AnyObject) -> [AnyObject] {
    guard let node = object as? ApiPluginRayNodeData else { return [] }
    return SearchInteropBridge.shared.formFields(node: node).map { $0 as AnyObject }
}

private func formSubmit(_ object: AnyObject) -> AnyObject? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.formSubmit(node: node) as AnyObject?
}

private func formNavigationTitle(_ object: AnyObject) -> ApiPluginUiText? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.formNavigationTitle(node: node)
}

private func formSubmitTitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.formSubmitTitle(submit: object)
}

private func formSubmitEnabled(_ object: AnyObject) -> Bool {
    SearchInteropBridge.shared.formSubmitEnabled(submit: object)
}

private func formSubmitStyle(_ object: AnyObject) -> String {
    SearchInteropBridge.shared.formSubmitStyle(submit: object)
}

private func formSubmitIcon(_ object: AnyObject) -> ApiPluginIcon? {
    SearchInteropBridge.shared.formSubmitIcon(submit: object)
}

private func formSubmitCallback(_ object: AnyObject) -> ApiPluginFormSubmitCallback? {
    SearchInteropBridge.shared.formSubmitCallback(submit: object)
}

private func formRequireChanges(_ object: AnyObject) -> Bool {
    guard let node = object as? ApiPluginRayNodeData else { return false }
    return SearchInteropBridge.shared.formRequireChanges(node: node)
}

private func formUnchangedView(_ object: AnyObject) -> AnyObject? {
    guard let node = object as? ApiPluginRayNodeData else { return nil }
    return SearchInteropBridge.shared.formUnchangedView(node: node) as AnyObject?
}

private func formActionPanelHintMode(_ object: AnyObject) -> String {
    guard let node = object as? ApiPluginRayNodeData else { return "Full" }
    return SearchInteropBridge.shared.formActionPanelHintMode(node: node)
}

private func formFieldId(_ object: AnyObject) -> String {
    SearchInteropBridge.shared.formFieldId(field: object)
}

private func formFieldTitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.formFieldTitle(field: object)
}

private func formFieldRequired(_ object: AnyObject) -> Bool {
    SearchInteropBridge.shared.formFieldRequired(field: object)
}

private func formFieldText(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.formFieldText(field: object)
}

private func formFieldPlaceholder(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.formFieldPlaceholder(field: object)
}

private func formFieldDefaultText(_ object: AnyObject) -> String {
    SearchInteropBridge.shared.formFieldDefaultText(field: object)
}

private func formFieldPassword(_ object: AnyObject) -> Bool {
    SearchInteropBridge.shared.formFieldPassword(field: object)
}

private func formFieldMultiline(_ object: AnyObject) -> Bool {
    SearchInteropBridge.shared.formFieldMultiline(field: object)
}

private func formFieldDefaultBoolean(_ object: AnyObject) -> Bool {
    SearchInteropBridge.shared.formFieldDefaultBoolean(field: object)
}

private func formFieldOptions(_ object: AnyObject) -> [AnyObject] {
    SearchInteropBridge.shared.formFieldOptions(field: object).map { $0 as AnyObject }
}

private func formFieldDefaultOption(_ object: AnyObject) -> String? {
    SearchInteropBridge.shared.formFieldDefaultOption(field: object)
}

private func formFieldDefaultDate(_ object: AnyObject) -> String? {
    SearchInteropBridge.shared.formFieldDefaultDate(field: object)
}

private func formOptionValue(_ object: AnyObject) -> String {
    SearchInteropBridge.shared.formOptionValue(option: object)
}

private func formOptionTitle(_ object: AnyObject) -> ApiPluginUiText? {
    SearchInteropBridge.shared.formOptionTitle(option: object)
}

private func formOptionIcon(_ object: AnyObject) -> ApiPluginIcon? {
    SearchInteropBridge.shared.formOptionIcon(option: object)
}

private func formSubmitStyleModel(_ object: AnyObject) -> FormSubmitStyleModel {
    switch formSubmitStyle(object).lowercased() {
    case "tonal":
        return .tonal
    default:
        return .filled
    }
}

private func commandItemIdValue(_ value: Any?) -> String {
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
