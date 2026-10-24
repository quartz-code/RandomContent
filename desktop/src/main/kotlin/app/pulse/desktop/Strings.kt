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

    val together = t("Вместе", "Together")
    val togetherIntro = t(
        "Слушайте одну музыку одновременно — с телефонов и компьютеров. Один ведёт, остальные слышат то же, что у него, секунда в секунду.",
        "Listen to the same music at the same time, on phones and computers. One person leads; everyone hears what they play, to the second.",
    )
    val yourName = t("Твоё имя в комнате", "Your name in the room")
    val createRoom = t("Создать комнату", "Create a room")
    val joinRoom = t("Войти по коду", "Join with a code")
    val roomCodeHint = t("Код комнаты или приглашение целиком", "Room code or the whole invite")
    val create = t("Создать", "Create")
    val join = t("Войти", "Join")
    val cancel = t("Отмена", "Cancel")
    val connecting = t("Подключаюсь…", "Connecting…")
    val waking = t(
        "Сервер просыпается — если комнатами давно не пользовались, это до минуты",
        "The server is waking up: up to a minute if nobody used rooms for a while",
    )
    val waitingApproval = t("Ждём, пока хозяин комнаты пустит", "Waiting for the host to let you in")
    val roomCode = t("Код комнаты", "Room code")
    val copyInvite = t("Скопировать приглашение", "Copy the invite")
    val inviteCopied = t("Скопировано — отправь другу", "Copied: send it to a friend")
    fun inviteText(code: String) = t(
        "Заходи слушать вместе в Pulse 🎧\nОткрой Together → Войти, код комнаты: $code\n\nСкопируй это сообщение — Pulse сам подставит код.",
        "Join me in Pulse and let's listen together 🎧\nOpen Together → Join, room code: $code\n\nCopy this message: Pulse fills the code in by itself.",
    )
    val youLead = t("Ты ведёшь: все слышат то, что играет у тебя", "You lead: everyone hears what you play")
    fun hostLeads(host: String) = t("Ведёт $host — твой плеер играет то же самое", "$host leads: your player plays the same")
    fun hostControls(host: String) = t("Музыкой управляет $host", "$host controls the music")
    val suggestHint = t(
        "Чтобы предложить песню, нажми на неё в поиске или в «Моей музыке» — хозяин решит, ставить ли её следующей.",
        "To suggest a song, click it in Search or My music: the host decides whether it plays next.",
    )
    val inRoom = t("В комнате", "In the room")
    val you = t("ты", "you")
    val leads = t("ведёт", "leads")
    val offline = t("не в сети", "offline")
    val wantToJoin = t("Хотят войти", "Want to join")
    val letIn = t("Пустить", "Let in")
    val decline = t("Отказать", "Decline")
    val suggested = t("Предлагают", "Suggested")
    val addNext = t("Следующей", "Play next")
    val removeFromRoom = t("Убрать из комнаты", "Remove from the room")
    val handOver = t("Передать управление", "Hand over")
    val leaveRoom = t("Выйти из комнаты", "Leave the room")
    val reconnecting = t("Связь пропала — восстанавливаю…", "Connection lost: restoring…")
    val suggest = t("Предложить хозяину", "Suggest to the host")
    val suggestionSent = t("Предложение отправлено хозяину", "Suggestion sent to the host")
    val suggestionAdded = t("Хозяин поставил твою песню следующей", "The host put your song next")
    val suggestionDeclined = t("Хозяин не стал ставить песню", "The host passed on your song")
    fun suggestedBy(name: String) = t("Предложение: $name", "Suggested by $name")
    val togetherUnreachable = t("Не получилось связаться с сервером — проверь интернет и попробуй ещё раз", "Could not reach the server: check the internet and try again")
    val togetherNoRoom = t("Комнаты с таким кодом нет — проверь код", "There is no room with this code: check it")
    val togetherRejected = t("Хозяин не пустил в комнату", "The host did not let you in")
    val togetherKicked = t("Тебя убрали из комнаты", "You were removed from the room")
    val togetherLost = t("Комната закрылась — создай новую", "The room closed: create a new one")
    val togetherFull = t("Комната заполнена", "The room is full")
    val togetherNotAllowed = t("Сервер не пускает это приложение", "The server does not let this app in")
    fun togetherOther(detail: String?) = t("Сервер ответил: ${detail.orEmpty()}", "The server said: ${detail.orEmpty()}")

    val importTab = t("Импорт", "Import")
    val importIntro = t(
        "Перенеси музыку из Яндекс Музыки или ВКонтакте: Pulse найдёт каждую песню на YouTube Music, ты проверишь — и всё ляжет плейлистом или лайками в аккаунт.",
        "Bring music over from Yandex Music or VK: Pulse finds each song on YouTube Music, you check, and it all goes to the account as a playlist or likes.",
    )
    val yandex = t("Яндекс Музыка", "Yandex Music")
    val yandexHint = t("Ссылка на открытый плейлист, альбом, исполнителя или «Мне нравится»", "Link to a public playlist, album, artist or liked songs")
    val load = t("Загрузить", "Load")
    val vk = t("ВКонтакте", "VK")
    val vkNeedsPhone = t(
        "Своего входа в ВК у Pulse на компьютере нет — его передаёт телефон:\n1. На телефоне: Настройки → Импорт музыки → Импорт из ВК Музыки, войди в ВК.\n2. Там же: Настройки → Pulse → «Войти на компьютере», включи «Передать и вход ВКонтакте».\n3. Введи код с этого экрана, отправь сообщение себе и вставь сюда.",
        "Pulse on a computer has no VK sign-in of its own: the phone passes it.\n1. On the phone: Settings → Import music → Import from VK Music, sign in to VK.\n2. Then Settings → Pulse → Sign in on a computer, turn on \"Also pass the VK sign-in\".\n3. Type the code from this screen, send the message to yourself and paste it here.",
    )
    val vkSignIn = t("Войти в ВК через телефон", "Sign in to VK with the phone")
    val vkSignedIn = t("Вход в ВК есть", "Signed in to VK")
    val vkSignOut = t("Выйти из ВК", "Sign out of VK")
    val vkMyMusic = t("Моя музыка", "My music")
    val vkPlaylists = t("Мои плейлисты", "My playlists")
    val vkNoPlaylists = t("Плейлистов не нашлось", "No playlists found")
    val vkLinkHint = t("Ссылка на плейлист или альбом ВК", "Link to a VK playlist or album")
    val reading = t("Читаю список…", "Reading the list…")
    fun matching(done: Int, total: Int) = t("Ищу на YouTube Music: $done из $total", "Looking on YouTube Music: $done of $total")
    val stop = t("Хватит", "Stop")
    val matchingOffline = t("YouTube Music перестал отвечать — проверь интернет (и VPN)", "YouTube Music stopped answering: check the internet (and VPN)")
    val resume = t("Продолжить", "Continue")
    val all = t("Все", "All")
    val found = t("Нашлись", "Found")
    val unsure = t("Под вопросом", "Not sure")
    val notFound = t("Не нашлись", "Not found")
    val notFoundRow = t("Не нашлась — найди сам", "Not found: look it up")
    val otherOptions = t("Другие варианты", "Other options")
    val findMyself = t("Найти самому", "Look it up")
    val findSong = t("Найти песню", "Find the song")
    val pick = t("Выбрать", "Pick")
    val selectAll = t("Выбрать все", "Select all")
    val selectNone = t("Снять все", "Select none")
    fun chosenCount(n: Int) = t("Выбрано: ${songsCount(n)}", "Selected: ${songsCount(n)}")
    val playChosen = t("Играть", "Play")
    val playlistName = t("Название плейлиста", "Playlist name")
    val saveAsPlaylist = t("Плейлистом в YouTube Music", "As a YouTube Music playlist")
    val likeAll = t("Лайкнуть все", "Like them all")
    val saveNeedsSignIn = t("Чтобы сохранить в аккаунт, войди в YouTube Music (кнопка аккаунта вверху)", "To keep them in the account, sign in to YouTube Music (the account button at the top)")
    val newImport = t("Другой импорт", "Another import")
    fun saving(done: Int, total: Int) = t("Сохраняю: $done из $total", "Saving: $done of $total")
    fun savedPlaylist(n: Int) = t("Готово: плейлист в YouTube Music, ${songsCount(n)}", "Done: a YouTube Music playlist with ${songsCount(n)}")
    fun savedLikes(n: Int) = t("Готово: ${songsCount(n)} в понравившихся", "Done: ${songsCount(n)} in liked songs")
    fun playingImported(n: Int) = t("Играет: ${songsCount(n)}", "Playing: ${songsCount(n)}")
    val openMyMusic = t("Открыть «Мою музыку»", "Open My music")
    val importYandexRegion = t("Яндекс отвечает только российским адресам — выключи VPN и попробуй снова", "Yandex answers Russian addresses only: turn the VPN off and retry")
    val importYandexPrivate = t("Список закрыт — открой его в Яндекс Музыке или пришли другую ссылку", "The list is private: make it public in Yandex Music or use another link")
    val importBadLink = t("Ссылка не подходит — скопируй её из приложения или сайта ещё раз", "This link does not work: copy it from the app or site again")
    val importNetwork = t("Не получилось загрузить — проверь интернет", "Could not load: check the internet")
    val importVkSession = t("ВК просит войти заново: на телефоне открой Импорт из ВК Музыки, затем передай вход сюда ещё раз", "VK wants a new sign-in: open Import from VK Music on the phone, then pass the sign-in here again")
    fun importVkDenied(answer: String?) = t("ВК не отдал список. Его ответ: ${answer.orEmpty()}", "VK did not give the list. Its answer: ${answer.orEmpty()}")
    val importEmpty = t("В списке нет песен", "There are no songs in the list")
    val importSaveFailed = t("YouTube Music не принял изменения — попробуй ещё раз", "YouTube Music did not take the changes: try again")
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
