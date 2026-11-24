@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidLibrary {
        namespace = "com.comixon.editor.sample.shared"
        compileSdk = 36
        minSdk = 26
        androidResources.enable = true
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ComixonEditor"
            isStatic = true
        }
    }
    sourceSets {
        getByName("androidDeviceTest").dependencies {
            implementation(libs.android.test.runner)
            implementation("androidx.compose.ui:ui-test-manifest:1.9.3")
        }
        commonMain.dependencies {
            implementation(project(":editor-compose"))
            implementation(project(":editor-network"))
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.coroutines.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.ktor.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.uiTest)
        }
        getByName("desktopTest").dependencies { implementation(compose.desktop.uiTestJUnit4) }
        androidMain.dependencies { implementation(libs.activity.compose) }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.coroutines.swing)
        }
    }
}

val compileMacSpeech by tasks.registering(Exec::class) {
    onlyIf { System.getProperty("os.name").startsWith("Mac") }
    val source = layout.projectDirectory.file("native/macos/SpeechHelper.swift")
    val plist = layout.projectDirectory.file("native/macos/SpeechHelper.plist")
    val binary = layout.buildDirectory.file("native-resources/macos/comixon-speech")
    inputs.files(source, plist)
    outputs.file(binary)
    doFirst {
        binary
            .get()
            .asFile.parentFile
            .mkdirs()
    }
    commandLine(
        "swiftc",
        "-O",
        source.asFile,
        "-o",
        binary.get().asFile,
        "-Xlinker",
        "-sectcreate",
        "-Xlinker",
        "__TEXT",
        "-Xlinker",
        "__info_plist",
        "-Xlinker",
        plist.asFile,
    )
}

compose.desktop {
    application {
        mainClass = "com.comixon.editor.sample.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ComixonEditor"
            packageVersion = "1.0.0"
            description = "Standalone Comixon editor"
            vendor = "Comixon"
            modules("java.net.http", "jdk.unsupported", "jdk.crypto.ec", "jdk.accessibility")
            appResourcesRootDir.set(layout.buildDirectory.dir("native-resources"))
            macOS {
                bundleID = "com.comixon.editor.sample"
                infoPlist {
                    extraKeysRawXml =
                        """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>Dictate text into a comic bubble or lettering.</string>
                        <key>NSSpeechRecognitionUsageDescription</key>
                        <string>Convert your voice into editor text using system speech recognition.</string>
                        """.trimIndent()
                }
            }
            windows {
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "706b7784-b024-4d11-81d2-ad1d47130489"
            }
        }
    }
}

compose.resources { packageOfResClass = "com.comixon.editor.sample.resources" }

tasks.matching { it.name in setOf("prepareAppResources", "run") }.configureEach {
    dependsOn(compileMacSpeech)
}
