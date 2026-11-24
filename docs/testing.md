# Testing the standalone editor

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

Deferred by project scope. Source and host remain in the repository, but iOS is not an installation or acceptance target for this iteration. The optional ios workflow input is reserved for later work.
