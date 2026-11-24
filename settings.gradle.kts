pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "comixon-editor-kmp"
include(":editor-core", ":editor-compose", ":editor-network", ":sample", ":androidApp")
