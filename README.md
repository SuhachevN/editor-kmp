# Comixon Editor

A standalone Kotlin Multiplatform comic editor for **Android, iOS and Desktop**, extracted from Comixon. Editing and rendering live in shared Kotlin code.

- 24 bubble styles, editable tails, image masks and 14 lettering variants.
- Text and font editing, layers, frames, crop, rotation, flip and instant filters.
- Undo/redo, portable JSON projects and PNG export for a frame or a complete strip.
- Comixon AI filters and dialogue generation through direct HTTP requests.

## Modules

| Module | Responsibility |
| --- | --- |
| `editor-core` | Document model, editing operations and JSON format |
| `editor-compose` | Shared geometry, text layout, canvas, gestures and export |
| `editor-network` | Ktor client for the existing Comixon API |
| `sample` | Shared demo UI and Desktop/iOS entry points |
| `androidApp` / `iosApp` | Platform launchers |

## Run

Use **JDK 17** and Android SDK 36. Set `sdk.dir` in an untracked `local.properties`, or set `ANDROID_HOME`.

```shell
./gradlew :sample:run                 # Desktop
./gradlew :androidApp:assembleDebug   # Android; or run androidApp in Android Studio
```

For iOS, use a Mac with Xcode and XcodeGen: `xcodegen generate --spec iosApp/project.yml`, then open `iosApp/ComixonEditor.xcodeproj` and run the ComixonEditor scheme. Xcode builds the shared Kotlin framework automatically.

## Test

```shell
./gradlew ktlintCheck :editor-core:desktopTest :editor-compose:desktopTest :editor-network:desktopTest :sample:desktopTest
# macOS / Apple Silicon
./gradlew :editor-core:iosSimulatorArm64Test :editor-compose:iosSimulatorArm64Test :editor-network:iosSimulatorArm64Test
```

Tests cover bubble geometry and tail history, scaling, masks, editing, undo/redo, JSON round trips and HTTP contracts. GitHub Actions builds Android and iOS and runs the shared tests. Network tests use MockEngine and do not consume generation credits.

## Server

In the demo's **AI** tab, set the server URL, language and a valid Comixon token. The default server is `https://dev.comixon.com`. Requests go directly to `/v2/filter` and `/v2/get_speech`; WebSockets are not used. Tokens stay in memory and are excluded from project files. Live generation requires server access and an eligible Comixon account.
