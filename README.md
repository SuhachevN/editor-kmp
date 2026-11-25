# Comixon Editor

A standalone **Kotlin Multiplatform** comic editor for Android, iOS and Desktop (Windows, macOS and Linux). The canvas, editing operations and UI are shared Kotlin code. The original Comixon application is kept separate.

- 24 bubble styles, editable tails, image masks and 14 lettering variants.
- A native photo picker before the editor, with multi-photo import and a separate demo.
- Text, fonts, layers, frames, crop, rotation, flip and instant filters.
- Undo/redo, portable JSON projects and PNG frame/strip export.
- AI images and dialogue through the existing Comixon HTTP API.
- System speech dictation into bubble and lettering text on Android, iOS, Windows and macOS.

## Installation

Sign in to GitHub with access to this private repository. Open [Actions → Build and test](https://github.com/SuhachevN/editor-kmp/actions/workflows/ci.yml), choose the latest successful **main** run, then download and unzip the required **Artifact**.

| Platform | Artifact | Install |
| --- | --- | --- |
| Android 8+ | `android-demo` | Open the APK on your phone and allow installation when asked |
| Windows x64 | `windows-demo` | Extract the whole folder and run `ComixonEditor.exe`; keep `app` and `runtime` together |
| macOS, Apple Silicon | `macos-demo-macos-15` | Open the DMG and copy the app to Applications |
| macOS, Intel | `macos-demo-macos-15-intel` | Open the DMG and copy the app to Applications |
| iOS 15+ | `ios-unsigned` | Sign `comixon-editor-unsigned.ipa` before installing; see below |
| Linux | **Code → Download ZIP** | Install JDK 17, extract the source and run `sh gradlew :sample:run` in the project folder |

**iOS from Windows:** install [Sideloadly](https://sideloadly.io/) and the web versions of iTunes/iCloud linked on its website. Connect and trust the iPhone over USB, select it in Sideloadly, load the IPA, enter your Apple ID and click **Start**. On iOS 16+, enable **Settings → Privacy & Security → Developer Mode**; if prompted, trust the developer profile under **General → VPN & Device Management**. Free Apple ID signatures expire after seven days and need renewal. [Sideloadly FAQ](https://sideloadly.io/faq).

Desktop packages include Java. macOS builds are unsigned: if you trust this repository's build, use **System Settings → Privacy & Security → Open Anyway**, if offered. If macOS reports a damaged app, use a source build instead. [Apple's instructions](https://support.apple.com/en-us/102445).

## Установка

Войди в GitHub под аккаунтом с доступом к приватному репозиторию. Открой [Actions → Build and test](https://github.com/SuhachevN/editor-kmp/actions/workflows/ci.yml), выбери последнюю успешную сборку **main**, скачай нужный файл в **Artifacts** и распакуй ZIP.

| Платформа | Артефакт | Установка |
| --- | --- | --- |
| Android 8+ | `android-demo` | Открой APK на телефоне и разреши установку, если Android попросит |
| Windows x64 | `windows-demo` | Распакуй всю папку и запусти `ComixonEditor.exe`; сохрани папки `app` и `runtime` рядом |
| macOS, Apple Silicon | `macos-demo-macos-15` | Открой DMG и скопируй приложение в «Программы» |
| macOS, Intel | `macos-demo-macos-15-intel` | Открой DMG и скопируй приложение в «Программы» |
| iOS 15+ | `ios-unsigned` | Перед установкой подпиши `comixon-editor-unsigned.ipa`, см. ниже |
| Linux | **Code → Download ZIP** | Установи JDK 17, распакуй исходники и выполни `sh gradlew :sample:run` в папке проекта |

**iOS через Windows:** установи [Sideloadly](https://sideloadly.io/) и версии iTunes/iCloud с сайта Apple по ссылкам на странице Sideloadly. Подключи iPhone по USB и подтверди доверие компьютеру. В Sideloadly выбери телефон, добавь IPA, укажи Apple ID и нажми **Start**. На iOS 16+ включи **Настройки → Конфиденциальность и безопасность → Режим разработчика**; при запросе подтверди доверие профилю в **Основные → VPN и управление устройством**. С бесплатным Apple ID подпись действует семь дней, затем её нужно продлить. [FAQ Sideloadly](https://sideloadly.io/faq).

В готовые Desktop-сборки Java уже включена. Сборки macOS без подписи Apple: если доверяешь сборке из этого репозитория, выбери **Системные настройки → Конфиденциальность и безопасность → Всё равно открыть**, если этот пункт доступен. При сообщении о повреждённом приложении используй запуск из исходников. [Инструкция Apple](https://support.apple.com/en-us/102445).

## Develop

Use JDK 17. Android builds also need SDK 36 and ANDROID_HOME or an untracked local.properties with sdk.dir. On Windows use gradlew.bat. Source builds need internet on first launch; macOS also needs Xcode command-line tools for dictation.

```shell
./gradlew :sample:run                 # Desktop
./gradlew :androidApp:assembleDebug   # Android
./gradlew :sample:packageDmg          # macOS only; includes native speech helper
./gradlew :sample:createDistributable # Desktop app with bundled Java
```

For iOS development on a Mac, install Xcode and XcodeGen, run `xcodegen generate --spec iosApp/project.yml`, then open `iosApp/ComixonEditor.xcodeproj`. Choose the **ComixonEditor** scheme and an Apple Silicon simulator, or select your own development team for a connected iPhone.

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
