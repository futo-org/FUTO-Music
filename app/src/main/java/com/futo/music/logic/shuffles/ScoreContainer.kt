package com.futo.music.logic.shuffles

import com.futo.music.states.DBPlayableType
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.ScoredItem

class ScoreContainer(val scores: Map<Int, MutableList<ScoredItem>>, _allTracks: Map<Long, ScoredItem>? = null) {

    val allTracks: Map<Long, ScoredItem>;

    init {
        allTracks = if(_allTracks != null)
                _allTracks
            else {
                val mapping = HashMap<Long, ScoredItem>();
                for(item in scores.flatMap{ it.value.filter { it.type == DBPlayableType.Track } })
                    mapping[item.id] = item;
                mapping;
            }
    }

    fun getTrackById(id: Long): ScoredItem? {
        return allTracks[id];
    }

    fun removeTrack(id: Long) {
        val item = allTracks.getOrDefault(id, null) ?: return;

        val collection = scores[item.score];
        collection?.remove(item);
    }

    fun copy(): ScoreContainer {
        return ScoreContainer(HashMap(scores), allTracks);
    }

    companion object {
        fun retrieve(): ScoreContainer {
            val items = StateDatabase.instance.db.albumDao().getAllScores().map { it.withType(DBPlayableType.Album) } +
                StateDatabase.instance.db.artistDao().getAllScores().map { it.withType(DBPlayableType.Artist) } +
                StateDatabase.instance.db.playlistDao().getAllScores().map { it.withType(DBPlayableType.Playlist) };

            val tracks = StateDatabase.instance.db.tracksDao().getAllScores().map { it.withType(DBPlayableType.Track) };

            val trackMap = HashMap<Long, ScoredItem>();
            for(track in tracks)
                trackMap[track.id] = track;


            val grouped = (items + tracks).groupBy<ScoredItem, Int>{ it.score };

            return ScoreContainer(grouped.mapValues { it.value.toMutableList() }, trackMap);
        }
    }
}