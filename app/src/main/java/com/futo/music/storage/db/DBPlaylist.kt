package com.futo.music.storage.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.PlayableType
import com.futo.music.models.playable.Track
import com.futo.music.states.StateDatabase
import java.time.OffsetDateTime

@Entity(tableName = "playlists")
class DBPlaylist(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    override val name: String,
    override val score: Int = 0,

    var artUri1: String? = null,
    var artUriTrack1: Long? = null,
    var artUri2: String? = null,
    var artUriTrack2: Long? = null,
    var artUri3: String? = null,
    var artUriTrack3: Long? = null,
    var artUri4: String? = null,
    var artUriTrack4: Long? = null,

    val dateAdded: OffsetDateTime = OffsetDateTime.MIN,
    override val datePlayed: OffsetDateTime = OffsetDateTime.MIN,
    val dateModified: OffsetDateTime = OffsetDateTime.MIN,

    val plays: Int = 0,

    var trackCount: Int = -1,
    var trackDurations: Int = -1,
): IPlayable {
    override val type: PlayableType get() = PlayableType.Playlist;

    override fun getImage(): ImageVariable? {
        return ImageVariable.fromUrl(artUri1);
    }

    override fun getTracks(context: Context): List<IPlayableTrack> {
        return StateDatabase.instance.getPlaylistTracks(id);
    }
}

@Entity(tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(DBPlaylist::class, arrayOf("id"), arrayOf("playlistId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
        ForeignKey(DBTrack::class, arrayOf("id"), arrayOf("trackId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ],
    primaryKeys = ["playlistId", "trackId"])
class DBPlaylistTrack(
    val playlistId: Long,
    val trackId: Long,
    val ordering: Int
)

@Dao
interface DBPlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAll(): List<DBPlaylist>;
    @Query("SELECT * FROM playlists ORDER BY datePlayed DESC")
    fun getAllByRecentPlayed(): List<DBPlaylist>;
    @Query("SELECT * FROM playlists ORDER BY datePlayed DESC LIMIT :count")
    fun getTopByRecentPlayed(count: Int): List<DBPlaylist>;

    @Query("SELECT MAX(ordering) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistMaxOrder(playlistId: Long): Int;

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun get(id: Long): DBPlaylist?;

    @Query("SELECT COUNT(*) FROM playlists")
    fun count(): Int;


    @Query("SELECT a.* FROM playlists a INNER JOIN playlist_tracks at ON a.id = at.playlistId WHERE at.trackId = :trackId")
    fun getTrackPlaylists(trackId: Long): List<DBPlaylist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg albums: DBPlaylist): Array<Long>;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg atrack: DBPlaylistTrack): Array<Long>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    fun deletePlaylistTrack(playlistId: Long, trackId: Long);

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    fun getPlaylistTrackRef(playlistId: Long, trackId: Long): DBPlaylistTrack?;

    @Update(entity = DBPlaylist::class)
    fun setPlayed(update: DBPlaylistUpdatePlayed)

    @Update(entity = DBPlaylist::class)
    fun setRating(update: DBPlaylistUpdateRating): Int

    @Update(entity = DBPlaylist::class)
    fun setTrackMetadata(update: DBPlaylistUpdateTrackMetadata)

    @Update(entity = DBPlaylistTrack::class)
    fun setPlaylistOrder(track: DBPlaylistTrack);
}
@Entity
class DBPlaylistUpdatePlayed(
    val id: Long,
    val datePlayed: OffsetDateTime
)
@Entity
class DBPlaylistUpdateRating(
    val id: Long,
    val score: Int
)
@Entity
class DBPlaylistUpdateTrackMetadata(
    val id: Long,
    val trackCount: Int,
    val trackDurations: Int,
    val artUri1: String?,
    val artUriTrack1: Long?,
    val artUri2: String?,
    val artUriTrack2: Long?,
    val artUri3: String?,
    val artUriTrack3: Long?,
    val artUri4: String?,
    val artUriTrack4: Long?
)