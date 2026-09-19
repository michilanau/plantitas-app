import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    // Mirrors org.mlanau.project.shared.ui.theme.StatusBarAppearance, which reflects the app's
    // effective LIGHT/DARK/SYSTEM setting (resolved in App()). Applying it here via
    // .preferredColorScheme is what drives the status bar style for the embedded Compose view
    // controller — see that object's doc for why.
    @State private var isDark = false

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .preferredColorScheme(isDark ? .dark : .light)
            .onAppear {
                StatusBarAppearance.shared.setListener { dark in
                    isDark = dark
                }
            }
    }
}