import Foundation
import RaydroidShared

struct CommandCatalogItem: Identifiable, Hashable {
    let id: String
    let title: String
}

protocol CommandCatalogProviding {
    func commands() async -> [CommandCatalogItem]
    func command(id: String) async -> CommandCatalogItem?
}

final class KmpCommandCatalogProvider: CommandCatalogProviding {
    private let provider: CommandCatalogProvider
    private let languageCode: () -> String

    init(
        provider: CommandCatalogProvider = RaydroidBootstrapKt.CreateCommandCatalogProvider(),
        languageCode: @escaping () -> String = {
            Locale.current.language.languageCode?.identifier ?? "en"
        }
    ) {
        self.provider = provider
        self.languageCode = languageCode
    }

    func commands() async -> [CommandCatalogItem] {
        do {
            let entries = try await provider.commands(language: languageCode())
            return entries.map { entry in
                CommandCatalogItem(
                    id: entry.id,
                    title: entry.title
                )
            }
        } catch {
            return []
        }
    }

    func command(id: String) async -> CommandCatalogItem? {
        await commands().first(where: { $0.id == id })
    }
}
