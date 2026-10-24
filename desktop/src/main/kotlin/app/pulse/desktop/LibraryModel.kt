package app.pulse.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.utils.completed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pulse desktop: the signed-in account's music — liked songs and saved playlists — read with the
 * same calls the Android app's library sync makes.
 */
class LibraryModel(private val scope: CoroutineScope) {
    data class Playlist(val id: String, val title: String, val subtitle: String?, val thumbnail: String?)

    private val _liked = MutableStateFlow<List<Track>?>(null)
    val liked: StateFlow<List<Track>?> = _liked.asStateFlow()
    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()
    private val _playlists = MutableStateFlow<List<Playlist>?>(null)
    val playlists: StateFlow<List<Playlist>?> = _playlists.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    fun refresh() {
        if (!Account.signedIn) {
            _liked.value = null
            _playlists.value = null
            _likedIds.value = emptySet()
            return
        }
        _failed.value = false
        scope.launch {
            YouTube.playlist("LM").completed()
                .onSuccess { page ->
                    val tracks = page.songs.map(Track::from)
                    _liked.value = tracks
                    _likedIds.value = tracks.map { it.videoId }.toSet()
                }
                .onFailure { _failed.value = true; _liked.value = emptyList() }
        }
        scope.launch {
            YouTube.library("FEmusic_liked_playlists").completed()
                .onSuccess { page ->
                    _playlists.value = page.items.filterIsInstance<PlaylistItem>()
                        .filterNot { it.id == "LM" || it.id == "SE" }
                        .map { Playlist(it.id, it.title, it.songCountText ?: it.author?.name, it.thumbnail) }
                }
                .onFailure { _failed.value = true; _playlists.value = emptyList() }
        }
    }

    suspend fun playlistTracks(id: String): List<Track>? =
        YouTube.playlist(id).completed().getOrNull()?.songs?.map(Track::from)

    /** Likes go to the account at once, as on the phone; the heart flips before the answer comes. */
    fun toggleLike(track: Track) {
        val like = track.videoId !in _likedIds.value
        _likedIds.update { if (like) it + track.videoId else it - track.videoId }
        _liked.update { list -> list?.let { if (like) listOf(track) + it else it.filterNot { t -> t.videoId == track.videoId } } }
        scope.launch {
            YouTube.likeVideo(track.videoId, like).onFailure {
                _likedIds.update { if (like) it - track.videoId else it + track.videoId }
            }
        }
    }
}
