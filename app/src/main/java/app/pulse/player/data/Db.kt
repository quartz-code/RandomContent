package app.pulse.player.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

object TrackStatus {
    const val PENDING = "PENDING"
    const val DOWNLOADED = "DOWNLOADED"
    const val FAILED = "FAILED"
    /** Removed by the user on the device: sync must not download it again. */
    const val REMOVED = "REMOVED"
}

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSec: Int = 0,
    val filePath: String? = null,
    val coverPath: String? = null,
    val status: String = TrackStatus.PENDING,
    /** Position in the YouTube Music "Liked" list, 0 = most recently liked. */
    val likedIndex: Int = 0,
    val liked: Boolean = true,
    val error: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrack(
    val playlistId: Long,
    val trackId: String,
    val position: Int,
)

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    @ColumnInfo(name = "trackCount") val trackCount: Int,
    @ColumnInfo(name = "coverPath") val coverPath: String?,
)

data class StatusCount(val status: String, val count: Int)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE status = 'DOWNLOADED' ORDER BY likedIndex ASC, addedAt DESC")
    fun downloaded(): Flow<List<Track>>

    @Query("SELECT * FROM tracks")
    suspend fun all(): List<Track>

    @Query("SELECT * FROM tracks WHERE status IN ('PENDING', 'FAILED') AND liked = 1 ORDER BY likedIndex ASC")
    suspend fun toDownload(): List<Track>

    @Query("SELECT * FROM tracks WHERE status = 'FAILED' ORDER BY likedIndex ASC")
    fun failed(): Flow<List<Track>>

    @Query("SELECT status, COUNT(*) AS count FROM tracks WHERE liked = 1 OR status = 'DOWNLOADED' GROUP BY status")
    fun statusCounts(): Flow<List<StatusCount>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun get(id: String): Track?

    @Upsert
    suspend fun upsert(tracks: List<Track>)

    @Query("UPDATE tracks SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: String, status: String, error: String? = null)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT p.id, p.name, p.createdAt, COUNT(t.id) AS trackCount,
            (SELECT t2.coverPath FROM playlist_tracks pt2 JOIN tracks t2 ON t2.id = pt2.trackId
             WHERE pt2.playlistId = p.id AND t2.status = 'DOWNLOADED' ORDER BY pt2.position LIMIT 1) AS coverPath
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id
        LEFT JOIN tracks t ON t.id = pt.trackId AND t.status = 'DOWNLOADED'
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    fun playlists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun playlist(id: Long): Flow<Playlist?>

    @Query(
        """
        SELECT t.* FROM tracks t JOIN playlist_tracks pt ON pt.trackId = t.id
        WHERE pt.playlistId = :id AND t.status = 'DOWNLOADED'
        ORDER BY pt.position ASC
        """
    )
    fun tracks(id: Long): Flow<List<Track>>

    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun clear(id: Long)

    @Query("SELECT COALESCE(MAX(position), 0) FROM playlist_tracks WHERE playlistId = :id")
    suspend fun maxPosition(id: Long): Int

    @Upsert
    suspend fun upsertEntry(entry: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeEntry(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun removeTrackEverywhere(trackId: String)

    suspend fun add(playlistId: Long, trackId: String) {
        upsertEntry(PlaylistTrack(playlistId, trackId, maxPosition(playlistId) + 1))
    }

    suspend fun delete(id: Long) {
        clear(id)
        deletePlaylist(id)
    }
}

@Database(entities = [Track::class, Playlist::class, PlaylistTrack::class], version = 1, exportSchema = false)
abstract class PulseDb : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun playlists(): PlaylistDao

    companion object {
        @Volatile private var instance: PulseDb? = null

        fun get(context: Context): PulseDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PulseDb::class.java, "pulse.db")
                .build().also { instance = it }
        }
    }
}
