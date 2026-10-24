package app.pulse.desktop

import com.google.protobuf.MessageLite
import com.metrolist.music.listentogether.ApproveJoinPayload
import com.metrolist.music.listentogether.ApproveSuggestionPayload
import com.metrolist.music.listentogether.BufferReadyPayload
import com.metrolist.music.listentogether.CreateRoomPayload
import com.metrolist.music.listentogether.ErrorPayload
import com.metrolist.music.listentogether.HostChangedPayload
import com.metrolist.music.listentogether.JoinApprovedPayload
import com.metrolist.music.listentogether.JoinRequestPayload
import com.metrolist.music.listentogether.JoinRoomPayload
import com.metrolist.music.listentogether.KickUserPayload
import com.metrolist.music.listentogether.MessageCodec
import com.metrolist.music.listentogether.MessageTypes
import com.metrolist.music.listentogether.PingPayload
import com.metrolist.music.listentogether.PlaybackActionPayload
import com.metrolist.music.listentogether.PlaybackActions
import com.metrolist.music.listentogether.PongPayload
import com.metrolist.music.listentogether.ReconnectPayload
import com.metrolist.music.listentogether.ReconnectedPayload
import com.metrolist.music.listentogether.RejectJoinPayload
import com.metrolist.music.listentogether.RejectSuggestionPayload
import com.metrolist.music.listentogether.RoomCreatedPayload
import com.metrolist.music.listentogether.RoomState
import com.metrolist.music.listentogether.ServerClock
import com.metrolist.music.listentogether.SuggestTrackPayload
import com.metrolist.music.listentogether.SuggestionReceivedPayload
import com.metrolist.music.listentogether.SyncStatePayload
import com.metrolist.music.listentogether.TrackInfo
import com.metrolist.music.listentogether.TransferHostPayload
import com.metrolist.music.listentogether.UserDisconnectedPayload
import com.metrolist.music.listentogether.UserInfo
import com.metrolist.music.listentogether.UserJoinedPayload
import com.metrolist.music.listentogether.UserLeftPayload
import com.metrolist.music.listentogether.UserReconnectedPayload
import com.metrolist.music.listentogether.currentTrackOrNull
import com.metrolist.music.listentogether.positionOrNull
import com.metrolist.music.listentogether.queue
import com.metrolist.music.listentogether.serverTimeOrNull
import com.metrolist.music.listentogether.trackIdOrNull
import com.metrolist.music.listentogether.trackInfoOrNull
import com.metrolist.music.listentogether.users
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Pulse desktop: Listen Together — the phone's rooms, server and protocol, so a phone and a computer
 * share one room. The host's player drives the room; a guest's player loads each song, tells the
 * server it is ready, and then plays where the host is.
 *
 * Everything that touches the session runs on one thread ([serial]): socket callbacks, the player's
 * "ready" and the host's watchers arrive from different threads.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TogetherSession(
    private val scope: CoroutineScope,
    private val player: RoomPlayer,
    private val serverUrl: String = DEFAULT_SERVER,
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    enum class Phase { IDLE, CONNECTING, WAKING, WAITING_APPROVAL, IN_ROOM }

    enum class Problem { UNREACHABLE, ROOM_NOT_FOUND, REJECTED, KICKED, ROOM_LOST, ROOM_FULL, NOT_ALLOWED, OTHER }

    enum class Notice { SUGGESTION_SENT, SUGGESTION_ADDED, SUGGESTION_DECLINED }

    data class Member(val id: String, val name: String, val host: Boolean, val online: Boolean)
    data class JoinAsk(val userId: String, val name: String)
    data class Suggestion(val id: String, val from: String, val track: Track)

    data class State(
        val phase: Phase = Phase.IDLE,
        val roomCode: String? = null,
        val myId: String? = null,
        val isHost: Boolean = false,
        val members: List<Member> = emptyList(),
        val requests: List<JoinAsk> = emptyList(),
        val suggestions: List<Suggestion> = emptyList(),
        /** The connection dropped while in the room; it is being restored. */
        val reconnecting: Boolean = false,
        val problem: Problem? = null,
        /** The server's own words, for [Problem.OTHER]. */
        val problemDetail: String? = null,
        val notice: Notice? = null,
    ) {
        val inRoom: Boolean get() = phase == Phase.IN_ROOM
        val hostName: String? get() = members.firstOrNull { it.host }?.name
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val serial = Dispatchers.Default.limitedParallelism(1)
    private val codec = MessageCodec()
    private val clock = ServerClock(clockMs)
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // A sleeping free server answers the upgrade only once it is up.
        .readTimeout(75, TimeUnit.SECONDS)
        .pingInterval(60, TimeUnit.SECONDS)
        .build()

    private sealed interface Pending {
        data class Create(val name: String) : Pending
        data class Join(val code: String, val name: String) : Pending
    }

    private var socket: WebSocket? = null
    /** Which socket the callbacks belong to; an old one may still report after a new one opens. */
    private var socketId = 0
    private var pending: Pending? = null
    private var sessionToken: String? = null
    private var myName: String = ""
    private var reconnectAttempts = 0
    private var pingJob: Job? = null
    private var pingSequence = 0L
    private var hostJob: Job? = null
    /** Read on the socket's thread when it closes. */
    @Volatile private var closedSignal: CompletableDeferred<Unit>? = null

    // Host: what the room last heard, so only changes are sent.
    private var sentTrackId: String? = null
    private var sentPlaying = false

    // Guest: the room's song, and how far this player is with it.
    private var roomTrack: TrackInfo? = null
    private var roomPlaying = false
    private var loadingTrackId: String? = null
    private var loadedTrackId: String? = null

    /** A free server sleeps after a quarter of an hour alone; a plain request wakes it. */
    fun wake() {
        scope.launch(Dispatchers.IO) { knock() }
    }

    fun create(name: String) = scope.launch(serial) { start(Pending.Create(name.trim())) }

    fun join(code: String, name: String) = scope.launch(serial) { start(Pending.Join(code.trim().uppercase(), name.trim())) }

    fun leave() = scope.launch(serial) {
        if (socket != null && _state.value.roomCode != null) send(MessageTypes.LEAVE_ROOM, null)
        reset(problem = null)
    }

    fun approve(userId: String) = scope.launch(serial) {
        send(MessageTypes.APPROVE_JOIN, ApproveJoinPayload(userId))
        _state.update { s -> s.copy(requests = s.requests.filterNot { it.userId == userId }) }
    }

    fun reject(userId: String) = scope.launch(serial) {
        send(MessageTypes.REJECT_JOIN, RejectJoinPayload(userId, null))
        _state.update { s -> s.copy(requests = s.requests.filterNot { it.userId == userId }) }
    }

    fun kick(userId: String) = scope.launch(serial) { send(MessageTypes.KICK_USER, KickUserPayload(userId, null)) }

    fun makeHost(userId: String) = scope.launch(serial) { send(MessageTypes.TRANSFER_HOST, TransferHostPayload(userId)) }

    /** Guest: the host decides whether it plays next. */
    fun suggest(track: Track) = scope.launch(serial) {
        if (!_state.value.inRoom || _state.value.isHost) return@launch
        send(MessageTypes.SUGGEST_TRACK, SuggestTrackPayload(track.toTrackInfo()))
        _state.update { it.copy(notice = Notice.SUGGESTION_SENT) }
    }

    fun acceptSuggestion(id: String) = scope.launch(serial) {
        send(MessageTypes.APPROVE_SUGGESTION, ApproveSuggestionPayload(id))
        _state.update { s -> s.copy(suggestions = s.suggestions.filterNot { it.id == id }) }
    }

    fun declineSuggestion(id: String) = scope.launch(serial) {
        send(MessageTypes.REJECT_SUGGESTION, RejectSuggestionPayload(id, null))
        _state.update { s -> s.copy(suggestions = s.suggestions.filterNot { it.id == id }) }
    }

    /** For drawing the window in tests without a server. */
    internal fun showForPreview(preview: State) = _state.update { preview }

    fun dismissNotice() = _state.update { it.copy(notice = null, problem = null, problemDetail = null) }

    /**
     * On exit: leave the room properly, so the others are not left waiting for this player to come
     * back. Waits a moment for the server to confirm; the socket's threads die with the app.
     */
    fun shutdown() = runBlocking {
        withTimeoutOrNull(1_500) {
            val closed = withContext(serial) {
                val open = socket ?: return@withContext null
                if (_state.value.roomCode != null) send(MessageTypes.LEAVE_ROOM, null)
                closedSignal = CompletableDeferred()
                open.close(1000, null)
                closedSignal
            }
            closed?.await()
        }
        Unit
    }

    // ---- connection ----

    private suspend fun start(action: Pending) {
        reset(problem = null)
        myName = when (action) {
            is Pending.Create -> action.name
            is Pending.Join -> action.name
        }
        pending = action
        _state.value = State(phase = Phase.CONNECTING)
        // Say "the server is waking up" if it is slow, rather than look stuck.
        scope.launch(serial) {
            delay(WAKING_AFTER_MS)
            if (pending === action && _state.value.phase == Phase.CONNECTING) _state.update { it.copy(phase = Phase.WAKING) }
        }
        withContext(Dispatchers.IO) { knock() }
        if (pending === action && socket == null) open()
    }

    private fun open() {
        val id = ++socketId
        val request = Request.Builder()
            .url(serverUrl)
            // The server lets clients in by their name; this is Pulse's, on the phone as here.
            .header("User-Agent", USER_AGENT)
            .build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch(serial) { if (id == socketId) onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                scope.launch(serial) { if (id == socketId) onMessage(data) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                closedSignal?.complete(Unit)
                scope.launch(serial) { if (id == socketId) onDropped() }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                closedSignal?.complete(Unit)
                scope.launch(serial) { if (id == socketId) onDropped() }
            }
        })
    }

    private fun onOpen() {
        reconnectAttempts = 0
        clock.reset()
        startPings()
        val token = sessionToken
        if (token != null) {
            send(MessageTypes.RECONNECT, ReconnectPayload(token))
            return
        }
        when (val action = pending) {
            is Pending.Create -> send(MessageTypes.CREATE_ROOM, CreateRoomPayload(action.name))
            is Pending.Join -> {
                send(MessageTypes.JOIN_ROOM, JoinRoomPayload(action.code, action.name))
                _state.update { it.copy(phase = Phase.WAITING_APPROVAL) }
            }
            null -> Unit
        }
    }

    private fun onDropped() {
        socket = null
        pingJob?.cancel()
        if (sessionToken == null) {
            // Never got into a room: the server is down or unreachable.
            if (_state.value.phase != Phase.IDLE) reset(Problem.UNREACHABLE)
            return
        }
        if (reconnectAttempts >= MAX_RECONNECTS) {
            reset(Problem.ROOM_LOST)
            return
        }
        _state.update { it.copy(reconnecting = true) }
        val wait = RECONNECT_DELAYS_MS[reconnectAttempts.coerceAtMost(RECONNECT_DELAYS_MS.lastIndex)]
        reconnectAttempts++
        scope.launch(serial) {
            delay(wait)
            if (socket == null && sessionToken != null) open()
        }
    }

    private fun reset(problem: Problem?, detail: String? = null) {
        // Callbacks of the socket closed here are ignored from now on.
        socketId++
        socket?.close(1000, null)
        socket = null
        pingJob?.cancel()
        stopHosting()
        sessionToken = null
        pending = null
        reconnectAttempts = 0
        roomTrack = null
        loadingTrackId = null
        loadedTrackId = null
        player.follow(null)
        _state.value = State(problem = problem, problemDetail = detail)
    }

    private fun startPings() {
        pingJob?.cancel()
        pingJob = scope.launch(serial) {
            // A few quick ones first: the guest needs the server's clock before it can join in on time.
            repeat(3) {
                ping()
                delay(250)
            }
            while (isActive) {
                ping()
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun ping() = send(MessageTypes.PING, PingPayload(clockMs(), ++pingSequence))

    private fun send(type: String, payload: MessageLite?) {
        val data = runCatching { codec.encode(type, payload) }.getOrNull() ?: return
        socket?.send(data.toByteString())
    }

    private fun knock() {
        val url = healthUrl(serverUrl) ?: return
        runCatching { http.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute().close() }
    }

    // ---- messages ----

    private fun onMessage(data: ByteArray) {
        val (type, bytes) = runCatching { codec.decode(data) }.getOrNull() ?: return
        val payload = runCatching { codec.decodePayload(type, bytes) }.getOrNull()
        when (type) {
            MessageTypes.ROOM_CREATED -> (payload as? RoomCreatedPayload)?.let(::onRoomCreated)
            MessageTypes.JOIN_APPROVED -> (payload as? JoinApprovedPayload)?.let(::onJoinApproved)
            MessageTypes.JOIN_REJECTED -> reset(Problem.REJECTED)
            MessageTypes.RECONNECTED -> (payload as? ReconnectedPayload)?.let(::onReconnected)
            MessageTypes.JOIN_REQUEST -> (payload as? JoinRequestPayload)?.let { p ->
                _state.update { s -> s.copy(requests = s.requests.filterNot { it.userId == p.userId } + JoinAsk(p.userId, p.username)) }
            }
            MessageTypes.USER_JOINED -> (payload as? UserJoinedPayload)?.let { p ->
                _state.update { s ->
                    s.copy(
                        members = s.members.filterNot { it.id == p.userId } + Member(p.userId, p.username, host = false, online = true),
                        requests = s.requests.filterNot { it.userId == p.userId },
                    )
                }
            }
            MessageTypes.USER_LEFT -> (payload as? UserLeftPayload)?.let { p ->
                _state.update { s -> s.copy(members = s.members.filterNot { it.id == p.userId }) }
            }
            MessageTypes.USER_DISCONNECTED -> (payload as? UserDisconnectedPayload)?.let { p -> setOnline(p.userId, false) }
            MessageTypes.USER_RECONNECTED -> (payload as? UserReconnectedPayload)?.let { p -> setOnline(p.userId, true) }
            MessageTypes.HOST_CHANGED -> (payload as? HostChangedPayload)?.let(::onHostChanged)
            MessageTypes.KICKED -> reset(Problem.KICKED)
            MessageTypes.SYNC_PLAYBACK -> (payload as? PlaybackActionPayload)?.let(::onPlayback)
            MessageTypes.SYNC_STATE -> (payload as? SyncStatePayload)?.let { p ->
                if (!_state.value.isHost) followRoom(p.currentTrackOrNull, p.isPlaying, p.position, p.lastUpdate, p.queue.orEmpty())
            }
            MessageTypes.SUGGESTION_RECEIVED -> (payload as? SuggestionReceivedPayload)?.let { p ->
                if (_state.value.isHost) {
                    _state.update { s ->
                        s.copy(suggestions = s.suggestions.filterNot { it.id == p.suggestionId } + Suggestion(p.suggestionId, p.fromUsername, p.trackInfo.toTrack()))
                    }
                }
            }
            MessageTypes.SUGGESTION_APPROVED -> _state.update { it.copy(notice = Notice.SUGGESTION_ADDED) }
            MessageTypes.SUGGESTION_REJECTED -> _state.update { it.copy(notice = Notice.SUGGESTION_DECLINED) }
            MessageTypes.PONG -> (payload as? PongPayload)?.let { p ->
                val first = clock.recordPong(p.clientTime, p.serverReceiveTime, p.serverSendTime)
                // Until now a guest could not tell how far the host has got; ask again.
                if (first && _state.value.inRoom && !_state.value.isHost) send(MessageTypes.REQUEST_SYNC, null)
            }
            MessageTypes.ERROR -> (payload as? ErrorPayload)?.let(::onError)
        }
    }

    private fun onRoomCreated(p: RoomCreatedPayload) {
        sessionToken = p.sessionToken
        pending = null
        _state.value = State(
            phase = Phase.IN_ROOM,
            roomCode = p.roomCode,
            myId = p.userId,
            isHost = true,
            members = listOf(Member(p.userId, myName, host = true, online = true)),
        )
        startHosting(roomTrackId = null)
    }

    private fun onJoinApproved(p: JoinApprovedPayload) {
        sessionToken = p.sessionToken
        pending = null
        _state.value = State(phase = Phase.IN_ROOM, roomCode = p.roomCode, myId = p.userId, isHost = false, members = members(p.state))
        startFollowing(p.state)
    }

    private fun onReconnected(p: ReconnectedPayload) {
        _state.update {
            it.copy(phase = Phase.IN_ROOM, roomCode = p.roomCode, myId = p.userId, isHost = p.isHost, members = members(p.state), reconnecting = false)
        }
        if (p.isHost) startHosting(roomTrackId = p.state.currentTrackOrNull?.id) else startFollowing(p.state)
    }

    private fun onHostChanged(p: HostChangedPayload) {
        val wasHost = _state.value.isHost
        val nowHost = p.newHostId == _state.value.myId
        _state.update { s -> s.copy(isHost = nowHost, members = s.members.map { it.copy(host = it.id == p.newHostId) }) }
        when {
            nowHost && !wasHost -> {
                // The host left or handed over: from now on this player drives the room.
                startHosting(roomTrackId = roomTrack?.id)
            }
            !nowHost && wasHost -> {
                stopHosting()
                // The room is on this player's song already.
                val s = player.state.value
                roomTrack = s.current?.toTrackInfo()
                loadedTrackId = s.current?.videoId?.takeIf { s.loading == null }
                loadingTrackId = null
                player.follow(p.newHostName)
                send(MessageTypes.REQUEST_SYNC, null)
            }
            !nowHost -> player.follow(p.newHostName)
        }
    }

    private fun onError(p: ErrorPayload) {
        when (p.code) {
            "room_not_found", "invalid_room_code" -> reset(Problem.ROOM_NOT_FOUND)
            "room_full" -> reset(Problem.ROOM_FULL)
            "host_not_allowed" -> reset(Problem.NOT_ALLOWED)
            "session_not_found" -> {
                // The server forgot this seat (it restarted, or the break was too long).
                val code = _state.value.roomCode
                val wasGuest = !_state.value.isHost
                val name = myName
                reset(if (wasGuest && code != null) null else Problem.ROOM_LOST)
                if (wasGuest && code != null) scope.launch(serial) { start(Pending.Join(code, name)) }
            }
            // A guest pressing play, a stale track: nothing the user needs to see.
            "not_host", "stale_track", "no_track", "duplicate_track", "not_in_room" -> Unit
            else -> if (_state.value.phase != Phase.IN_ROOM) reset(Problem.OTHER, p.message.ifBlank { p.code })
        }
    }

    private fun setOnline(userId: String, online: Boolean) =
        _state.update { s -> s.copy(members = s.members.map { if (it.id == userId) it.copy(online = online) else it }) }

    private fun members(room: RoomState): List<Member> =
        room.users.map { u: UserInfo -> Member(u.userId, u.username, u.isHost, u.isConnected) }

    // ---- host: tell the room what this player does ----

    /** [roomTrackId]: the song the room is on already, which needs no change of track, just the position. */
    private fun startHosting(roomTrackId: String?) {
        stopHosting()
        player.follow(null)
        roomTrack = null
        loadingTrackId = null
        loadedTrackId = null
        val s = player.state.value
        if (roomTrackId != null && roomTrackId == s.current?.videoId) {
            sentTrackId = roomTrackId
            sentPlaying = s.isPlaying
            sendPlayState(roomTrackId, s.isPlaying)
        } else {
            sentTrackId = null
            sentPlaying = false
        }
        hostJob = scope.launch(serial) {
            launch { player.state.collect(::onHostPlayer) }
            launch {
                player.seeks.collect { ms ->
                    val id = sentTrackId ?: return@collect
                    send(MessageTypes.PLAYBACK_ACTION, action(PlaybackActions.SEEK, id, ms))
                }
            }
            launch {
                player.state.map { player.upcoming().map(Track::videoId) }.distinctUntilChanged().drop(1).collect {
                    // Several quick edits to the queue go out as one.
                    delay(QUEUE_DEBOUNCE_MS)
                    if (sentTrackId != null) send(MessageTypes.PLAYBACK_ACTION, PlaybackActionPayload(PlaybackActions.SYNC_QUEUE, queue = upcomingInfo()))
                }
            }
            launch {
                // Guests who drifted or missed a message find the host again within a few seconds.
                while (isActive) {
                    delay(HEARTBEAT_MS)
                    val id = sentTrackId
                    if (id != null && sentPlaying && player.livePlaying) {
                        send(MessageTypes.PLAYBACK_ACTION, action(PlaybackActions.PLAY, id, player.livePositionMs))
                    }
                }
            }
        }
    }

    private fun stopHosting() {
        hostJob?.cancel()
        hostJob = null
    }

    private fun onHostPlayer(s: PlayerState) {
        if (!_state.value.isHost || !_state.value.inRoom) return
        val track = s.current
        if (track?.videoId != sentTrackId) {
            sentTrackId = track?.videoId
            sentPlaying = false
            if (track != null) {
                send(MessageTypes.PLAYBACK_ACTION, PlaybackActionPayload(PlaybackActions.CHANGE_TRACK, trackInfo = track.toTrackInfo(), queue = upcomingInfo()))
            }
        }
        if (track != null && s.loading == null && s.isPlaying != sentPlaying) {
            sentPlaying = s.isPlaying
            sendPlayState(track.videoId, s.isPlaying)
        }
    }

    private fun sendPlayState(trackId: String, playing: Boolean) {
        send(MessageTypes.PLAYBACK_ACTION, action(if (playing) PlaybackActions.PLAY else PlaybackActions.PAUSE, trackId, player.livePositionMs))
    }

    /** Stamped with the server's clock, so the server can add the time the message spent travelling. */
    private fun action(name: String, trackId: String, positionMs: Long) =
        PlaybackActionPayload(name, trackId = trackId, position = positionMs, capturedAtServerTime = clock.now())

    private fun upcomingInfo(): List<TrackInfo> =
        player.upcoming().filter { it.videoId != sentTrackId }.distinctBy { it.videoId }.take(MAX_QUEUE).map { it.toTrackInfo() }

    // ---- guest: do what the room does ----

    private fun startFollowing(room: RoomState) {
        stopHosting()
        // After a dropped connection the song is usually still here: keep it rather than start over.
        val local = player.state.value
        loadedTrackId = local.current?.videoId?.takeIf { local.loading == null && local.error == null }
        loadingTrackId = null
        player.follow(room.users.firstOrNull { it.isHost }?.username ?: "")
        followRoom(room.currentTrackOrNull, room.isPlaying, room.position, room.lastUpdate, room.queue)
    }

    private fun followRoom(track: TrackInfo?, playing: Boolean, position: Long, lastUpdate: Long, queue: List<TrackInfo>) {
        roomTrack = track
        roomPlaying = playing
        if (track == null) {
            // Nothing on in the room yet: what played here before joining stops.
            player.roomPause()
            return
        }
        when (track.id) {
            loadedTrackId -> {
                player.roomQueue(queue.map { it.toTrack() })
                catchUp(clock.positionAt(position, lastUpdate.takeIf { it > 0 }, playing), playing)
            }
            loadingTrackId -> Unit
            else -> load(track, queue)
        }
    }

    private fun onPlayback(p: PlaybackActionPayload) {
        val s = _state.value
        // The host hears its own actions back; queue additions from approved suggestions are the exception.
        if (s.isHost) {
            if (p.action == PlaybackActions.QUEUE_ADD) {
                p.trackInfoOrNull?.let { info -> if (player.upcoming().none { it.videoId == info.id }) player.playNext(info.toTrack()) }
            }
            return
        }
        when (p.action) {
            PlaybackActions.CHANGE_TRACK -> p.trackInfoOrNull?.let { track ->
                roomTrack = track
                roomPlaying = false
                load(track, p.queue.orEmpty())
            }
            PlaybackActions.PLAY, PlaybackActions.PAUSE, PlaybackActions.SEEK -> {
                if (p.action != PlaybackActions.SEEK) roomPlaying = p.action == PlaybackActions.PLAY
                val trackId = p.trackIdOrNull ?: roomTrack?.id ?: return
                if (trackId != loadedTrackId) {
                    // Still loading: the server sends the position again once this player says it is ready.
                    val track = roomTrack?.takeIf { it.id == trackId }
                    if (track != null && trackId != loadingTrackId) load(track, emptyList())
                    if (track == null && trackId != loadingTrackId) send(MessageTypes.REQUEST_SYNC, null)
                    return
                }
                val target = clock.positionAt(p.positionOrNull ?: 0L, p.serverTimeOrNull, roomPlaying)
                catchUp(target, roomPlaying, heartbeat = p.action == PlaybackActions.PLAY)
            }
            PlaybackActions.SYNC_QUEUE, PlaybackActions.QUEUE_ADD, PlaybackActions.QUEUE_REMOVE, PlaybackActions.QUEUE_CLEAR ->
                player.roomQueue(p.queue.orEmpty().map { it.toTrack() })
        }
    }

    private fun load(track: TrackInfo, queue: List<TrackInfo>) {
        loadingTrackId = track.id
        loadedTrackId = null
        player.roomLoad(track.toTrack(), queue.map { it.toTrack() }) {
            scope.launch(serial) {
                if (loadingTrackId != track.id) return@launch
                loadingTrackId = null
                loadedTrackId = track.id
                send(MessageTypes.BUFFER_READY, BufferReadyPayload(track.id))
            }
        }
    }

    /**
     * Brings this player to [targetMs]. While it plays, a small gap is left alone: the host repeats
     * its position every few seconds, and jumping on each would be heard as stutter.
     */
    private fun catchUp(targetMs: Long, playing: Boolean, heartbeat: Boolean = false) {
        val off = abs(player.livePositionMs - targetMs)
        if (playing) {
            if (!player.livePlaying) {
                if (off > SOFT_SYNC_MS) player.roomSeek(targetMs)
                player.roomResume()
            } else if (off > (if (heartbeat) PLAYING_TOLERANCE_MS else SOFT_SYNC_MS)) {
                player.roomSeek(targetMs)
            }
        } else {
            player.roomPause()
            if (off > SOFT_SYNC_MS) player.roomSeek(targetMs)
        }
    }

    companion object {
        const val DEFAULT_SERVER = "wss://pulse-together.onrender.com/ws"
        const val USER_AGENT = "app.pulse.music"
        private const val WAKING_AFTER_MS = 3_000L
        private const val PING_INTERVAL_MS = 25_000L
        private const val HEARTBEAT_MS = 8_000L
        private const val QUEUE_DEBOUNCE_MS = 500L
        private const val SOFT_SYNC_MS = 150L
        private const val PLAYING_TOLERANCE_MS = 2_000L
        private const val MAX_QUEUE = 200
        private const val MAX_RECONNECTS = 8
        private val RECONNECT_DELAYS_MS = longArrayOf(1_000, 2_000, 4_000, 8_000, 15_000, 30_000)

        /** "wss://host/ws" → "https://host/health", as on the phone. */
        fun healthUrl(serverUrl: String): String? {
            val trimmed = serverUrl.trim()
            val scheme = when {
                trimmed.startsWith("wss://", ignoreCase = true) -> "https://"
                trimmed.startsWith("ws://", ignoreCase = true) -> "http://"
                else -> return null
            }
            val host = trimmed.substringAfter("://").substringBefore("/").takeIf { it.isNotBlank() } ?: return null
            return "$scheme$host/health"
        }

        private val codeInLink = Regex("""[?&](?:code|room)=([A-Za-z0-9]{8})\b""")
        private val codeAfterWord = Regex("""(?:[Кк]од|[Cc]ode)[^A-Z0-9]{0,16}([A-Z0-9]{8})\b""")
        private val bareCode = Regex("""^[A-Za-z0-9]{8}$""")

        /** The room code in a pasted invite (the phone's message), a link or the bare code. */
        fun extractCode(text: String?): String? {
            if (text.isNullOrBlank()) return null
            codeInLink.find(text)?.let { return it.groupValues[1].uppercase() }
            codeAfterWord.find(text)?.let { return it.groupValues[1] }
            return text.trim().takeIf { bareCode.matches(it) }?.uppercase()
        }
    }
}
