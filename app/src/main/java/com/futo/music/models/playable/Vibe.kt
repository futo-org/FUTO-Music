package com.futo.music.models.playable

import android.content.Context
import com.futo.music.models.ImageVariable
import java.time.OffsetDateTime

//TODO: Delete these?
class Vibe: IPlayable {
    override val type = PlayableType.Vibe;

    override val name: String;
    val art: ImageVariable;
    val albums: List<Album>;
    val artist: List<Artist>;
    val singles: List<Track>;

    override val datePlayed: OffsetDateTime? = null;

    constructor(name: String, art: ImageVariable, albums: List<Album>, artists: List<Artist>, tracks: List<Track>) {
        this.name = name;
        this.art = art;
        this.albums = albums;
        this.artist = artists;
        this.singles = tracks;
    }


    override fun getImage(): ImageVariable? {
        return null;
    }

    override fun getTracks(context: Context): List<Track> {
        val tracks = albums.flatMap { it.getTracks(context) } +
                artist.flatMap { it.getTracks(context) } +
                singles.map { it };
        return tracks;
    }
}