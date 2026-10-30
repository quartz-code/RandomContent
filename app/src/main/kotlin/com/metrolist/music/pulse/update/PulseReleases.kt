package com.metrolist.music.pulse.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pulse: what is newer than the installed build, read from this repository's GitHub releases.
 * Every push publishes a release `build-N`; those from `main` are the regular ones, those from
 * other branches are marked pre-release and offered only to who asked for test builds.
 * Plain Kotlin, so the phone and the computer read releases the same way.
 */
object PulseReleases {
    /** The repository is public, so no token: 60 requests an hour per address, a check is one. */
    const val API = "https://api.github.com/repos/quartz-code/RandomContent/releases?per_page=30"

    /** The phone's file in every release. */
    const val APK = "Pulse.apk"

    private const val NOTES_START = "<!-- notes -->"
    private const val NOTES_END = "<!-- /notes -->"

    data class Release(
        val build: Int,
        val page: String,
        /** Where to download [APK] from; null while CI is still uploading it. */
        val apk: String?,
        val apkSize: Long,
        val testBuild: Boolean,
        val date: String,
        val notes: List<String>,
    )

    /** The newest release to offer, with what changed since the installed build. */
    data class Update(val release: Release, val notes: List<String>)

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tag: String = "",
        @SerialName("html_url") val page: String = "",
        val prerelease: Boolean = false,
        val draft: Boolean = false,
        @SerialName("published_at") val date: String? = null,
        val body: String? = null,
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        val name: String = "",
        @SerialName("browser_download_url") val url: String = "",
        val size: Long = 0,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Releases newest first; anything that is not a `build-N` (or is a draft) is left out. */
    fun parse(text: String): List<Release> =
        json.decodeFromString<List<GitHubRelease>>(text)
            .filter { !it.draft }
            .mapNotNull { release ->
                val build = release.tag.removePrefix("build-").toIntOrNull() ?: return@mapNotNull null
                val apk = release.assets.firstOrNull { it.name == APK }
                Release(build, release.page, apk?.url, apk?.size ?: 0, release.prerelease, release.date.orEmpty(), notesOf(release.body.orEmpty()))
            }
            .sortedByDescending { it.build }

    /**
     * The newest release above [current] in the listener's channel, or null when there is none.
     * [needApk] skips a release whose phone file is not uploaded yet. The notes gather every
     * release skipped over, newest first, so a jump of several builds shows all that changed;
     * a build without its own notes repeats the last ones, which are then shown once.
     */
    fun update(releases: List<Release>, current: Int, testBuilds: Boolean, needApk: Boolean): Update? {
        if (current <= 0) return null
        val newer = releases.filter { it.build > current && (testBuilds || !it.testBuild) }
        val newest = newer.firstOrNull { !needApk || it.apk != null } ?: return null
        val notes = newer.filter { it.build <= newest.build }.flatMap { it.notes }.distinct()
        return Update(newest, notes)
    }

    /** The changelog lines CI puts between the markers in a release's description. */
    fun notesOf(body: String): List<String> {
        val start = body.indexOf(NOTES_START)
        val end = body.indexOf(NOTES_END)
        if (start < 0 || end < start) return emptyList()
        return body.substring(start + NOTES_START.length, end)
            .lines()
            .map { it.trim() }
            .filter { it.startsWith("- ") }
            .map { it.removePrefix("- ").trim() }
    }
}
