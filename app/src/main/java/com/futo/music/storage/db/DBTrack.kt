package com.futo.music.storage.db

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.futo.music.audioContainerToExtension
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.PlayableType
import com.futo.music.models.playable.Track
import com.futo.music.states.StateDatabase
import com.futo.music.toSafeFileName
import java.io.InputStream
import java.time.OffsetDateTime

@Entity(tableName = "tracks")
class DBTrack(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,

    override val name: String,
    val author: String,

    val artistId: Long? = null,
    val artistLine: String? = null,
    val albumLine: String? = null,

    val contentUrl: String? = null,

    override val datePlayed: OffsetDateTime = OffsetDateTime.MIN,
    @ColumnInfo(defaultValue = "NULL")
    val dateOpened: OffsetDateTime? = null,
    val dateAdded: OffsetDateTime = OffsetDateTime.MIN,
    override var score: Int = 0,
    val scoreLevel: Int = 0,
    val scoreCalculated: Int = 0,

    val skips: Int = 0,
    val plays: Int = 0,

    val duration: Int = 0,

    val mimeType: String? = null,
    val fileName: String? = null,

    val metadataType: MetadataType = MetadataType.UNKNOWN,

    val mediaStoreId: Long = -1,
    val mediaStoreArtistId: Long = -1,
    val mediaStoreAlbumId: Long = -1
): IPlayable, IPlayableTrack {
    override val type: PlayableType get() = PlayableType.Track;

    override fun getItemId(): String {
        return id.toString();
    }


    override fun getImage(): ImageVariable? {
        return null;
    }

    override fun getTracks(context: Context): List<IPlayableTrack> {
        return listOf(this);
    }

    fun refetch(context: Context): DBTrack {
        return StateDatabase.instance.getTrack(id) ?: throw IllegalStateException("Track was deleted or never existed");
    }

    fun getStream(context: Context): InputStream? {
        return context.contentResolver.openInputStream(Uri.parse(contentUrl));
    }

    override fun getMediaItem(): MediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setMediaId(id.toString())
            .setTag(id.toString())
            .setUri(contentUrl);


        val artists = StateDatabase.instance.getTrackArtists(id);
        val albums = StateDatabase.instance.getTrackAlbums(id);

        //#region Metadata
        val metadataBuilder = MediaMetadata.Builder();
        metadataBuilder.setExtras(Bundle().apply {
            putLong("id", id);
        });
        if(artistLine != null) {
            metadataBuilder.setTitle(name);
            metadataBuilder.setArtist(artistLine);
        }
        else
            metadataBuilder.setTitle(name);
        val art = albums.find { it.artUri != null }?.artUri ?:
            artists.find { it.artUri != null }?.artUri;

        if(art != null) {
            metadataBuilder.setArtworkUri(Uri.parse(art));
        }

        val metadata = metadataBuilder.build();
        //#endregion

        val mediaItem = mediaItemBuilder
            .setMediaMetadata(metadata)
            .build();

        return mediaItem;
    }

    fun getShareFileName(): String {
        return (((fileName ?: (if(!artistLine.isNullOrBlank()) artistLine + " - " + name else name).toSafeFileName() + "." + audioContainerToExtension(mimeType))));
    }

    fun filter(query: String): Boolean {
        return name.lowercase().contains(query) || (artistLine != null && artistLine.contains(query))
    }
}

@Dao
interface DBTrackDao {
    @Query("SELECT * FROM tracks")
    fun getAll(): List<DBTrack>;
    @Query("SELECT * FROM tracks WHERE id = :id")
    fun get(id: Long): DBTrack?;
    @Query("SELECT * FROM tracks WHERE mediaStoreId = :id")
    fun getByMSID(id: Long): DBTrack?;

    @Query("SELECT * FROM tracks WHERE INSTR(lower(name), lower(:str))")
    fun search(str: String): List<DBTrack>;

    @Query("SELECT t.* FROM tracks t INNER JOIN album_tracks at ON t.id = at.trackId WHERE at.albumId = :albumId ORDER BY at.ordering")
    fun getAlbumTracks(albumId: Long): List<DBTrack>
    @Query("SELECT t.* FROM tracks t INNER JOIN artist_tracks at ON t.id = at.trackId WHERE at.artistId = :artistId")
    fun getArtistTracks(artistId: Long): List<DBTrack>
    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks at ON t.id = at.trackId WHERE at.playlistId = :playlistId ORDER BY at.ordering")
    fun getPlaylistTracks(playlistId: Long): List<DBTrack>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC LIMIT :count")
    fun getTracksNew(count: Int): List<DBTrack>


    @Query("SELECT t.* FROM tracks t WHERE t.scoreCalculated > 0 ORDER BY (ABS(RANDOM())/ 9223372036854775808.0) * t.scoreCalculated / 100 DESC LIMIT :count")
    fun getRandomWeightedTracks(count: Int): List<DBTrack>
    @Query("SELECT t.* FROM tracks t  ORDER BY RANDOM() DESC LIMIT :count")
    fun getRandomShuffledTracks(count: Int): List<DBTrack>

    @Query("SELECT * FROM tracks WHERE dateOpened IS NOT NULL ORDER BY dateOpened DESC LIMIT :count")
    fun getTopByRecentOpened(count: Int): List<DBTrack>;

    @Query("SELECT COUNT(*) FROM tracks")
    fun count(): Int;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg tracks: DBTrack): Array<Long>;

    @Update(entity = DBTrack::class)
    fun update(vararg tracks: DBTrack): Int

    @Update(entity = DBTrack::class)
    fun setPlayed(update: DBTrackUpdatePlayed)
    @Update(entity = DBTrack::class)
    fun setOpened(update: DBTrackUpdateOpened)

    @Update(entity = DBTrack::class)
    fun setRating(update: DBTrackUpdateRating): Int

    @Update(entity = DBTrack::class)
    fun setRatingCalculated(update: DBTrackUpdateRatingCalculated): Int
}

@Entity
class DBTrackUpdatePlayed(
    val id: Long,
    val datePlayed: OffsetDateTime
)
@Entity
class DBTrackUpdateOpened(
    val id: Long,
    val datePlayed: OffsetDateTime,
    val dateOpened: OffsetDateTime?
)

@Entity
class DBTrackUpdateRating(
    val id: Long,
    val score: Int
)


@Entity
class DBTrackUpdateRatingCalculated(
    val id: Long,
    val scoreLevel: Int,
    val scoreCalculated: Int
)