import SwiftUI

@MainActor
struct ContentView: View {
    let container: RaydroidAppContainer

    var body: some View {
        if PreviewRuntime.isActive {
            PreviewRootView()
        } else {
            SearchSceneView(container: container)
        }
    }
}

private struct PreviewRootView: View {
    var body: some View {
        RaydroidBackground()
            .overlay {
                Text("Raydroid Preview Host")
                    .font(.headline)
                    .foregroundStyle(.secondary)
            }
    }
}
