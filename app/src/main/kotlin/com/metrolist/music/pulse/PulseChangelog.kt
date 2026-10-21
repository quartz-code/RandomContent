package com.metrolist.music.pulse

import com.metrolist.music.utils.ReleaseInfo

/**
 * Pulse: "What's new" lists this fork's changes instead of upstream Metrolist releases. The repo is
 * private, so the notes ship inside the app. Numbers are the GitHub Actions build numbers.
 */
object PulseChangelog {
    private data class Entry(val build: Int, val date: String, val changes: List<String>)

    private val entries = listOf(
        Entry(
            10, "2026-10-21",
            listOf(
                "«Telegram-облако» и «Импорт из Яндекс Музыки» теперь в самом начале Настроек, в разделе «Pulse», и у каждого свой экран с инструкцией.",
                "В Библиотеке → Плейлисты появилась кнопка «Из Яндекса».",
                "Треки уходят в Telegram как музыка — с названием, исполнителем, обложкой и длительностью, а не голосовыми. Старые «голосовые» можно удалить: Pulse отправит эти треки заново.",
                "«Что нового» показывает изменения Pulse.",
            ),
        ),
        Entry(8, "2026-10-21", listOf("Пошаговые инструкции в Telegram-облаке и в импорте из Яндекс Музыки.")),
        Entry(
            7, "2026-10-21",
            listOf(
                "Telegram-облако: копия скачанной музыки в своём боте или закрытом канале. Бесплатно, без ограничения места, слушать можно в Telegram на любом устройстве.",
            ),
        ),
        Entry(6, "2026-10-21", listOf("Импорт плейлистов, альбомов и «Мне нравится» из Яндекс Музыки в YouTube Music.")),
        Entry(
            5, "2026-10-21",
            listOf(
                "Оформления на выбор: «Радист» (в стиле Shortwave), «Классика» и «Терминал». Настройки → Внешний вид → Оформление.",
                "В «Радисте» обычный шрифт в списках и кнопках — так легче читать.",
            ),
        ),
        Entry(
            3, "2026-10-21",
            listOf(
                "Pulse на основе Metrolist: своё имя и значок, ставится рядом с Metrolist.",
                "Все лайки скачиваются на телефон сами, в том числе поставленные на компьютере.",
            ),
        ),
    )

    val latestBuild: Int = entries.maxOf { it.build }

    val releases: List<ReleaseInfo> = entries.map { entry ->
        ReleaseInfo(
            tagName = "Pulse ${entry.build}",
            versionName = "Pulse ${entry.build}",
            description = entry.changes.joinToString("\n") { "- $it" },
            releaseDate = entry.date,
            assets = emptyList(),
        )
    }
}
