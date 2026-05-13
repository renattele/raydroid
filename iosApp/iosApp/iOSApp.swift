import SwiftUI

@main
@MainActor
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .task {
                    guard !PreviewRuntime.isActive else { return }
                    await RaydroidAppContainer.shared.spotlightIndexing.indexCommands()
                }
        }
    }
}
