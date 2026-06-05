import Observation

enum AppRoute: Equatable {
    case search(String)
    case command(String)
}

@MainActor
@Observable
final class AppRouter {
    var pendingRoute: AppRoute?

    func openSearch(query: String = "") {
        pendingRoute = .search(query)
    }

    func openCommand(id: String) {
        pendingRoute = .command(id)
    }

    func clearPendingRoute() {
        pendingRoute = nil
    }
}
