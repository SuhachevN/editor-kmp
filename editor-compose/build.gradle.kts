plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidLibrary {
        namespace = "com.comixon.editor.editorcompose"
        compileSdk = 36
        minSdk = 26
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets {
        getByName("androidDeviceTest").dependencies { implementation(libs.android.test.runner) }
        commonMain.dependencies {
            api(project(":editor-core"))
            api(compose.foundation)
            api(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("desktopTest").dependencies { implementation(compose.desktop.currentOs) }
    }
}

compose.resources { packageOfResClass = "com.comixon.editor.resources" }
