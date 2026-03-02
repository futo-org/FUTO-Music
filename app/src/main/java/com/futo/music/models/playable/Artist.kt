package com.futo.music.models.playable

import android.content.Context
import com.futo.music.models.ImageVariable
import com.futo.music.states.StateLibrary
import java.time.OffsetDateTime

//TODO: Delete these?
class Artist: IPlayable {
    override val type = PlayableType.Artist;

    val id: String?;
    override val name: String;
    val art: ImageVariable?;

    override val datePlayed: OffsetDateTime? = null;

    constructor(name: String, art: ImageVariable?, id: String? = null) {
        this.id = id;
        this.name = name;
        this.art = art;
    }

    override fun getImage(): ImageVariable? {
        return art;
    }

    override fun getTracks(context: Context): List<Track> {
        val idLong = id?.toLongOrNull() ?: return listOf();
        return StateLibrary.instance.getArtistTracks(context, idLong);
    }
}