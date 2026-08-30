import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinInitIosKt.startKoinIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
