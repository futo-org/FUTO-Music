package com.futo.music.models.playable

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.futo.music.models.ImageVariable
import java.time.OffsetDateTime
import java.util.UUID

//TODO: Delete these?
class Track: IPlayable, IPlayableTrack {
    override val type = PlayableType.Track;

    val id: String;

    override val name: String;
    val uri: Uri;

    var artist: Artist? = null;
    var album: Album? = null;

    var duration: Int = 0;

    var albumOrder: Int = 999;

    override var score: Int = 0;

    override val datePlayed: OffsetDateTime? = null;

    constructor(name: String, uri: Uri, id: String? = null) {
        this.name = name;
        this.uri = uri;
        this.id = id ?: UUID.randomUUID().toString();
    }

    override fun getItemId(): String {
        return id;
    }

    fun withAlbum(album: Album?): Track  {
        if(album == null)
            return this;
        this.album = album;
        return this;
    }
    fun withArtist(artist: Artist?): Track {
        if(artist == null)
            return this;
        this.artist = artist;
        return this;
    }

    fun withAlbumOrder(order: Int): Track {
        albumOrder = order;
        return this;
    }

    fun withDuration(duration: Int): Track {
        this.duration = duration;
        return this;
    }

    override fun getImage(): ImageVariable? {
        return null;
    }

    override fun getMediaItem(): MediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri);

        //#region Metadata
        val metadataBuilder = MediaMetadata.Builder();
        if(artist != null) {
            metadataBuilder.setTitle(name);
            metadataBuilder.setArtist(artist!!.name);
        }
        else
            metadataBuilder.setTitle(name);
        val art = album?.art ?: artist?.art;

        if(art != null && art.url != null) {
            metadataBuilder.setArtworkUri(Uri.parse(art.url));
        }

        val metadata = metadataBuilder.build();
        //#endregion

        val mediaItem = mediaItemBuilder
            .setMediaMetadata(metadata)
            .build();

        return mediaItem;
    }

    override fun getTracks(context: Context): List<Track> {
        return listOf(this);
    }
}