package com.futo.music

import com.futo.music.fragments.main.ArtistFragment
import com.futo.music.fragments.main.MainFragment
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset


fun Long?.sToOffsetDateTimeUTC(): OffsetDateTime {
    if (this == null || this < 0)
        return OffsetDateTime.MIN
    if(this > 4070912400)
        return OffsetDateTime.MAX;
    return OffsetDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)
}

fun Long?.msToOffsetDateTimeUTC(): OffsetDateTime {
    if (this == null || this < 0)
        return OffsetDateTime.MIN
    if(this > 4070912400)
        return OffsetDateTime.MAX;
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
}

fun String.levenshtein(b: String): Int {
    return levenshteinDistance(this, b)
};
fun levenshteinDistance(a: String, b: String): Int {
    if(a == b)
        return 0;
    if(a.length == 0)
        return b.length;
    if(b.length == 0)
        return a.length;

    val diffMat = Array(a.length + 1) { IntArray(b.length + 1) }

    for (i in 0..a.length)
        diffMat[i][0] = i
    for (j in 0..b.length)
        diffMat[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val diff = if (a[i - 1] == b[j - 1]) 0 else 1
            diffMat[i][j] = minOf(diffMat[i - 1][j] + 1, diffMat[i][j - 1] + 1, diffMat[i - 1][j - 1] + diff)
        }
    }
    return diffMat[a.length][b.length]
}

class IPlayableWithPlaySettings(
    val playable: IPlayable,
    val playSettings: PlaySettings);

class PlaySettings(
    val shuffle: Boolean = false
)
fun IPlayable.withSettings(settings: PlaySettings): IPlayableWithPlaySettings {
    return IPlayableWithPlaySettings(this, settings);
}


fun IPlayable.openPlayable(fragment: MainFragment, preferMenu: Boolean = false) {
    if(this is DBArtist && !preferMenu)
        fragment.navigate<ArtistFragment>(this);
    else {
        if (preferMenu || StateQueue.instance.getCurrentTrack() != null) {
            UIDialogs.overlayPlayable(this);
        } else {
            fragment.navigate<PlaybackFragment>(this);
        }
    }
}