import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.compose") version "1.12.2"
}

kotlin {
    jvmToolchain(21)
}

// The natives of FFmpeg for the system the installer is built on; each CI runner builds its own.
val nativePlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("win") -> "windows-x86_64"
        os.contains("mac") -> "macosx-x86_64"
        else -> "linux-x86_64"
    }
}
val javacppVersion = "1.5.14"
val ffmpegVersion = "8.1.2-$javacppVersion"

// Code shared with the Android app, copied read-only at build time: the YouTube Music client
// (the innertube module, Android-free but for Timber) and the player theme model.
val sharedSources by tasks.registering(Sync::class) {
    from("../innertube/src/main/kotlin")
    from("../app/src/main/kotlin") {
        include("com/metrolist/music/ui/player/PlayerTheme.kt")
    }
    into(layout.buildDirectory.dir("shared-src"))
}

sourceSets {
    main {
        kotlin.srcDir(sharedSources)
    }
}

// Icons and the theme fonts, taken from the Android app's resources at build time. Android vector
// drawables point at @android:color/white and some measure in "dip"; desktop Compose knows neither.
val sharedIcons = listOf(
    "pulse_play", "pulse_pause", "pulse_skip_next", "pulse_skip_previous", "shuffle", "shuffle_on",
    "repeat", "repeat_on", "repeat_one_on", "queue_music", "search", "close", "volume_up", "volume_off",
    "palette", "music_note", "delete", "playlist_add", "playlist_play",
)
val composeRes by tasks.registering(Sync::class) {
    // Gradle does not see changes inside filter {}; this makes an edit to it rerun the copy.
    inputs.property("iconFilters", "white,dip-1")
    from("../app/src/main/res/drawable") {
        include(sharedIcons.map { "$it.xml" })
        filter { it.replace("@android:color/white", "#FFFFFFFF").replace(Regex("""(\d)dip""""), "$1dp\"") }
        into("drawable")
    }
    from("../app/src/main/res/font") {
        include("oswald.ttf", "pt_mono.ttf")
        into("font")
    }
    into(layout.buildDirectory.dir("compose-res"))
}

compose.resources {
    packageOfResClass = "app.pulse.desktop.res"
    generateResClass = always
    customDirectory("main", composeRes.map { layout.buildDirectory.dir("compose-res").get() })
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation(libs.innertubex)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation("io.coil-kt.coil3:coil-compose:${libs.versions.coil.get()}")
    implementation("io.coil-kt.coil3:coil-network-okhttp:${libs.versions.coil.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")

    // Audio: FFmpeg decodes every YouTube format; its natives ship inside these jars.
    implementation("org.bytedeco:javacv:$javacppVersion") { isTransitive = false }
    implementation("org.bytedeco:javacpp:$javacppVersion")
    implementation("org.bytedeco:javacpp:$javacppVersion:$nativePlatform")
    implementation("org.bytedeco:ffmpeg:$ffmpegVersion")
    implementation("org.bytedeco:ffmpeg:$ffmpegVersion:$nativePlatform")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "app.pulse.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi)
            packageName = "Pulse"
            packageVersion = (findProperty("pulse.version") as String?) ?: "1.0.0"
            description = "Pulse — YouTube Music"
            vendor = "Pulse"
            // From ./gradlew -p desktop suggestRuntimeModules, plus what ktor and Java Sound need.
            modules("java.instrument", "java.management", "java.naming", "java.net.http", "jdk.crypto.ec", "jdk.unsupported")
            linux {
                packageName = "pulse-music"
                debMaintainer = "pulse@localhost"
                menuGroup = "AudioVideo"
                appCategory = "Audio"
            }
            windows {
                menuGroup = "Pulse"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "8d3c8a62-5f0e-4f6b-9b7e-2f1c4a6e9d10"
            }
        }
    }
}

// A run from the terminal that checks search, stream extraction and decoding without a window.
tasks.register<JavaExec>("probe") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("app.pulse.desktop.ProbeKt")
    args = listOfNotNull(findProperty("query") as String?)
}
