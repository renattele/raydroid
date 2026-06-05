import AppIntents
import Foundation

struct RaydroidCommandEntity: AppEntity, Identifiable {
    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Raydroid Command")
    static var defaultQuery = RaydroidCommandQuery()

    let id: String
    let title: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)", subtitle: "\(id)", image: .init(systemName: "command"))
    }
}

struct RaydroidCommandQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [RaydroidCommandEntity] {
        let provider = await MainActor.run {
            RaydroidAppContainer.shared.commandCatalogProvider
        }
        return await provider.commands()
            .filter { identifiers.contains($0.id) }
            .map { RaydroidCommandEntity(item: $0) }
    }

    func suggestedEntities() async throws -> [RaydroidCommandEntity] {
        let provider = await MainActor.run {
            RaydroidAppContainer.shared.commandCatalogProvider
        }
        return await provider.commands().map { RaydroidCommandEntity(item: $0) }
    }
}

struct OpenRaydroidSearchIntent: AppIntent {
    static var title: LocalizedStringResource = "Open Raydroid Search"
    static var description = IntentDescription("Open Raydroid and focus command search.")
    static var openAppWhenRun = true

    @Parameter(title: "Query")
    var query: String?

    @MainActor
    func perform() async throws -> some IntentResult {
        RaydroidAppContainer.shared.router.openSearch(query: query ?? "")
        return .result()
    }
}

struct RunRaydroidCommandIntent: AppIntent {
    static var title: LocalizedStringResource = "Run Raydroid Command"
    static var description = IntentDescription("Open Raydroid to a selected command.")
    static var openAppWhenRun = true

    @Parameter(title: "Command")
    var command: RaydroidCommandEntity

    @MainActor
    func perform() async throws -> some IntentResult {
        RaydroidAppContainer.shared.router.openCommand(id: command.id)
        return .result()
    }
}

struct RaydroidShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenRaydroidSearchIntent(),
            phrases: [
                "Search in \(.applicationName)",
                "Open \(.applicationName)"
            ],
            shortTitle: "Search",
            systemImageName: "magnifyingglass"
        )

        AppShortcut(
            intent: RunRaydroidCommandIntent(),
            phrases: [
                "Run command in \(.applicationName)"
            ],
            shortTitle: "Run Command",
            systemImageName: "command"
        )
    }
}

private extension RaydroidCommandEntity {
    init(item: CommandCatalogItem) {
        self.init(id: item.id, title: item.title)
    }
}
