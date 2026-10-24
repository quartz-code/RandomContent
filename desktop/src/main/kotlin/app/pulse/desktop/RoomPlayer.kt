package app.pulse.desktop

import com.metrolist.music.listentogether.TrackInfo
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What Listen Together needs from the player. [PlayerController] is the real one; tests drive a
 * room with a fake, since the real one needs YouTube and a sound card.
 */
interface RoomPlayer {
    val state: StateFlow<PlayerState>

    /** Positions the user moved to by hand, so the host can tell the room. */
    val seeks: SharedFlow<Long>

    /** Where the sound is right now, fresher than [state], which is refreshed a few times a second. */
    val livePositionMs: Long
    val livePlaying: Boolean

    /** The songs after the current one, in the order they will play (shuffle included). */
    fun upcoming(): List<Track>

    /** Host: a suggestion the host approved goes right after the current song. */
    fun playNext(track: Track)

    /** Guest: lock the controls to the room's host ([hostName]), or give them back with null. */
    fun follow(hostName: String?)

    /** Guest: open the room's song paused at the start; [onReady] once it can play at once. */
    fun roomLoad(track: Track, upcoming: List<Track>, onReady: () -> Unit)
    fun roomQueue(upcoming: List<Track>)
    fun roomSeek(positionMs: Long)
    fun roomResume()
    fun roomPause()
}

/** The phone's track card in a room, and back. The server wants a duration, so 3 minutes when unknown. */
internal fun Track.toTrackInfo(): TrackInfo = TrackInfo(
    id = videoId,
    title = title,
    artist = artists,
    duration = durationSec?.takeIf { it > 0 }?.let { it * 1000L } ?: 180_000L,
    thumbnail = thumbnail,
    suggestedBy = suggestedBy,
)

internal fun TrackInfo.toTrack(): Track = Track(
    videoId = id,
    title = title,
    artists = artist,
    thumbnail = thumbnail.takeIf { it.isNotEmpty() },
    durationSec = (duration / 1000).toInt().takeIf { it > 0 },
    suggestedBy = suggestedBy.takeIf { it.isNotEmpty() },
)
