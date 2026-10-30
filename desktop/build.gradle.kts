import com.google.protobuf.gradle.proto
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.compose") version "1.12.2"
    alias(libs.plugins.protobuf)
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
// (the innertube module, Android-free but for Timber), the player theme model, the phone sign-in
// and the Listen Together wire format, so the computer and the phone speak the same protocol.
val sharedSources by tasks.registering(Sync::class) {
    from("../innertube/src/main/kotlin")
    from("../app/src/main/kotlin") {
        include("com/metrolist/music/ui/player/PlayerTheme.kt")
        include("com/metrolist/music/pulse/LoginTransfer.kt")
        include("com/metrolist/music/listentogether/Protocol.kt")
        include("com/metrolist/music/listentogether/MessageCodec.kt")
        include("com/metrolist/music/listentogether/ServerClock.kt")
        include("com/metrolist/music/pulse/importer/ImportedTrack.kt")
        include("com/metrolist/music/pulse/importer/TrackMatcher.kt")
        include("com/metrolist/music/pulse/importer/YandexMusicImport.kt")
        include("com/metrolist/music/pulse/importer/VkMusicImport.kt")
        include("com/metrolist/music/pulse/recap/RecapStats.kt")
        include("com/metrolist/music/pulse/feed/PulseFeed.kt")
        include("com/metrolist/music/pulse/feed/YouTubePulseSource.kt")
        include("com/metrolist/music/pulse/update/PulseReleases.kt")
        // "My pulse": its look and how it is drawn, plain Compose on both.
        include("com/metrolist/music/ui/screens/pulse/PulseLook.kt")
        include("com/metrolist/music/ui/screens/pulse/PulseVisuals.kt")
        include("com/metrolist/music/ui/screens/pulse/PulsePage.kt")
        include("com/metrolist/music/ui/screens/pulse/PulseTuneSheet.kt")
    }
    into(layout.buildDirectory.dir("shared-src"))
}

// The app's own tests of that code run here too, on the same copies.
val sharedTests by tasks.registering(Sync::class) {
    from("../app/src/test/kotlin") {
        include("com/metrolist/music/pulse/LoginTransferTest.kt")
        include("com/metrolist/music/pulse/importer/TrackMatcherTest.kt")
        include("com/metrolist/music/pulse/importer/YandexMusicImportTest.kt")
        include("com/metrolist/music/pulse/importer/VkMusicImportTest.kt")
        include("com/metrolist/music/pulse/recap/RecapCalculatorTest.kt")
        include("com/metrolist/music/pulse/feed/PulseFeedTest.kt")
        include("com/metrolist/music/pulse/update/PulseReleasesTest.kt")
        include("com/metrolist/music/ui/screens/pulse/PulseLookTest.kt")
    }
    into(layout.buildDirectory.dir("shared-test"))
}

sourceSets {
    main {
        kotlin.srcDir(sharedSources)
        proto { srcDir("../app/src/main/proto") }
    }
    test {
        kotlin.srcDir(sharedTests)
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                named("java") { option("lite") }
            }
        }
    }
}

// Icons and the theme fonts, taken from the Android app's resources at build time. Android vector
// drawables point at @android:color/white and some measure in "dip"; desktop Compose knows neither.
val sharedIcons = listOf(
    "pulse_play", "pulse_pause", "pulse_skip_next", "pulse_skip_previous", "shuffle", "shuffle_on",
    "repeat", "repeat_on", "repeat_one_on", "queue_music", "search", "close", "volume_up", "volume_off",
    "palette", "music_note", "delete", "playlist_add", "playlist_play", "person", "favorite",
    "favorite_border", "arrow_back", "library_music", "group", "logout", "login", "stats", "download",
    "navigate_next", "thumb_down", "tune", "explore_outlined", "trending_up", "content_copy", "content_paste",
)
val composeRes by tasks.registering(Sync::class) {
    // Gradle does not see changes inside filter {}; this makes an edit to it rerun the copy.
    inputs.property("iconFilters", "white,dip-1,attr-tint")
    from("../app/src/main/res/drawable") {
        include(sharedIcons.map { "$it.xml" })
        filter {
            it.replace("@android:color/white", "#FFFFFFFF").replace(Regex("""(\d)dip""""), "$1dp\"")
                // A theme attribute means nothing outside Android; the icon is tinted where it is drawn.
                .replace("""android:tint="?attr/colorControlNormal"""", "")
        }
        into("drawable")
    }
    from("../app/src/main/res/font") {
        include("oswald.ttf", "pt_mono.ttf")
        into("font")
    }
    // The app's logo (assets/logo/build_icons.py), for the window and the taskbar.
    from("icons") {
        include("pulse.png")
        into("drawable")
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
    implementation(libs.protobuf.javalite)

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
                iconFile.set(project.file("icons/pulse.png"))
                packageName = "pulse-music"
                debMaintainer = "pulse@localhost"
                menuGroup = "AudioVideo"
                appCategory = "Audio"
            }
            windows {
                iconFile.set(project.file("icons/pulse.ico"))
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
