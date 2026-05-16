import SwiftUI

@main
@MainActor
struct iOSApp: App {
    @State private var container = RaydroidAppContainer.shared

    var body: some Scene {
        WindowGroup {
            ContentView(container: container)
                .task {
                    guard !PreviewRuntime.isActive else { return }
                    await container.spotlightIndexing.indexCommands()
                }
        }
    }
}
