package com.futo.music.models.playable

import android.content.Context
import com.futo.music.models.ImageVariable
import com.futo.music.states.StateLibrary
import java.time.OffsetDateTime

//TODO: Delete these?
class Album: IPlayable {
    override val type = PlayableType.Album;

    override val name: String;

    val id: String?;
    val art: ImageVariable?;
    val singles: List<Track>?;

    val artist: Artist?;

    override val datePlayed: OffsetDateTime? = null;

    constructor(name: String, art: ImageVariable?, tracks: List<Track>?, artist: Artist?, id: String? = null) {
        this.id = id;
        this.name = name;
        this.art = art;
        this.singles = tracks;
        this.artist = artist;
    }

    override fun getImage(): ImageVariable? {
        return art;
    }

    override fun getTracks(context: Context): List<Track> {
        val idLong = id?.toLongOrNull() ?: return listOf();
        return StateLibrary.instance.getAlbumTracks(context, idLong);
    }
}