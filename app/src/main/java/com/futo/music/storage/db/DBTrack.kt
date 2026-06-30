package com.futo.music.storage.db

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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
import com.futo.music.R
import com.futo.music.audioContainerToExtension
import com.futo.music.logging.Logger
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.PlayableType
import com.futo.music.models.playable.Track
import com.futo.music.settings.Settings
import com.futo.music.states.DBPlayableType
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.toSafeFileName
import java.io.InputStream
import java.net.URI
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
    val mediaStoreAlbumId: Long = -1,

    @ColumnInfo(defaultValue = "FALSE")
    var hidden: Boolean = false,

    @ColumnInfo(defaultValue = "FALSE")
    var markedRated: Boolean = false
): IPlayable, IPlayableTrack {
    override val type: PlayableType get() = PlayableType.Track;

    override fun getItemId(): String {
        return id.toString();
    }


    override fun getImage(): ImageVariable? {
        try {
            if (!Settings.instance.media.loadTrackArt) return null;
            val mediaDataRetriever = MediaMetadataRetriever();
            val context = StateApp.instance.activity() ?: return null;
            mediaDataRetriever.setDataSource(context, Uri.parse(contentUrl));
            return mediaDataRetriever?.embeddedPicture?.let {
                return@let ImageVariable.fromBitmap(BitmapFactory.decodeByteArray(it, 0, it.size));
            }
        }
        catch(ex: Throwable) {
            Logger.e("DBTrack", "Could not get track art for [${name}]: ${ex.message}", ex);
            return ImageVariable.fromResource(R.drawable.ic_image_broken)
        }
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
    @Query("SELECT * FROM tracks WHERE hidden != 1")
    fun getAll(): List<DBTrack>;
    @Query("SELECT * FROM tracks WHERE hidden == 1")
    fun getAllHidden(): List<DBTrack>;
    @Query("SELECT * FROM tracks WHERE id = :id")
    fun get(id: Long): DBTrack?;
    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    fun getList(ids: List<Long>): List<DBTrack>;
    @Query("SELECT * FROM tracks WHERE mediaStoreId = :id")
    fun getByMSID(id: Long): DBTrack?;
    @Query("SELECT * FROM tracks WHERE fileName = :fileName")
    fun getByFileName(fileName: String): DBTrack?;

    @Query("SELECT * FROM tracks WHERE hidden != 1 AND INSTR(lower(name), lower(:str))")
    fun search(str: String): List<DBTrack>;

    @Query("SELECT t.* FROM tracks t INNER JOIN album_tracks at ON t.id = at.trackId WHERE at.albumId = :albumId AND t.hidden != 1 ORDER BY at.ordering")
    fun getAlbumTracks(albumId: Long): List<DBTrack>
    @Query("SELECT t.id FROM tracks t INNER JOIN album_tracks at ON t.id = at.trackId WHERE at.albumId = :albumId AND t.hidden != 1 ORDER BY at.ordering")
    fun getAlbumTrackIds(albumId: Long): List<Long>
    @Query("SELECT t.* FROM tracks t INNER JOIN artist_tracks at ON t.id = at.trackId WHERE at.artistId = :artistId AND t.hidden != 1")
    fun getArtistTracks(artistId: Long): List<DBTrack>
    @Query("SELECT t.id FROM tracks t INNER JOIN artist_tracks at ON t.id = at.trackId WHERE at.artistId = :artistId AND t.hidden != 1")
    fun getArtistTrackIds(artistId: Long): List<Long>
    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks at ON t.id = at.trackId WHERE at.playlistId = :playlistId ORDER BY at.ordering")
    fun getPlaylistTracks(playlistId: Long): List<DBTrack>
    @Query("SELECT t.id FROM tracks t INNER JOIN playlist_tracks at ON t.id = at.trackId WHERE at.playlistId = :playlistId ORDER BY at.ordering")
    fun getPlaylistTrackIds(playlistId: Long): List<Long>

    @Query("SELECT id, score FROM tracks")
    fun getAllScores(): List<ScoredItem>;
    @Query("SELECT id, score FROM tracks WHERE id IN (SELECT id FROM albums ORDER BY RANDOM() LIMIT :count)")
    fun getRandomScores(count: Int): List<ScoredItem>;


    @Query("SELECT * FROM tracks WHERE hidden != 1 ORDER BY dateAdded DESC LIMIT :count")
    fun getTracksNew(count: Int): List<DBTrack>
    @Query("SELECT * FROM tracks WHERE hidden != 1 AND score <= 0 AND markedRated = :markedRated ORDER BY dateAdded DESC LIMIT :count")
    fun getTracksNewUnrated(count: Int, markedRated: Boolean = false): List<DBTrack>
    @Query("SELECT id FROM tracks WHERE hidden != 1 AND score <= 0 AND markedRated = :markedRated ORDER BY RANDOM() DESC LIMIT :count")
    fun getTracksUnratedIds(count: Int, markedRated: Boolean = false): List<Long>

    @Query("SELECT * FROM tracks WHERE hidden != 1 AND plays > 0 ORDER BY plays DESC LIMIT :count")
    fun getMostPlayed(count: Int): List<DBTrack>

    @Query("SELECT t.* FROM tracks t WHERE hidden != 1 AND t.scoreCalculated > 0 ORDER BY (ABS(RANDOM())/ 9223372036854775808.0) * t.scoreCalculated / 100 DESC LIMIT :count")
    fun getRandomWeightedTracks(count: Int): List<DBTrack>
    @Query("SELECT t.id FROM tracks t WHERE hidden != 1 AND t.scoreCalculated > 0 ORDER BY (ABS(RANDOM())/ 9223372036854775808.0) * t.scoreCalculated / 100 DESC LIMIT :count")
    fun getRandomWeightedTrackIds(count: Int): List<Long>
    @Query("SELECT t.* FROM tracks t WHERE hidden != 1 ORDER BY RANDOM() DESC LIMIT :count")
    fun getRandomShuffledTracks(count: Int): List<DBTrack>

    @Query("SELECT * FROM tracks WHERE hidden != 1 AND dateOpened IS NOT NULL ORDER BY dateOpened DESC LIMIT :count")
    fun getTopByRecentOpened(count: Int): List<DBTrack>;

    @Query("UPDATE tracks SET plays = plays + 1 WHERE id == :id")
    fun incrementTrackPlayed(id: Long);

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
    fun setHidden(update: DBSetHidden): Int

    @Update(entity = DBTrack::class)
    fun setRatingCalculated(update: DBTrackUpdateRatingCalculated): Int

    @Update(entity = DBTrack::class)
    fun setMarkedRated(update: DBSetMarkRated): Int
}

@Entity
class DBTrackUpdatePlayed(
    val id: Long,
    val datePlayed: OffsetDateTime
)
@Entity
class DBSetMarkRated(
    val id: Long,
    val markedRated: Boolean
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

class ScoredItem(
    val id: Long,
    val score: Int
) {
    var type: DBPlayableType? = null;

    fun withType(type: DBPlayableType): ScoredItem{
        this.type = type;
        return this;
    }
}