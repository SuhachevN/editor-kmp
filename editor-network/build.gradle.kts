plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.comixon.editor.editornetwork"
        compileSdk = 36
        minSdk = 26
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies {
            api(project(":editor-core"))
            implementation(libs.serialization.json)
            implementation(libs.coroutines.core)
            implementation(libs.ktor.core)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies { implementation(libs.ktor.cio) }
        getByName("desktopMain").dependencies { implementation(libs.ktor.cio) }
        iosMain.dependencies { implementation(libs.ktor.darwin) }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
            implementation(libs.ktor.mock)
        }
    }
}
