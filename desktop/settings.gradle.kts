// Pulse for Windows and Linux: its own Gradle build, so the Android app's build never loads it.
// Versions come from the app's catalog, so both use the same Kotlin, ktor, Coil and InnerTubeX.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeGroup("com.github.MetrolistGroup.innertubex") }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "pulse-desktop"
