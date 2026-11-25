# Install Comixon Editor from GitHub

[Русская версия](installation.ru.md)

## Download a build

1. Sign in to GitHub with an account that has access to [SuhachevN/editor-kmp](https://github.com/SuhachevN/editor-kmp). The repository is private.
2. Open **Actions → Build and test**, or use the [workflow page](https://github.com/SuhachevN/editor-kmp/actions/workflows/ci.yml).
3. Open the latest successful run on **main** with a green check mark. If the newest run is still building, use the previous successful one.
4. Scroll to **Artifacts**, click the name below and extract the downloaded ZIP.

| Platform | Artifact | File to open |
| --- | --- | --- |
| Android 8.0+ | `android-demo` | `androidApp-debug.apk` |
| Windows x64 | `windows-demo` | `ComixonEditor.exe` |
| macOS, Apple Silicon | `macos-demo-macos-15` | The `.dmg` file |
| macOS, Intel | `macos-demo-macos-15-intel` | The `.dmg` file |
| iOS 15+ | `ios-unsigned` | `comixon-editor-unsigned.ipa`, after signing |
| Linux | Source code | See [Linux](#linux) |

The artifact names are labels used by CI. For example, `macos-15` identifies the build runner; it is not a separate edition of the editor. Artifacts are attached to workflow runs, not GitHub Releases.

Downloading artifacts requires a signed-in account with repository read access. Artifacts expire; GitHub's default retention is 90 days. See [GitHub's download instructions](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

## Android

1. Download and extract `android-demo` on your phone, or transfer the extracted APK from a computer.
2. Open `androidApp-debug.apk` in the phone's file manager.
3. If Android asks, allow this browser or file manager to install the app, then confirm installation.
4. Open **Comixon Editor**.

This is a debug build for testing. Android Studio, Java and a Comixon account are not needed to install it.

## Windows

1. Download `windows-demo` on your Windows x64 computer.
2. Right-click the ZIP and choose **Extract All**.
3. Open the extracted folder and run `ComixonEditor.exe`.

Keep the entire extracted folder together, including `app` and `runtime`. Running from inside the ZIP or moving only the EXE will not work. Java is bundled with the application.

## macOS

1. Check **Apple menu → About This Mac**. An Apple M-series chip needs `macos-demo-macos-15`; an Intel processor needs `macos-demo-macos-15-intel`.
2. Download and extract that artifact, then open the DMG.
3. Copy **ComixonEditor.app** to **Applications**.
4. Open the application from **Applications**.

These test builds are not Developer ID-signed or notarized. If macOS blocks the app because the developer cannot be verified, and you trust the build downloaded from this repository, use **System Settings → Privacy & Security → Open Anyway**, if offered. Follow [Apple's first-launch instructions](https://support.apple.com/en-us/102445).

If macOS reports that the app is damaged or does not offer an exception, use a source build instead: install JDK 17 and Xcode command-line tools, download the source as described under Linux, then run `./gradlew :sample:run` from the project folder. The command-line tools supply `swiftc` for the dictation helper. Packaged DMGs already include Java and the helper.

## iOS

The IPA is an unsigned ARM64 device build for iOS 15+. Downloading or tapping it on an iPhone does not install it: it must first be signed for that device.

### Install using Windows and Sideloadly

You need an iPhone, a USB cable, your Apple ID and a Windows computer.

1. Download and extract `ios-unsigned` to get `comixon-editor-unsigned.ipa`.
2. Install [Sideloadly from its official website](https://sideloadly.io/). Its Windows setup requires the web versions of **iTunes and iCloud**, linked on that page, rather than the Microsoft Store versions.
3. Connect the iPhone by USB, unlock it and accept **Trust This Computer** on the phone.
4. Open Sideloadly, select the iPhone and drag the IPA into the application.
5. Enter your Apple ID and click **Start**. Complete authentication in Sideloadly yourself.
6. On iOS 16+, enable **Settings → Privacy & Security → Developer Mode** when required, following the phone's restart and confirmation prompts.
7. If the phone reports an untrusted developer, open **Settings → General → VPN & Device Management**, select the Apple ID used for signing and trust its developer profile.
8. Open **Comixon Editor**.

With a free Apple ID, the signature lasts seven days. Sign the app again with the same Apple ID and bundle identifier, or configure Sideloadly's automatic refresh. Export your projects before reinstalling or renewing a build. The [official Sideloadly FAQ](https://sideloadly.io/faq) covers renewal, Developer Mode and device detection.

Your Apple password, verification codes and signing certificates belong in your own signing setup; this repository and GitHub Actions do not need them.

### Run using Xcode on a Mac

1. Install Xcode, JDK 17 and [XcodeGen](https://github.com/yonaskolb/XcodeGen#installing).
2. Download the repository source with **Code → Download ZIP** and extract it.
3. In Terminal, open the extracted project folder and run:

   ```sh
   chmod +x gradlew
   xcodegen generate --spec iosApp/project.yml
   open iosApp/ComixonEditor.xcodeproj
   ```

4. Select the **ComixonEditor** scheme. On an Apple Silicon Mac, choose an iPhone Simulator and press **Run**.
5. To run on a connected iPhone, select **ComixonEditorApp → Signing & Capabilities**, choose your development team, select the phone as the destination and press **Run**.

The framework is built by the Xcode build script. CocoaPods is not used. Intel iOS Simulator is not configured; the physical-device target is ARM64.

## Linux

Linux currently runs from source; CI does not publish a Linux installer. The CI environment is Ubuntu x86_64.

1. Install **JDK 17** using your distribution's package manager. Check that `java -version` reports version 17.
2. While signed into GitHub, open the repository and choose **Code → Download ZIP**.
3. Extract the archive. Open a terminal in the extracted project folder, where `gradlew` and `settings.gradle.kts` are located.
4. Run:

   ```sh
   chmod +x gradlew
   ./gradlew :sample:run
   ```

The first launch downloads Gradle and project dependencies, so it needs internet. Run this in a graphical desktop session. The editor's microphone button has no Linux dictation adapter; keyboard input remains available.

## First check and updates

Open **Choose photos** to start with your own images, or **Open demo** to explore the editor. Add a bubble, edit its text, try undo/redo, save a JSON project and export a PNG.

Local editing does not require a Comixon account. Without a Comixon session token, **Start generation** shows an authorization notice and sends no requests. AI filters use the current server's access rules. Dictation depends on the OS recognition service and may need microphone permissions and internet. See the [testing checklist](testing.md) for platform and network checks.

To update, export your projects, then install a newer build using the same steps. Keep the whole Windows app folder together; on iOS keep the same Apple ID and bundle identifier when renewing.

If an artifact has expired, a repository member with write access can open **Actions → Build and test → Run workflow**, select **main** and start a new run. Wait for it to finish before downloading its artifacts. iOS builds can take considerably longer than Android builds.
