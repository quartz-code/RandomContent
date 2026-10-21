package com.metrolist.music.pulse.importer

/** Pulse: a track read from another service, before it is matched on YouTube Music. */
data class ImportedTrack(
    val title: String,
    val artists: List<String>,
    /** Length in seconds when the source knows it, used to tell versions apart. */
    val durationSec: Int? = null,
)

/** A list read from another service: a playlist, an album or the whole library. */
data class ImportedList(
    val title: String,
    val source: ImportSource,
    val tracks: List<ImportedTrack>,
)

enum class ImportSource { YANDEX, VK }
