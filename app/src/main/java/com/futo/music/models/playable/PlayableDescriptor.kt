package com.futo.music.models.playable

import android.content.Context
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBTrack
import kotlinx.serialization.Serializable

@Serializable
class PlayableDescriptor(
    val type: PlayableType,
    val tracks: List<Long> = listOf(),
    val id: Long = -1
) {
    fun restore(context: Context): Instance {

        val type = this.type;
        val item = if(id > 0) {
            when(type) {
                PlayableType.Album -> StateDatabase.instance.getAlbum(id)
                PlayableType.Artist -> StateDatabase.instance.getArtist(id)
                PlayableType.Playlist -> StateDatabase.instance.getPlaylist(id)
                else -> null
            }
        } else null;

        val tracks = tracks.map { StateDatabase.instance.getTrack(it) }.filterNotNull() //TODO: Group query?
        return Instance(
            type,
            tracks,
            item
        );
    }


    class Instance(
        val type: PlayableType,
        val tracks: List<DBTrack>,
        val item: IPlayable?
    )
}