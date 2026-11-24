import SwiftUI
import ComixonEditor

@main
struct ComixonEditorApp: App {
    private let speechInput = AppleSpeechInput()

    var body: some Scene {
        WindowGroup {
            EditorView(speechInput: speechInput)
                .ignoresSafeArea()
                .preferredColorScheme(.dark)
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)) { _ in
                    speechInput.cancel()
                }
        }
    }
}

private struct EditorView: UIViewControllerRepresentable {
    let speechInput: AppleSpeechInput

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(
            speechInput: speechInput,
            startWithDemo: ProcessInfo.processInfo.arguments.contains("--demo")
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
