import CoreSpotlight
import Foundation
import UIKit

protocol SpotlightIndexing {
    func indexCommands() async
}

struct SpotlightIndexingService: SpotlightIndexing {
    private let commandCatalogProvider: any CommandCatalogProviding

    init(commandCatalogProvider: any CommandCatalogProviding) {
        self.commandCatalogProvider = commandCatalogProvider
    }

    func indexCommands() async {
        let commands = await commandCatalogProvider.commands()
        let searchableItems = commands.map { command in
            let attributes = CSSearchableItemAttributeSet(contentType: .item)
            attributes.title = command.title
            attributes.contentDescription = "Open \(command.title) in Raydroid"
            attributes.keywords = ["Raydroid", command.title, command.id]
            attributes.thumbnailData = UIImage(systemName: "command")?.pngData()

            return CSSearchableItem(
                uniqueIdentifier: command.id,
                domainIdentifier: "ru.raydroid.commands",
                attributeSet: attributes
            )
        }

        do {
            try await CSSearchableIndex.default().indexSearchableItems(searchableItems)
        } catch {
        }
    }
}
