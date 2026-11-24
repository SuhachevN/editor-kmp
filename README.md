# Comixon Editor

A standalone **Kotlin Multiplatform** comic editor for Android, iOS and Desktop (Windows, macOS and Linux). The canvas, editing operations and UI are shared Kotlin code. The original Comixon application is kept separate.

- 24 bubble styles, editable tails, image masks and 14 lettering variants.
- A native photo picker before the editor, with multi-photo import and a separate demo.
- Text, fonts, layers, frames, crop, rotation, flip and instant filters.
- Undo/redo, portable JSON projects and PNG frame/strip export.
- AI images and dialogue through the existing Comixon HTTP API.
- System speech dictation into bubble and lettering text on Android, iOS, Windows and macOS.

## Try it

Open the latest successful [Build and test run](https://github.com/SuhachevN/editor-kmp/actions/workflows/ci.yml) and download an artifact:

| Platform | Artifact | Start |
| --- | --- | --- |
| Android 8+ | android-demo | Unzip and install the APK |
| iOS 15+ | ios-unsigned | Sign the IPA with your Apple ID before installing; see [iOS notes](docs/testing.md#ios) |
| Windows | windows-demo | Unzip and run ComixonEditor.exe |
| macOS (Apple Silicon) | macos-demo-macos-15 | Open the DMG and copy the app to Applications |
| macOS (Intel) | macos-demo-macos-15-intel | Open the DMG and copy the app to Applications |

Desktop packages include Java. macOS builds are unsigned; see [testing notes](docs/testing.md) for first launch and platform checks.

## Develop

Use JDK 17. Android builds also need SDK 36 and ANDROID_HOME or an untracked local.properties with sdk.dir. On Windows use gradlew.bat.

```shell
./gradlew :sample:run                 # Desktop
./gradlew :androidApp:assembleDebug   # Android
./gradlew :sample:packageDmg          # macOS only; includes native speech helper
./gradlew :sample:createDistributable # Desktop app with bundled Java
```

| Module | Responsibility |
| --- | --- |
| editor-core | Document model, editing operations and JSON |
| editor-compose | Geometry, text layout, canvas, gestures and export |
| editor-network | Direct Ktor integration with Comixon |
| sample | Shared editor UI and platform adapters |
| androidApp / iosApp | Platform launchers |

## Test

```shell
./gradlew ktlintCheck :editor-core:desktopTest :editor-compose:desktopTest :editor-network:desktopTest :sample:desktopTest
./gradlew :editor-core:connectedAndroidDeviceTest :editor-compose:connectedAndroidDeviceTest :editor-network:connectedAndroidDeviceTest :sample:connectedAndroidDeviceTest
```

CI checks Android on an emulator and Desktop on Windows, macOS and Linux, including packaged Windows/macOS startup. Tests cover geometry, gestures, text/crop flows, export, undo/redo, serialization, dictation insertion, HTTP contracts and the complete two-request generation workflow. Network tests use MockEngine and do not consume credits. iOS checks run shared tests and an application smoke test in Simulator, then build an unsigned device IPA.

## Server

Set URL, language and optional Comixon session token in **⋯ → Server settings**. The default server is https://dev.comixon.com. Without a Comixon token, Start generation shows a short authorization notification and sends no requests. With a token it sends **/v2/filter** (anime image) and **/v2/get_speech** (dialogue) concurrently. AI filter selection uses /v2/filter; blending also sends a second image. WebSockets are not used. Tokens stay in memory and are excluded from project files. Authorization and generation limits are controlled by the existing server; publishing posts is outside this editor.

[Platform checks and live-network status](docs/testing.md)
