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
        commonMain.dependencies {
            implementation(project(":editor-compose"))
            implementation(project(":editor-network"))
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.coroutines.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.ktor.core)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("desktopTest").dependencies { implementation(compose.desktop.uiTestJUnit4) }
        androidMain.dependencies { implementation(libs.activity.compose) }
        getByName("desktopMain").dependencies { implementation(compose.desktop.currentOs) }
    }
}

compose.desktop { application { mainClass = "com.comixon.editor.sample.MainKt" } }
