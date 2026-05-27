import FirebaseAnalytics
import FirebaseCore
import Foundation
import RaydroidShared

private final class IOSFirebaseAnalyticsTracker: NSObject, AnalyticsTracker {
    func logLaunch(target: AnalyticsLaunchTarget) {
        Analytics.logEvent(target.eventName, parameters: nil)
    }
}

enum FirebaseBootstrap {
    private static let analyticsTracker = IOSFirebaseAnalyticsTracker()

    static func configureIfNeeded() {
        if PreviewRuntime.isActive {
            FirebaseAnalyticsBridge.shared.clear()
            return
        }

        FirebaseAnalyticsBridge.shared.register(tracker: analyticsTracker)

        guard FirebaseApp.app() == nil else { return }
        FirebaseApp.configure()
    }
}
