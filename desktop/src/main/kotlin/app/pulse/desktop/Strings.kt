package app.pulse.desktop

import java.util.Locale

/** Pulse desktop: the window's texts, Russian or English by the system language. */
object Strings {
    private val locale: Locale = Locale.getDefault()
    private val ru = locale.language in setOf("ru", "uk", "be", "kk")
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

    val recaps = t("Итоги", "Recaps")
    val recapsIntro = t(
        "Итоги есть у каждого месяца, где на этом компьютере набралось хотя бы полчаса музыки, и у каждого года. Они рисуются в твоей теме плеера. У телефона — свои итоги.",
        "Every month with at least half an hour of music on this computer has a recap, and so does every year. They are drawn in your player theme. The phone keeps its own.",
    )
    val recapsEmpty = t(
        "Итогов пока нет: послушай в этом месяце хотя бы полчаса музыки на компьютере — в следующем они появятся здесь.",
        "No recaps yet: listen to at least half an hour of music on this computer this month, and next month it will be here.",
    )
    fun monthName(month: Int): String =
        java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.uppercase() }
    fun periodTitle(year: Int, month: Int?) = if (month == null) t("$year год", "$year") else "${monthName(month)} $year"
    fun dayText(day: java.time.LocalDate): String = day.format(java.time.format.DateTimeFormatter.ofPattern(if (ru) "d MMMM" else "MMMM d", locale))
    val recapIntroMonth = t("Что у тебя звучало в этом месяце, сколько и когда", "What you played this month, how much and when")
    val recapIntroYear = t("Весь твой год в музыке", "Your whole year in music")
    val recapHint = t("Нажми справа или стрелку →, чтобы листать дальше", "Click on the right or press → to go on")
    val recapMinutesKicker = t("Наслушано", "Listened")
    fun recapMinutesWord(n: Long) = if (ru) plural(n, "минуту музыки", "минуты музыки", "минут музыки") else if (n == 1L) "minute of music" else "minutes of music"
    fun hoursMinutes(ms: Long): String {
        val minutes = ms / 60_000
        if (minutes < 60) return t("$minutes мин", "$minutes min")
        return t("${minutes / 60} ч ${minutes % 60} мин", "${minutes / 60} h ${minutes % 60} min")
    }
    fun minutesOf(ms: Long): String {
        val n = ms / 60_000
        return if (ru) "$n ${plural(n, "минута", "минуты", "минут")}" else "$n min"
    }
    fun timesCount(n: Int) = if (ru) "$n ${plural(n.toLong(), "раз", "раза", "раз")}" else "$n time${if (n == 1) "" else "s"}"
    fun playsWord(n: Int) = if (ru) plural(n.toLong(), "прослушивание", "прослушивания", "прослушиваний") else if (n == 1) "play" else "plays"
    fun songsWord(n: Int) = if (ru) plural(n.toLong(), "песня", "песни", "песен") else if (n == 1) "song" else "songs"
    fun artistsWord(n: Int) = if (ru) plural(n.toLong(), "артист", "артиста", "артистов") else if (n == 1) "artist" else "artists"
    fun songOf(year: Boolean) = if (year) t("Песня года", "Song of the year") else t("Песня месяца", "Song of the month")
    fun artistOf(year: Boolean) = if (year) t("Артист года", "Artist of the year") else t("Артист месяца", "Artist of the month")
    val recapTimeKicker = t("Твоё время для музыки", "Your time for music")
    val partNames = listOf(t("Утро", "Morning"), t("День", "Day"), t("Вечер", "Evening"), t("Ночь", "Night"))
    val partLines = listOf(
        t("Музыка раньше всего остального: больше всего звучало с 5 до 11.", "Music before anything else: most of it played from 5 to 11."),
        t("Музыка под дела: больше всего звучало с 11 до 17.", "Music for getting things done: most of it played from 11 to 17."),
        t("Вечера — для музыки: больше всего звучало с 17 до 23.", "Evenings are for music: most of it played from 17 to 23."),
        t("Ночной слушатель: больше всего звучало после 23.", "A night listener: most of it played after 23."),
    )
    fun busiestDay(day: java.time.LocalDate, ms: Long) = t("Самый музыкальный день — ${dayText(day)}: ${hoursMinutes(ms)}.", "The most musical day was ${dayText(day)}: ${hoursMinutes(ms)}.")
    val recapMonthsKicker = t("Самый музыкальный месяц", "The most musical month")
    val recapNewKicker = t("Впервые", "First time")
    fun newSongsWord(n: Int) = if (ru) plural(n.toLong(), "песня прозвучала у тебя впервые", "песни прозвучали у тебя впервые", "песен прозвучали у тебя впервые") else if (n == 1) "song you heard for the first time" else "songs you heard for the first time"
    val recapStreakKicker = t("Подряд", "In a row")
    fun streakWord(n: Int) = if (ru) plural(n.toLong(), "день с музыкой подряд", "дня с музыкой подряд", "дней с музыкой подряд") else if (n == 1) "day with music in a row" else "days with music in a row"
    fun activeDays(n: Int) = if (ru) "Музыка звучала $n ${plural(n.toLong(), "день", "дня", "дней")}" else "Music played on $n day${if (n == 1) "" else "s"}"
    val recapCardKicker = t("Всё на одной карточке", "All on one card")
    val recapCardBrand = t("Pulse · итоги", "Pulse · recap")
    val recapCardSongs = t("Песни", "Songs")
    val recapCardArtists = t("Артисты", "Artists")
    val saveImage = t("Сохранить картинкой", "Save as a picture")
    fun savedTo(path: String) = t("Сохранено: $path", "Saved: $path")
    val saveFailed = t("Не получилось сохранить картинку", "Could not save the picture")
    fun recapOffer(title: String) = t("$title: итоги готовы", "$title: your recap is ready")
    val recapOfferText = t(
        "Сколько музыки, что звучало чаще всего и в какое время — в твоей теме плеера. Итоги лежат и под кнопкой со столбиками вверху.",
        "How much music, what played most and when, in your player theme. Recaps are also under the bar-chart button at the top.",
    )
    val recapOfferOpen = t("Смотреть", "Open")
    val recapOfferLater = t("Позже", "Later")
    // "My pulse": the same words as on the phone.
    val pulseTab = t("Мой пульс", "My pulse")
    val pulseListen = t("Слушать", "Listen")
    val pulseTagline = t("Бесконечная музыка под тебя: от того, что ты любишь, и дальше.", "Endless music for you: from what you love, and onwards.")
    val pulseTune = t("Настроить", "Tune")
    val pulseLike = t("Нравится", "Like")
    val pulseDislike = t("Не моё", "Not for me")
    val pulseNext = t("Дальше", "Next")
    val pulseSignInHint = t("Войди в YouTube Music, и пульс будет учиться ещё и на твоём аккаунте.", "Sign in to YouTube Music and the pulse learns from your account too.")
    val pulseMix = t("Микс", "Mix")
    val pulseMixHint = t("В основном новое, иногда что-то знакомое", "Mostly new, now and then something familiar")
    val pulseUnfamiliar = t("Незнакомое", "Unfamiliar")
    val pulseUnfamiliarHint = t("Только песни, которых нет в твоих лайках и истории", "Only songs that are not in your likes and history")
    val pulseFavorite = t("Любимое", "Favourite")
    val pulseFavoriteHint = t("Рядом с тем, что ты любишь; знакомые песни тоже будут", "Close to what you love; familiar songs too")
    val pulsePopular = t("Популярное", "Popular")
    val pulsePopularHint = t("От того, что популярно прямо сейчас", "Starting from what is popular right now")
    val pulseLanguageAny = t("Любой", "Any")
    val pulseLanguageRussian = t("Русское", "Russian")
    val pulseLanguageForeign = t("Иностранное", "Foreign")
    val pulseMorning = t("Утро", "Morning")
    val pulseDay = t("День", "Day")
    val pulseEvening = t("Вечер", "Evening")
    val pulseNight = t("Ночь", "Night")
    val pulseCharacter = t("Характер", "Character")
    val pulseLanguage = t("Язык", "Language")
    val pulseMood = t("Настроение и жанр", "Mood and genre")
    val pulseMoodAny = t("Любое", "Any")
    val pulseMoodsUnavailable = t("YouTube Music не прислал настроения: проверь интернет (и VPN) и открой настройки ещё раз.", "YouTube Music sent no moods: check the internet (and VPN) and open the settings again.")
    val pulseApply = t("Применить", "Apply")
    val pulsePageMusic = t("Музыка", "Music")
    val pulsePageLook = t("Вид", "Look")
    val pulsePageSchedule = t("Расписание", "Schedule")
    val pulseColors = t("Цвет сердца и кнопок", "Heart and button colour")
    val pulsePalettePulse = t("Пульс", "Pulse")
    val pulsePaletteOcean = t("Океан", "Ocean")
    val pulsePaletteNeon = t("Неон", "Neon")
    val pulsePaletteForest = t("Лес", "Forest")
    val pulsePaletteSunset = t("Закат", "Sunset")
    val pulsePaletteIce = t("Лёд", "Ice")
    val pulseArtworkLights = t("Фон в цветах обложки", "Background in the artwork's colours")
    val pulseArtworkLightsHint = t("Огни позади, пока играет песня; сердце и кнопки красит «Из обложки» выше", "The lights behind, while a song plays; the heart and buttons follow \"From the artwork\" above")
    val pulseShape = t("Сердце", "Heart")
    val pulseShapeDrop = t("Капля", "Drop")
    val pulseShapeCircle = t("Круг", "Circle")
    val pulseShapeFlower = t("Цветок", "Flower")
    val pulseShapeStar = t("Звезда", "Star")
    val pulseEchoes = t("Кольца вокруг сердца", "Rings around the heart")
    val pulseBackground = t("Фон", "Background")
    val pulseBackgroundLights = t("Огни", "Lights")
    val pulseBackgroundArtwork = t("Обложка", "Artwork")
    val pulseBackgroundCalm = t("Спокойный", "Calm")
    val pulseMotion = t("Движение", "Motion")
    val pulseMotionCalm = t("Плавное", "Gentle")
    val pulseMotionNormal = t("Обычное", "Normal")
    val pulseMotionLively = t("Живое", "Lively")
    val pulsePaletteArtwork = t("Из обложки", "From the artwork")
    val pulsePaletteCustom = t("Свой", "Your own")
    val pulseCustomStart = t("Начало", "Start")
    val pulseCustomEnd = t("Конец", "End")
    val pulseShapeHeart = t("Сердечко", "Heart")
    val pulseShapeWave = t("Волна", "Wave")
    val pulseBackgroundParticles = t("Искры", "Sparks")
    val pulseBackgroundAurora = t("Сияние", "Aurora")
    val pulseBackgroundWaves = t("Волны", "Waves")
    val pulseReaction = t("Реакция на музыку", "Reaction to the music")
    val pulseReactionLess = t("Спокойнее", "Calmer")
    val pulseReactionMore = t("Сильнее", "Stronger")
    val pulseReactionDefault = t("Лучшее для этого фона", "Best for this background")
    val pulseShare = t("Скопировать код", "Copy code")
    val pulsePaste = t("Вставить код", "Paste code")
    fun pulseShareText(code: String) = t("Мой вид «Моего пульса» в Pulse: $code", "My \"My pulse\" look in Pulse: $code")
    val pulseCopied = t("Код вида скопирован — отправь его другу", "Look code copied: send it to a friend")
    val pulseApplied = t("Вид применён", "Look applied")
    val pulseNoCode = t("В скопированном тексте нет кода вида", "The copied text has no look code")
    val pulseScheduleSwitch = t("Менять по времени суток", "Change with the time of day")
    val pulseScheduleHint = t("Пульс сам переключится, когда начнётся новая часть дня.", "The pulse switches by itself when a new part of the day begins.")
    fun pulsePartHours(from: Int, to: Int) = "$from:00–$to:00"
    val pulseNow = t("сейчас", "now")
    val pulseAsUsual = t("Как обычно", "As usual")
    val pulseMoodAsUsual = t("Любое настроение", "Any mood")
    val pulseDisliked = t("Учтено: такого будет меньше", "Noted: less of this")
    val pulseNothing = t("Песни не пришли: проверь интернет (и VPN) или ослабь настройки.", "No songs came: check the internet (and VPN), or loosen the settings.")
    fun songsCount(n: Int) = if (ru) "$n ${plural(n, "песня", "песни", "песен")}" else "$n song${if (n == 1) "" else "s"}"
    private fun plural(n: Int, one: String, few: String, many: String): String = plural(n.toLong(), one, few, many)

    private fun plural(n: Long, one: String, few: String, many: String): String {
        val m10 = (n % 10).toInt()
        val m100 = (n % 100).toInt()
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }
}
