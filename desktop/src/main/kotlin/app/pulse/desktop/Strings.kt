package app.pulse.desktop

import java.util.Locale

/** Pulse desktop: the window's texts, Russian or English by the system language. */
object Strings {
    private val ru = Locale.getDefault().language in setOf("ru", "uk", "be", "kk")
    private fun t(ruText: String, en: String) = if (ru) ruText else en

    val appName = "Pulse"
    val searchHint = t("Песня, исполнитель или альбом", "Song, artist or album")
    val searchEmpty = t("Найди песню, чтобы начать", "Search for a song to start")
    val searchNothing = t("Ничего не нашлось", "Nothing found")
    val searchFailed = t("Не получилось найти: проверь интернет (и VPN)", "Search failed: check the internet (and VPN)")
    val searchTab = t("Поиск", "Search")
    val queue = t("Очередь", "Queue")
    val queueEmpty = t("Очередь пуста", "The queue is empty")
    val playNext = t("Играть следующей", "Play next")
    val addToQueue = t("В очередь", "Add to queue")
    val remove = t("Убрать", "Remove")
    val nothingPlaying = t("Ничего не играет", "Nothing is playing")
    val loading = t("Загрузка…", "Loading…")
    val onAir = t("В эфире", "On air")
    val nowPlaying = t("Сейчас играет", "Now playing")
    val theme = t("Тема плеера", "Player theme")
    val themeIntro = t(
        "Та же тема, что на телефоне: в Pulse на телефоне открой Конструктор плеера → «Поделиться» и вставь сюда сообщение с кодом.",
        "The same theme as on the phone: in Pulse on the phone open Player builder → Share and paste the message with the code here.",
    )
    val themeCodeHint = t("Сообщение или код PULSE1-…", "Message or PULSE1-… code")
    val themeBadCode = t("Код не подходит", "This code does not work")
    val themeApply = t("Применить", "Apply")
    val presets = t("Заготовки", "Presets")
    val presetClean = t("Чистый", "Clean")
    val presetOnAir = t("Эфир", "On air")
    val presetLight = t("Светлый", "Light")
    val close = t("Закрыть", "Close")
    val errorBlocked = t("YouTube не отдал эту песню. Если включён VPN — попробуй другой сервер.", "YouTube refused this song. If you use a VPN, try another server.")
    val errorNoSound = t("Не найдено звуковое устройство", "No sound device found")
    val errorNetwork = t("Нет связи с YouTube: проверь интернет (и VPN)", "Cannot reach YouTube: check the internet (and VPN)")
    val errorGeneric = t("Не получилось включить песню — переходим к следующей", "Could not play this song, moving on")
    val play = t("Играть", "Play")
    val pause = t("Пауза", "Pause")
    val next = t("Дальше", "Next")
    val previous = t("Назад", "Previous")
    val shuffle = t("Перемешать", "Shuffle")
    val repeat = t("Повтор", "Repeat")
    val volume = t("Громкость", "Volume")
}
