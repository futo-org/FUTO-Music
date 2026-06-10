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

@Entity(tableName = "artists")
class DBArtist(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    override val name: String,
    override var score: Int = 0,

    val artUri: String? = null,

    val dateAdded: OffsetDateTime = OffsetDateTime.MIN,
    override val datePlayed: OffsetDateTime = OffsetDateTime.MIN,

    val plays: Int = 0,

    val trackCount: Int = 0,
    val trackDurations: Int = 0,

    val metadataType: MetadataType = MetadataType.UNKNOWN,

    val mediaStoreId: Long = -1,

    @ColumnInfo(defaultValue = "FALSE")
    val hidden: Boolean = false
): IPlayable {
    override val type: PlayableType get() = PlayableType.Artist;

    override fun getImage(): ImageVariable? {
        return ImageVariable.fromUrl(artUri);
    }

    override fun getTracks(context: Context): List<IPlayableTrack> {
        return StateDatabase.instance.getArtistTracks(id);
    }
}


@Entity(tableName = "artist_tracks",
    foreignKeys = [
        ForeignKey(DBArtist::class, arrayOf("id"), arrayOf("artistId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
        ForeignKey(DBTrack::class, arrayOf("id"), arrayOf("trackId"), onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ],
    primaryKeys = ["artistId", "trackId"])
class DBArtistTrack(
    val artistId: Long,
    val trackId: Long
)

@Dao
interface DBArtistDao {
    @Query("SELECT * FROM artists WHERE hidden != 1")
    fun getAll(): List<DBArtist>;
    @Query("SELECT * FROM artists WHERE hidden == 1")
    fun getAllHidden(): List<DBArtist>;
    @Query("SELECT * FROM artists WHERE hidden != 1 ORDER BY datePlayed DESC, trackCount DESC")
    fun getAllByRecentPlayed(): List<DBArtist>;
    @Query("SELECT * FROM artists WHERE hidden != 1 ORDER BY datePlayed DESC, trackCount DESC LIMIT :count")
    fun getTopByRecentPlayed(count: Int): List<DBArtist>;
    @Query("SELECT * FROM artists WHERE id = :id")
    fun get(id: Long): DBArtist?;
    @Query("SELECT * FROM artists WHERE mediaStoreId = :id")
    fun getByMSID(id: Long): DBArtist?;


    @Query("SELECT * FROM artists WHERE hidden != 1 AND INSTR(lower(name), lower(:str))")
    fun search(str: String): List<DBArtist>;


    @Query("SELECT COUNT(*) FROM artists")
    fun count(): Int;

    @Query("SELECT a.* FROM artists a INNER JOIN artist_tracks at ON a.id = at.artistId WHERE at.trackId = :trackId")
    fun getTrackArtists(trackId: Long): List<DBArtist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg tracks: DBArtist): Array<Long>;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg atrack: DBArtistTrack);


    @Update(entity = DBArtist::class)
    fun setPlayed(update: DBArtistUpdatePlayed)

    @Update(entity = DBArtist::class)
    fun setRating(update: DBArtistUpdateRating): Int

    @Update(entity = DBArtist::class)
    fun setHidden(update: DBSetHidden): Int

    @Update(entity = DBArtist::class)
    fun setTrackMetadata(update: DBArtistUpdateTrackMetadata)
}
@Entity
class DBArtistUpdatePlayed(
    val id: Long,
    val datePlayed: OffsetDateTime
)

@Entity
class DBArtistUpdateRating(
    val id: Long,
    val score: Int
)

@Entity
class DBArtistUpdateTrackMetadata(
    val id: Long,
    val trackCount: Int,
    val trackDurations: Int,
    val artUri: String?
)