# Testing the standalone editor

Installation: [English](installation.en.md) · [Русский](installation.ru.md).

## Android

Install the APK from the android-demo artifact in GitHub Actions. Allow installation from the browser/file manager when Android asks. No development tools are needed for a manual check.
For automated tests, connect a phone with USB debugging or start an emulator, then run the connectedAndroidDeviceTest tasks in the README. The test build is separate from the original Comixon app.

## Windows

Unzip windows-demo and run ComixonEditor.exe. Keep the app and runtime directories together. Alternatively, install JDK 17 and run gradlew.bat :sample:run from the checkout.

## macOS

Use macos-demo-macos-15 for Apple Silicon or macos-demo-macos-15-intel for Intel. Open the DMG and copy the app to Applications. Developer signing and notarization need an Apple developer identity, which this repository does not contain. For this private unsigned test build, use Privacy & Security → Open Anyway if offered. If Gatekeeper reports a damaged app, run from the trusted checkout with ./gradlew :sample:run instead.
The packaged app includes the native speech helper. A source run compiles it with swiftc, so Xcode command-line tools are needed.

## Linux

Install JDK 17 and run ./gradlew :sample:run. Shared tests run under Xvfb in CI. The editor microphone currently has no Linux adapter; keyboard input and the desktop environment's voice input remain available.

## Manual checks

1. On the start screen choose photos, select two images and confirm their order in the frame strip. Cancel a selection and check that the current document is preserved. Alternatively open the demo; select each bubble and lettering family. Drag a layer, resize it and move its tail.
2. Edit text/font/size. Dictate into a selection, then confirm. Undo and redo should treat the confirmed text edit as one change.
3. Import a photo and add a second frame. Crop, zoom, rotate and flip; cancel one crop and confirm another.
4. Change layer order and delete a layer/frame. Undo and redo.
5. Save a JSON project, reopen it and export a PNG frame and strip. Reopened artwork and exported images should match the canvas without editing handles.
6. Without a token, tap Start generation: a short authorization notification should appear without a request. AI filters remain available under the server’s access rules. With an eligible Comixon session, try full generation and its previous/next versions, blending and cancellation.

## Dictation

Android uses the installed SpeechRecognizer service. Hold the microphone in the text field, speak and release; allow microphone access when asked. The device needs a working recognition provider and language support. Some providers require internet.
macOS uses the Speech framework and microphone; allow both permissions on first use. Windows uses System.Speech and needs a recognition language installed for the current OS locale; Win+H in the text field also provides system voice input. Dictation uses OS services and never sends audio to Comixon. Automated tests cover selection replacement and the helper protocol; acoustic recognition needs a manual device check.

## Network and authorization

The token is a **Comixon session token**, issued after signing into Comixon. It is unrelated to GitHub or GitLab. The sample does not include account login, feed or publishing. Use a development account/session supplied by the backend when an endpoint requires authorization. Never commit tokens or paste them into test reports.
Start generation sends separate normalized JPEG payloads: cropped/filter-adjusted artwork without overlays for the anime request, and the source photo for dialogue. Requests run concurrently; a successful result is retained if the other fails. Results apply to the captured frame only while its source image is unchanged and form one undo entry. Dialogue history retains up to ten generations per frame.
Live check on 2026-11-24 before enabling the no-token Start generation guard, Android emulator, default dev server: the anime endpoint returned an image, which was downloaded and applied; the dialogue endpoint rejected the anonymous request with an authorization error. Authenticated dialogue and account limits still need validation with an eligible session. MockEngine tests independently cover headers, both request bodies, response mapping, authorization errors, timeouts and partial success.

## iOS

Download **ios-unsigned** from the successful workflow and unzip comixon-editor-unsigned.ipa. This is an ARM64 iPhone/iPad build for iOS 15+. It is not signed and cannot be installed directly from GitHub.

Without a Mac, use [Sideloadly](https://sideloadly.io/) on Windows, following its current installation requirements. Connect and trust the iPhone over USB, select the IPA and sign it with your own Apple ID in Sideloadly. On iOS 16+, enable Developer Mode when required; trust the developer profile in Settings if prompted. A free account's signature expires after seven days, so the app needs to be signed again. See the [official FAQ](https://sideloadly.io/faq) for limits and renewal. Apple credentials and signing certificates are never needed by this repository or its workflows.

With a Mac, install Xcode and XcodeGen, run `xcodegen generate --spec iosApp/project.yml`, then open iosApp/ComixonEditor.xcodeproj. Select the ComixonEditor scheme and an Apple Silicon iOS Simulator. For a physical device choose your own development team under Signing & Capabilities. The host uses direct Kotlin framework integration, without CocoaPods. Intel iOS Simulator is not a configured target.

CI runs shared Kotlin tests and an XCTest welcome → demo → anonymous-generation check on iPhone Simulator. The ios-test-reports artifact includes the XCTest result bundle and screenshot. This does not prove recording quality, real-device photo selection or installation/signing: check those on an iPhone using the manual checklist above.

iOS dictation uses Apple Speech and AVAudioEngine. It requests speech and microphone access only when dictation starts, replaces partial transcripts in the current selection, and cancels when the editor closes or the app enters the background. Recognition availability and internet requirements depend on the device and language; audio is not sent to Comixon.
