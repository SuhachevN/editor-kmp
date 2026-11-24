import SwiftUI
import ComixonEditor

@main
struct ComixonEditorApp: App {
    var body: some Scene {
        WindowGroup {
            EditorView().ignoresSafeArea()
        }
    }
}

private struct EditorView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
