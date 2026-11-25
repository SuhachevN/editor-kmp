# Testing the standalone editor

Installation: [English](../README.md#installation) · [Русский](../README.md#установка).

For Android device tests, connect a phone with USB debugging or start an emulator, then run the commands in the [README](../README.md#test). Linux tests run under Xvfb in CI; its microphone adapter is not implemented.

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

CI runs shared Kotlin tests and an XCTest welcome → demo → anonymous-generation check on iPhone Simulator. The ios-test-reports artifact includes the XCTest result bundle and screenshot. This does not prove recording quality, real-device photo selection or installation/signing: check those on an iPhone using the manual checklist above.

iOS dictation uses Apple Speech and AVAudioEngine. It requests speech and microphone access only when dictation starts, replaces partial transcripts in the current selection, and cancels when the editor closes or the app enters the background. Recognition availability and internet requirements depend on the device and language; audio is not sent to Comixon.
