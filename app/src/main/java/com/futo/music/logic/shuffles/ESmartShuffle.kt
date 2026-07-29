package com.futo.music.logic.shuffles

import com.futo.music.states.DBPlayableType
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBPlaylist

class ESmartShuffle(scores: ScoreContainer): SmartShuffle(scores) {
    val chanceStar1 = 4;
    val chanceStar2 = 8;
    val chanceStar3 = 13;
    val chanceStar4 = 25;
    val chanceStar5 = 50;



    override fun getTrackIds(count: Int): List<RecommendationTrack> {
        val tracks = ArrayList<RecommendationTrack>();
        val scores = scoresRoot.copy();

        val albumCache = HashMap<Long, MutableList<Long>>();
        val artistCache = HashMap<Long, MutableList<Long>>();
        val playlistCache = HashMap<Long, MutableList<Long>>();
        val playlistDetailsCache = HashMap<Long, DBPlaylist?>();
        val albumDetailsCache = HashMap<Long, DBAlbum?>();

        while(tracks.size < count && !scores.scores.all { it.value.isEmpty() }){
            val hasStar1 = scores.scores[starToScore(1)]?.any() ?: false;
            val hasStar2 = scores.scores[starToScore(2)]?.any() ?: false;
            val hasStar3 = scores.scores[starToScore(3)]?.any() ?: false;
            val hasStar4 = scores.scores[starToScore(4)]?.any() ?: false;
            val hasStar5 = scores.scores[starToScore(5)]?.any() ?: false;

            val chance1 = if(hasStar1) chanceStar1 else 0;
            val chance2 = if(hasStar2) chanceStar2 else 0;
            val chance3 = if(hasStar3) chanceStar3 else 0;
            val chance4 = if(hasStar4) chanceStar4 else 0;
            val chance5 = if(hasStar5) chanceStar5 else 0;

            if(chance1 + chance2 + chance3 + chance4 + chance5 <= 0)
                break;
            val targetScore = selectScore(chance1, chance2, chance3, chance4, chance5); //Selects a given star assignment with the chances described above.
            val options = scores.scores[targetScore]; //All tracks/albums/artists/playlists with a given score/star rating.

            val optionIndex = random.nextInt(options!!.size); //Randomly selected playable option (track/album/artist/playlist)

            val option = options[optionIndex];

            val selectedTrackRecoms = when(option.type) {
                DBPlayableType.Track -> {
                    listOf(RecommendationTrackSelection(option.id, $"Track rated ${scoreToStar(targetScore)} stars"));
                }
                DBPlayableType.Album -> {
                    //Either fetch the existing track list, or if first time selected, fetch a new fresh list from database.
                    val tracks = albumCache.getOrDefault(option.id, null) ?: StateDatabase.instance.getAlbumTrackIds(option.id).let {
                        val list = it.toMutableList();
                        albumCache[option.id] = list;
                        list;
                    }
                    val album = albumDetailsCache.getOrDefault(option.id, null) ?: StateDatabase.instance.getAlbum(option.id).let {
                        albumDetailsCache[option.id] = it;
                        it;
                    }
                    if(album?.shuffleCombined ?: false) {
                        options.remove(option);
                        tracks.map { RecommendationTrackSelection(it, $"Album rated ${scoreToStar(targetScore)} stars (Combined)", true) };
                    }
                    else {
                        val selectTrackIndex = random.nextInt(tracks.size);
                        val trackId = tracks[selectTrackIndex];
                        tracks.remove(trackId);
                        if (tracks.size == 0)
                            options.remove(option); //Remove the album from the possible options if no tracks remain
                        listOf(RecommendationTrackSelection(trackId, $"Album rated ${scoreToStar(targetScore)} stars"));
                    }
                }
                DBPlayableType.Artist -> {
                    val tracks = artistCache.getOrDefault(option.id, null) ?: StateDatabase.instance.getArtistTrackIds(option.id).let {
                        val list = it.toMutableList();
                        artistCache[option.id] = list;
                        list;
                    }
                    val selectTrackIndex = random.nextInt(tracks.size);
                    val trackId = tracks[selectTrackIndex];
                    tracks.remove(trackId);
                    if(tracks.size == 0)
                        options.remove(option);
                    listOf(RecommendationTrackSelection(trackId, $"Artist rated ${scoreToStar(targetScore)} stars"));
                }
                DBPlayableType.Playlist -> {
                    val tracks = playlistCache.getOrDefault(option.id, null) ?: StateDatabase.instance.getPlaylistTrackIds(option.id).let {
                        val list = it.toMutableList();
                        playlistCache[option.id] = list;
                        list;
                    }
                    val playlist = playlistDetailsCache.getOrDefault(option.id, null) ?: StateDatabase.instance.getPlaylist(option.id).let {
                        playlistDetailsCache[option.id] = it;
                        it;
                    }
                    if(playlist?.shuffleCombined ?: false) {
                        options.remove(option);
                        tracks.map { RecommendationTrackSelection(it, $"Playlist rated ${scoreToStar(targetScore)} stars (Combined)", true) }; //Return all tracks of a shuffleCombined playlist in their original order.
                    }
                    else {
                        val selectTrackIndex = random.nextInt(tracks.size);
                        val trackId = tracks[selectTrackIndex];
                        tracks.remove(trackId);
                        if (tracks.size == 0)
                            options.remove(option);
                        listOf(RecommendationTrackSelection(trackId, $"Playlist rated ${scoreToStar(targetScore)} stars"));
                    }
                }
                else -> listOf()
            }

            for(selectedTrackRecom in selectedTrackRecoms) {
                val trackScore = scores.getTrackById(selectedTrackRecom.id);
                if (trackScore != null) {
                    scores.removeTrack(selectedTrackRecom.id);
                    tracks.add(RecommendationTrack(trackScore.id, selectedTrackRecom.reason));
                }
                else if(selectedTrackRecom.forceAdd) {
                    tracks.add(RecommendationTrack(selectedTrackRecom.id, selectedTrackRecom.reason));
                }
            }
        }

        return tracks;
    }

    class RecommendationTrackSelection(id: Long, reason: String, val forceAdd: Boolean = false) : RecommendationTrack(id, reason);
}