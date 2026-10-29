package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.pulse.feed.MyPulse
import com.metrolist.music.pulse.feed.PulseFeed

/**
 * Pulse: "My pulse" in the player — a queue that never ends. The service asks for more when five
 * songs are left, as for a radio, even with "load more" turned off: an endless stream is the point.
 */
class PulseQueue(
    private val pulse: MyPulse,
    val feed: PulseFeed,
    private val title: String,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus(): Queue.Status {
        pulse.playing(feed)
        return Queue.Status(title = title, items = pulse.next(feed).map { it.toMediaItem() }, mediaItemIndex = 0)
    }

    override fun hasNextPage(): Boolean = true

    override suspend fun nextPage(): List<MediaItem> = pulse.next(feed).map { it.toMediaItem() }
}
