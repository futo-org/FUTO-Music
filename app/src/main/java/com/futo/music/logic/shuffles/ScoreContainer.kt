package com.futo.music.logic.shuffles

import com.futo.music.states.DBPlayableType
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.ScoredItem

class ScoreContainer(val scores: MutableMap<Int, MutableList<ScoredItem>>, _allTracks: Map<Long, ScoredItem>? = null) {

    val allTracks: Map<Long, ScoredItem>;
    val trackScoreAlias = mutableMapOf<Long, Int>();

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

    fun getTrackScoreAlias(id: Long): Int {
        return trackScoreAlias.getOrDefault(id, -1);
    }
    fun getTrackById(id: Long): ScoredItem? {
        return allTracks[id];
    }

    fun removeTrack(id: Long) {
        val item = allTracks.getOrDefault(id, null) ?: return;
        val scoreId = if(trackScoreAlias.containsKey(item.id)) trackScoreAlias[item.id] else item.score;

        val collection = scores[scoreId];
        collection?.remove(item);
    }
    fun rescoreTrack(id: Long, newScore: Int) {
        val item = allTracks.getOrDefault(id, null) ?: return;
        val scoreId = if(trackScoreAlias.containsKey(item.id)) trackScoreAlias[item.id] else item.score;

        if(item.score != newScore) {
            trackScoreAlias[id] = newScore;

            val collection = scores[scoreId];
            collection?.remove(item);

            val newCollection = scores[newScore];
            if(scores.containsKey(newScore))
                newCollection?.add(item);
        }
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

            return ScoreContainer(grouped.mapValues { it.value.toMutableList() }.toMutableMap(), trackMap);
        }
    }
}