plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.comixon.editor.editorcore"
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
            implementation(libs.serialization.json)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
