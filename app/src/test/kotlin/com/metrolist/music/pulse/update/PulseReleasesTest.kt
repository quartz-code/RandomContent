package com.metrolist.music.pulse.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PulseReleasesTest {
    private fun release(build: Int, test: Boolean, notes: List<String>, apk: Boolean = true) = """
        {"tag_name":"build-$build","html_url":"https://github.com/r/releases/tag/build-$build","prerelease":$test,"draft":false,
         "published_at":"2026-10-30T10:00:00Z",
         "body":"Сборка.\n<!-- notes -->\n${notes.joinToString("\\n") { "- $it" }}\n<!-- /notes -->\nСкачай **Pulse.apk**.",
         "assets":[${if (apk) """{"name":"Pulse.apk","browser_download_url":"https://x/$build/Pulse.apk","size":42}""" else ""}],
         "author":{"login":"someone"}}
    """.trimIndent()

    private val releases = PulseReleases.parse(
        "[" + listOf(
            release(55, test = true, notes = listOf("Проверка нового")),
            release(54, test = false, notes = listOf("Обновления внутри приложения")),
            release(53, test = false, notes = listOf("Искры плавные")),
            release(52, test = false, notes = listOf("Искры плавные")),
            """{"tag_name":"desktop-test","prerelease":false,"assets":[]}""",
        ).joinToString(",") + "]",
    )

    @Test
    fun aFriendGetsTheNewestMainBuildWithEverythingSkipped() {
        val update = PulseReleases.update(releases, current = 52, testBuilds = false, needApk = true)!!
        assertEquals(54, update.release.build)
        assertEquals("https://x/54/Pulse.apk", update.release.apk)
        // 53 repeated 52's notes: shown once.
        assertEquals(listOf("Обновления внутри приложения", "Искры плавные"), update.notes)
    }

    @Test
    fun testBuildsOnlyForWhoAskedAndNothingForTheNewestOrALocalBuild() {
        assertEquals(55, PulseReleases.update(releases, current = 54, testBuilds = true, needApk = true)?.release?.build)
        assertNull(PulseReleases.update(releases, current = 54, testBuilds = false, needApk = true))
        assertNull(PulseReleases.update(releases, current = 55, testBuilds = true, needApk = true))
        assertNull(PulseReleases.update(releases, current = 0, testBuilds = true, needApk = true))
    }

    @Test
    fun aReleaseStillUploadingItsApkWaits() {
        val uploading = PulseReleases.parse("[" + release(56, test = false, notes = emptyList(), apk = false) + "," + release(54, test = false, notes = listOf("a")) + "]")
        assertEquals(54, PulseReleases.update(uploading, current = 53, testBuilds = false, needApk = true)?.release?.build)
        // The computer only opens the page, so it does not wait for the phone's file.
        assertEquals(56, PulseReleases.update(uploading, current = 53, testBuilds = false, needApk = false)?.release?.build)
        assertEquals(emptyList<String>(), PulseReleases.notesOf("Старый релиз без заметок"))
    }
}
