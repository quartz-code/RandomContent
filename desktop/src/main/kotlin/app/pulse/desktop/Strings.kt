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
    val like = t("Нравится", "Like")
    val myMusic = t("Моя музыка", "My music")
    val account = t("Аккаунт", "Account")
    val signIn = t("Войти", "Sign in")
    val signOut = t("Выйти из аккаунта", "Sign out")
    val signInWithPhone = t("Войти через телефон", "Sign in with the phone")
    val signInSteps = t(
        "1. На телефоне в Pulse открой Настройки → Pulse → «Войти на компьютере».\n2. Введи там этот код и нажми «Создать сообщение».\n3. Отправь сообщение себе (например, в «Избранное» Telegram) и вставь сюда.",
        "1. On the phone, in Pulse, open Settings → Pulse → Sign in on a computer.\n2. Type this code there and tap Make the message.\n3. Send the message to yourself (for example, to Saved Messages in Telegram) and paste it here.",
    )
    val newCode = t("Другой код", "New code")
    val pasteMessage = t("Сообщение с телефона (PULSELOGIN1-…)", "Message from the phone (PULSELOGIN1-…)")
    val loginNotFound = t("Здесь нет сообщения для входа — вставь его целиком", "There is no sign-in message here: paste all of it")
    val loginWrongCode = t("Сообщение не открывается этим кодом — создай его на телефоне с кодом выше", "This code does not open the message: make it on the phone with the code above")
    val loginExpired = t("Сообщение устарело (больше 30 минут) — создай новое", "The message is older than 30 minutes: make a new one")
    val loginRejected = t("YouTube не принял вход — войди заново на телефоне и повтори", "YouTube refused the sign-in: sign in again on the phone and retry")
    fun signedInAs(name: String) = t("Вход выполнен: $name", "Signed in as $name")
    val libraryNeedsSignIn = t("Войди в аккаунт YouTube Music, чтобы видеть свои лайки и плейлисты", "Sign in to YouTube Music to see your likes and playlists")
    val libraryFailed = t("Часть библиотеки не загрузилась — нажми «Обновить»", "Part of the library did not load: press Refresh")
    val refresh = t("Обновить", "Refresh")
    val likedSongs = t("Понравившиеся", "Liked songs")
    val playlists = t("Плейлисты", "Playlists")
    val playlistEmpty = t("В плейлисте пусто", "The playlist is empty")
    val back = t("Назад", "Back")
    fun songsCount(n: Int) = if (ru) "$n ${plural(n, "песня", "песни", "песен")}" else "$n song${if (n == 1) "" else "s"}"
    private fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }
}
