package com.futo.music.models.playable

import android.content.Context
import com.futo.music.models.ImageVariable
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBTrack
import java.time.OffsetDateTime


class Vibe: IPlayable {
    override val type = PlayableType.Vibe;

    override val name: String;
    val art: ImageVariable;
    val albums: List<DBAlbum>;
    val artist: List<DBArtist>;
    val singles: List<DBTrack>;

    val vibeType: QueueType;

    var recomReasons: List<String?>?;


    override var score: Int = 0;

    override val datePlayed: OffsetDateTime? = null;

    constructor(name: String, art: ImageVariable, albums: List<DBAlbum>, artists: List<DBArtist>, tracks: List<DBTrack>, type: QueueType = QueueType.Unknown, reasons: List<String?>? = null) {
        this.name = name;
        this.art = art;
        this.albums = albums;
        this.artist = artists;
        this.singles = tracks;
        this.vibeType = type;
        this.recomReasons = reasons;
    }


    override fun getImage(): ImageVariable? {
        return null;
    }

    override fun getTracks(context: Context): List<IPlayableTrack> {
        val tracks = albums.flatMap { it.getTracks(context) } +
                artist.flatMap { it.getTracks(context) } +
                singles.map { it };
        return tracks;
    }
}

enum class QueueType {
    Unknown,
    SmartShuffle,
    Shuffle
}