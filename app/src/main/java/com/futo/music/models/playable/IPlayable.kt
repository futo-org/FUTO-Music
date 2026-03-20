package com.futo.music.models.playable

import android.content.Context
import androidx.media3.common.MediaItem
import com.futo.music.models.ImageVariable
import java.time.OffsetDateTime

interface IPlayable {
    val type: PlayableType;

    val name: String;

    val score: Int?;

    val datePlayed: OffsetDateTime?

    fun getImage(): ImageVariable?;

    fun getTracks(context: Context): List<IPlayableTrack>;
}

interface IPlayableTrack {

    fun getMediaItem(): MediaItem;
}