package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TabHost
import androidx.room.Room
import androidx.room.RoomDatabase
import com.futo.music.RootApplication
import com.futo.music.constructs.Event0
import com.futo.music.levenshtein
import com.futo.music.levenshteinDistance
import com.futo.music.logging.Logger
import com.futo.music.logic.shuffles.ScoreContainer
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.PlayableType
import com.futo.music.storage.db.AppDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBAlbumUpdatePlayed
import com.futo.music.storage.db.DBAlbumUpdateRating
import com.futo.music.storage.db.DBAlbumUpdateTrackMetadata
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBArtistUpdatePlayed
import com.futo.music.storage.db.DBArtistUpdateRating
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBPlaylistTrack
import com.futo.music.storage.db.DBPlaylistUpdatePlayed
import com.futo.music.storage.db.DBPlaylistUpdateRating
import com.futo.music.storage.db.DBPlaylistUpdateTrackMetadata
import com.futo.music.storage.db.DBTrack
import com.futo.music.storage.db.DBTrackUpdateOpened
import com.futo.music.storage.db.DBTrackUpdatePlayed
import com.futo.music.storage.db.DBTrackUpdateRating
import com.futo.music.storage.db.DBTrackUpdateRatingCalculated
import java.time.OffsetDateTime

enum class DBPlayableType(val value: Int) {
    Track(1),
    Playlist(2),
    Album(3),
    Artist(4);


    companion object {
        fun ofValue(v: Int): DBPlayableType? {
            return when(v) {
                Track.value -> Track
                Playlist.value -> Playlist
                Album.value -> Album
                Artist.value -> Artist
                else -> null
            }
        }
        fun fromType(t: PlayableType): DBPlayableType? {
            return when(t) {
                PlayableType.Track -> Track
                PlayableType.Artist -> Artist
                PlayableType.Album -> Album
                PlayableType.Playlist -> Playlist
                else -> return null;
            }
        }
    }
}

class StateDatabase(
    val context: Context
) {
    val onLibraryUpdated = Event0();

    val db = Room
        .databaseBuilder(context.applicationContext, AppDatabase::class.java, "fmusic")
        //.fallbackToDestructiveMigration(true)
        .build();


    fun getScoresContainer(): ScoreContainer {
        return ScoreContainer.retrieve();
    }

    fun getRecentPlays(): List<IPlayable> {
        val recentAlbums = db.albumDao().getTopByRecentPlayed(15).filter { it.datePlayed.year > 2000 };
        val recentArtists = db.artistDao().getTopByRecentPlayed(15).filter { it.datePlayed.year > 2000 };
        val recentPlaylists = db.playlistDao().getTopByRecentPlayed(15).filter { it.datePlayed.year > 2000 };
        val recentTracks = db.tracksDao().getTopByRecentOpened(15).filter { it.dateOpened!!.year > 2000 };

        return (recentAlbums + recentArtists + recentPlaylists + recentTracks).sortedByDescending { it.datePlayed };
    }

    fun search(str: String): List<IPlayable> {
        val searchAlbums = db.albumDao().search(str);
        val searchArtists = db.artistDao().search(str);
        val searchTracks = db.tracksDao().search(str);

        return (searchAlbums + searchArtists + searchTracks).sortedBy { it.name.levenshtein(str) };
    }
    fun searchTracks(str: String): List<DBTrack> {
        val searchTracks = db.tracksDao().search(str);

        return (searchTracks).sortedBy { it.name.levenshtein(str) };
    }

    fun getAlbum(id: Long): DBAlbum? = db.albumDao().get(id);
    fun getTrack(id: Long): DBTrack? = db.tracksDao().get(id);
    fun getArtist(id: Long): DBArtist? = db.artistDao().get(id);
    fun getPlaylist(id: Long): DBPlaylist? = db.playlistDao().get(id);
    fun getPlaylistByName(name: String): DBPlaylist? = db.playlistDao().getByName(name);


    fun getAlbums(): List<DBAlbum> {
        return db.albumDao().getAll();
    }
    fun getAlbumsByRecent(): List<DBAlbum> {
        return db.albumDao().getAllByRecentPlayed();
    }
    fun getAlbumTracks(id: Long): List<DBTrack> {
        return db.tracksDao().getAlbumTracks(id);
    }
    fun getAlbumTrackIds(id: Long): List<Long> {
        return db.tracksDao().getAlbumTrackIds(id);
    }
    fun getTrackAlbums(id: Long): List<DBAlbum> {
        return db.albumDao().getTrackAlbums(id);
    }

    fun getArtists(): List<DBArtist> {
        return db.artistDao().getAll();
    }
    fun getArtistsByRecent(): List<DBArtist> {
        return db.artistDao().getAllByRecentPlayed();
    }
    fun getArtistTracks(id: Long): List<DBTrack> {
        return db.tracksDao().getArtistTracks(id);
    }
    fun getArtistTrackIds(id: Long): List<Long> {
        return db.tracksDao().getArtistTrackIds(id);
    }
    fun getArtistAlbums(id: Long): List<DBAlbum> {
        return db.albumDao().getArtistAlbums(id);
    }
    fun getTrackArtists(id: Long): List<DBArtist> {
        return db.artistDao().getTrackArtists(id);
    }

    fun getPlaylists(): List<DBPlaylist> {
        return db.playlistDao().getAll();
    }
    fun getPlaylistsByRecent(): List<DBPlaylist> {
        return db.playlistDao().getAllByRecentPlayed();
    }
    fun getPlaylistTracks(id: Long): List<DBTrack> {
        return db.tracksDao().getPlaylistTracks(id)
    }
    fun getPlaylistTrackIds(id: Long): List<Long> {
        return db.tracksDao().getPlaylistTrackIds(id)
    }
    fun getPlaylistTrackRefs(id: Long): List<DBPlaylistTrack> {
        return db.playlistDao().getPlaylistTrackRefs(id)
    }
    fun getTrackPlaylists(id: Long): List<DBPlaylist> {
        return db.playlistDao().getTrackPlaylists(id);
    }

    fun reorderPlaylist(playlistId: Long, tracks: List<DBTrack>){
        val playlistSongs = StateDatabase.instance.getPlaylistTrackRefs(playlistId);
        for(track in playlistSongs) {
            val index = track.ordering
            val indexNew = tracks.indexOfFirst { it.id == track.trackId };
            if(index != indexNew) {
                db.playlistDao().setPlaylistOrder(DBPlaylistTrack(track.playlistId, track.trackId, indexNew));
            }
        }
    }


    fun getTracksNew(count: Int): List<DBTrack> {
        return db.tracksDao().getTracksNew(count);
    }

    fun getTracksUnrated(count: Int, markedRated: Boolean = false): List<DBTrack> {
        val ids = db.tracksDao().getTracksUnratedIds(count, markedRated);
        return getTracks(ids);
    }
    fun getTracksNewUnrated(count: Int, markedRated: Boolean = false): List<DBTrack> {
        return db.tracksDao().getTracksNewUnrated(count, markedRated);
    }

    fun getTrackCount(): Int {
        return db.tracksDao().count();
    }
    fun getAllTracks(): List<DBTrack> {
        return db.tracksDao().getAll();
    }
    fun getTracks(ids: List<Long>, ordered: Boolean = true): List<DBTrack> {
        if(ordered)
            return db.tracksDao().getList(ids).sortedBy { ids.indexOf(it.id) };
        else
            return db.tracksDao().getList(ids);
    }

    fun getMostPlayed(count: Int): List<DBTrack> {
        return db.tracksDao().getMostPlayed(count);
    }


    fun updatePlaylistMetadata(playlistId: Long) {
        //TODO: Optimize to query
        val tracks = getPlaylistTracks(playlistId);
        val artList = mutableListOf<Pair<String, Long>>()
        for(track in tracks) {
            val albumArts = StateDatabase.instance.getTrackAlbumArt(track.id);
            if(albumArts?.isNotBlank() == true) {
                artList.add(Pair(albumArts, track.id));
                if(artList.size >= 4)
                    break;
            }
        }
        val trackDurations = if(tracks.size == 0) 0 else tracks.sumOf { it.duration };
        val trackCount = tracks.size;

        db.playlistDao().setTrackMetadata(DBPlaylistUpdateTrackMetadata(playlistId, trackCount, trackDurations,
            artUri1 = if(artList.size > 0) artList[0].first else null,
            artUriTrack1 = if(artList.size > 0) artList[0].second else null,
            artUri2 = if(artList.size > 1) artList[1].first else null,
            artUriTrack2 = if(artList.size > 1) artList[1].second else null,
            artUri3 = if(artList.size > 2) artList[2].first else null,
            artUriTrack3 = if(artList.size > 2) artList[2].second else null,
            artUri4 = if(artList.size > 3) artList[3].first else null,
            artUriTrack4 = if(artList.size > 3) artList[3].second else null));
    }


    fun setPlayedAlbum(albumId: Long) {
        return db.albumDao().setPlayed(DBAlbumUpdatePlayed(albumId, OffsetDateTime.now()));
    }
    fun setPlayedArtist(artistId: Long) {
        return db.artistDao().setPlayed(DBArtistUpdatePlayed(artistId, OffsetDateTime.now()));
    }
    fun setPlayedTrack(trackId: Long, opened: Boolean = false) {
        try {
            db.tracksDao().incrementTrackPlayed(trackId);
        } catch (ex: Throwable) {
            Logger.e(TAG, "Failed to increment plays", ex);
        }
        if(opened)
            return db.tracksDao().setOpened(DBTrackUpdateOpened(trackId, OffsetDateTime.now(), OffsetDateTime.now()));
        else
            return db.tracksDao().setPlayed(DBTrackUpdatePlayed(trackId, OffsetDateTime.now()));
    }
    fun setPlayedPlaylist(playlistId: Long) {
        return db.playlistDao().setPlayed(DBPlaylistUpdatePlayed(playlistId, OffsetDateTime.now()))
    }


    fun getTrackListWeighted(count: Int): List<DBTrack> {
        val tracks = db.tracksDao().getRandomWeightedTracks(count);

        return tracks;
    }
    fun getTrackIdsWeighted(count: Int): List<Long> {
        val trackIds = db.tracksDao().getRandomWeightedTrackIds(count);

        return trackIds;
    }
    fun getTrackListShuffled(count: Int): List<DBTrack> {
        val tracks = db.tracksDao().getRandomShuffledTracks(count);

        return tracks;
    }


    fun setRatingAlbum(albumId: Long, rating: Int): Boolean {
        val result = db.albumDao().setRating(DBAlbumUpdateRating(albumId, rating)) > 0;

        if(result) {
            val tracks = db.tracksDao().getAlbumTracks(albumId);
            updateTrackScores(tracks, DBPlayableType.Album, rating);
        }
        return result;
    }
    fun setRatingArtist(artistId: Long, rating: Int): Boolean {
        val result = db.artistDao().setRating(DBArtistUpdateRating(artistId, rating)) > 0;

        if(result) {
            val tracks = db.tracksDao().getArtistTracks(artistId);
            updateTrackScores(tracks, DBPlayableType.Artist, rating);
        }

        return result;
    }
    fun setRatingPlaylist(playlistId: Long, rating: Int): Boolean {
        val result = db.playlistDao().setRating(DBPlaylistUpdateRating(playlistId, rating)) > 0;

        if(result) {
            val tracks = db.tracksDao().getPlaylistTracks(playlistId);
            updateTrackScores(tracks, DBPlayableType.Playlist, rating);
        }

        return result;
    }
    fun setRatingTrack(trackId: Long, rating: Int): Pair<DBPlayableType?, Int>? {
        val result = db.tracksDao().setRating(DBTrackUpdateRating(trackId, rating)) > 0;

        val track = db.tracksDao().get(trackId);
        if(track != null) {
            return updateTrackScores(listOf(track), DBPlayableType.Track, rating).firstOrNull();
        }

        return null;
    }

    fun updateTrackScores(tracks: List<DBTrack>, newScoreType: DBPlayableType, newScore: Int): List<Pair<DBPlayableType?, Int>> {
        var count = 0;
        var results = mutableListOf<Pair<DBPlayableType?, Int>>();
        for(track in tracks) {
            if(newScore <= 0){
                val recalcScore = recalculateTrackScore(track);
                db.tracksDao().setRatingCalculated(DBTrackUpdateRatingCalculated(track.id, recalcScore.first?.value ?: 0, recalcScore.second))
                results.add(Pair(recalcScore.first, recalcScore.second));
            }
            else if(track.scoreLevel == newScoreType.value && track.scoreCalculated != newScore) {
                db.tracksDao().setRatingCalculated(DBTrackUpdateRatingCalculated(track.id, newScoreType.value, newScore));
                count++;
                results.add(Pair(newScoreType, newScore));
            }
            else if(track.scoreLevel > newScoreType.value || track.scoreLevel <= 0) {
                db.tracksDao().setRatingCalculated(DBTrackUpdateRatingCalculated(track.id, newScoreType.value, newScore));
                count++;
                results.add(Pair(newScoreType, newScore));
            }
            else if(track.scoreLevel == newScoreType.value && track.scoreCalculated == newScore){
                //No update
                results.add(Pair(newScoreType, newScore));
            }
            else {
                Logger.e(TAG, "Unhandled edgecase for rating update");
            }
        }
        return results;
    }

    fun recalculateTrackScore(track: DBTrack): Pair<DBPlayableType?, Int> {
        var score = 0;

        if(track.score > 0 && track.scoreLevel == DBPlayableType.Track.value)
            return Pair(DBPlayableType.Track, track.score);

        val playlists = StateDatabase.instance.getTrackPlaylists(track.id);
        if(playlists.any { it.score > 0 })
            return Pair(DBPlayableType.Playlist, playlists.maxOf { it.score });

        val albums = StateDatabase.instance.getTrackAlbums(track.id);
        if(albums.any { it.score > 0 })
            return Pair(DBPlayableType.Album, albums.maxOf { it.score });

        val artists = StateDatabase.instance.getTrackArtists(track.id);
        if(artists.any { it.score > 0 })
            return Pair(DBPlayableType.Artist, artists.maxOf { it.score });

        return Pair(null, 0);
    }



    fun getTrackAlbumArt(id: Long): String? {
        return db.albumDao().getTrackAlbumArts(id).firstOrNull()
    }


    fun insertOrUpdate(track: DBTrack, explicitUpdateId: Long = -1): Long {
        if(explicitUpdateId > 0) {
            db.tracksDao().update(track);
            return explicitUpdateId;
        }
        return db.tracksDao().insert(track).first();
    }
    fun insertOrUpdate(artist: DBArtist): Long {
        return db.artistDao().insert(artist).first();
    }
    fun insertOrUpdate(album: DBAlbum): Long {
        return db.albumDao().insert(album).first();
    }
    fun insertOrUpdate(playlist: DBPlaylist): Long {
        return db.playlistDao().insert(playlist).first();
    }


    fun createPlaylist(name: String): Long {
        val result = insertOrUpdate(DBPlaylist(
            name = name,
            dateAdded = OffsetDateTime.now(),
            datePlayed = OffsetDateTime.MIN,
            score = -1,
            dateModified = OffsetDateTime.now()
        ));
        return result;
    }
    fun addTrackToPlaylist(playlistId: Long, trackId: Long, order: Int = -1): Long {
        val orderToUse = if(order >= 0)
            order
        else
            db.playlistDao().getPlaylistMaxOrder(playlistId) + 1;
       return  db.playlistDao().insert(DBPlaylistTrack(playlistId, trackId, orderToUse)).first();
    }
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        return db.playlistDao().deletePlaylistTrack(playlistId, trackId);
    }

    fun deletePlaylist(id: Long) {
        return db.playlistDao().deletePlaylist(id);
    }

    fun getAlbumByMSID(id: Long): DBAlbum? {
        return db
            .albumDao()
            .getByMSID(id);
    }

    fun getArtistByMSID(id: Long): DBArtist? {
        return db
            .artistDao()
            .getByMSID(id);
    }

    fun getTrackByFileName(fileName: String): DBTrack? {
        return db
            .tracksDao()
            .getByFileName(fileName);
    }
    fun getTrackIdByFileName(fileName: String): Long? {
        return db
            .tracksDao()
            .getIdByFileName(fileName);
    }
    fun getTrackByMSID(id: Long): DBTrack? {
        return db
            .tracksDao()
            .getByMSID(id);
    }



    companion object {
        private val TAG = "StateMedia";
        @SuppressLint("StaticFieldLeak") //This is only alive while MainActivity is alive
        private var _instance : StateDatabase? = null;
        val instance : StateDatabase
            get(){
                if(_instance == null) {
                    _instance = StateDatabase(RootApplication.applicationContext);
                }
                return _instance!!
            };

    }
}