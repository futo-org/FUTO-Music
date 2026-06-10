package com.futo.music.storage.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
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

@Entity(tableName = "albums")
class DBAlbum(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    override val name: String,
    val authors: String,
    override var score: Int = 0,

    val artUri: String? = null,

    val dateAdded: OffsetDateTime = OffsetDateTime.MIN,
    override val datePlayed: OffsetDateTime = OffsetDateTime.MIN,

    val plays: Int = 0,
    val trackCount: Int = -1,
    val trackDurations: Int = -1,

    val metadataType: MetadataType = MetadataType.UNKNOWN,

    val mediaStoreId: Long = -1,
    val mediaStoreArtistId: Long = -1,

    @ColumnInfo(defaultValue = "FALSE")
    val hidden: Boolean = false
): IPlayable {
    override val type: PlayableType get() = PlayableType.Album;

    override fun getImage(): ImageVariable? {
        return ImageVariable.fromUrl(artUri);
    }

    override fun getTracks(context: Context): List<IPlayableTrack> {
        return StateDatabase.instance.getAlbumTracks(id);
    }
}

@Entity(tableName = "album_tracks",
    foreignKeys = [
        ForeignKey(DBAlbum::class, arrayOf("id"), arrayOf("albumId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
        ForeignKey(DBTrack::class, arrayOf("id"), arrayOf("trackId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ],
    primaryKeys = ["albumId", "trackId"])
class DBAlbumTrack(
    val albumId: Long,
    val trackId: Long,
    val ordering: Int
)

@Entity(tableName = "album_artists",
    foreignKeys = [
        ForeignKey(DBAlbum::class, arrayOf("id"), arrayOf("albumId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
        ForeignKey(DBArtist::class, arrayOf("id"), arrayOf("artistId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ],
    primaryKeys = ["albumId", "artistId"])
class DBAlbumArtist(
    val albumId: Long,
    val artistId: Long
)

@Dao
interface DBAlbumDao {
    @Query("SELECT * FROM albums WHERE hidden != 1")
    fun getAll(): List<DBAlbum>;
    @Query("SELECT * FROM albums WHERE hidden == 1")
    fun getAllHidden(): List<DBAlbum>;
    @Query("SELECT * FROM albums WHERE hidden != 1 ORDER BY datePlayed DESC, trackCount DESC")
    fun getAllByRecentPlayed(): List<DBAlbum>;
    @Query("SELECT * FROM albums WHERE hidden != 1 ORDER BY datePlayed DESC, trackCount DESC LIMIT :count")
    fun getTopByRecentPlayed(count: Int): List<DBAlbum>;

    @Query("SELECT * FROM albums WHERE id = :id")
    fun get(id: Long): DBAlbum?;
    @Query("SELECT * FROM albums WHERE mediaStoreId = :id")
    fun getByMSID(id: Long): DBAlbum?;
    @Query("SELECT * FROM albums WHERE mediaStoreArtistId = :id")
    fun getByArtistMSID(id: Long): List<DBAlbum>;



    @Query("SELECT * FROM albums WHERE INSTR(lower(name), lower(:str))")
    fun search(str: String): List<DBAlbum>;

    @Query("SELECT COUNT(*) FROM albums")
    fun count(): Int;


    @Query("SELECT a.* FROM albums a INNER JOIN album_tracks at ON a.id = at.albumId WHERE at.trackId = :trackId")
    fun getTrackAlbums(trackId: Long): List<DBAlbum>
    @Query("SELECT a.artUri FROM albums a INNER JOIN album_tracks at ON a.id = at.albumId WHERE at.trackId = :trackId AND a.artUri IS NOT NULL AND a.artUri != ''")
    fun getTrackAlbumArts(trackId: Long): List<String>

    @Query("SELECT a.artUri FROM albums a INNER JOIN album_artists at ON a.id = at.albumId WHERE at.artistId = :artistId AND a.artUri IS NOT NULL AND a.artUri != ''")
    fun getAlbumArtByArtist(artistId: Long): String?

    @Query("SELECT * FROM albums T INNER JOIN album_artists A ON A.albumId = T.id WHERE A.artistId = :artistId")
    fun getArtistAlbums(artistId: Long): List<DBAlbum>;

    @Query("SELECT * FROM artists T INNER JOIN album_artists A ON A.artistId = T.id WHERE A.albumId = :albumId")
    fun getAlbumArtists(albumId: Long): List<DBArtist>;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg albums: DBAlbum): Array<Long>;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg atrack: DBAlbumTrack);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg aartist: DBAlbumArtist);

    @Query("SELECT * FROM album_tracks WHERE albumId = :albumId AND trackId = :trackId")
    fun getAlbumTrackRef(albumId: Long, trackId: Long): DBAlbumTrack?;

    @Query("SELECT * FROM album_artists WHERE albumId = :albumId AND artistId = :artistId")
    fun getAlbumArtistRef(albumId: Long, artistId: Long): DBAlbumArtist?;

    @Update(entity = DBAlbum::class)
    fun setPlayed(update: DBAlbumUpdatePlayed)

    @Update(entity = DBAlbum::class)
    fun setRating(update: DBAlbumUpdateRating): Int

    @Update(entity = DBAlbum::class)
    fun setHidden(update: DBSetHidden): Int

    @Update(entity = DBAlbum::class)
    fun setTrackMetadata(update: DBAlbumUpdateTrackMetadata)
}

@Entity
class DBSetHidden (
    val id: Long,
    val hidden: Boolean
);

@Entity
class DBAlbumUpdatePlayed(
    val id: Long,
    val datePlayed: OffsetDateTime
)
@Entity
class DBAlbumUpdateRating(
    val id: Long,
    val score: Int
)

@Entity
class DBAlbumUpdateTrackMetadata(
    val id: Long,
    val trackCount: Int,
    val trackDurations: Int
)