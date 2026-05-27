import Foundation
import RaydroidShared

@MainActor
final class RaydroidAppContainer {
    static let shared = RaydroidAppContainer()

    let router: AppRouter
    let commandCatalogProvider: any CommandCatalogProviding
    let spotlightIndexing: any SpotlightIndexing
    let searchViewModelClient: SearchViewModelClient

    private init() {
        FirebaseBootstrap.configureIfNeeded()
        RaydroidBootstrapKt.InitKoin()

        let router = AppRouter()
        let commandCatalogProvider = KmpCommandCatalogProvider()
        self.router = router
        self.commandCatalogProvider = commandCatalogProvider
        self.spotlightIndexing = SpotlightIndexingService(
            commandCatalogProvider: commandCatalogProvider
        )
        self.searchViewModelClient = KmpSearchViewModelClient()
    }
}
