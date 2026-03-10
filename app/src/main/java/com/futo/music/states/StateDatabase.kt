package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.futo.music.RootApplication
import com.futo.music.constructs.Event0
import com.futo.music.levenshtein
import com.futo.music.levenshteinDistance
import com.futo.music.models.playable.IPlayable
import com.futo.music.storage.db.AppDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBAlbumUpdatePlayed
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBArtistUpdatePlayed
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBPlaylistTrack
import com.futo.music.storage.db.DBPlaylistUpdatePlayed
import com.futo.music.storage.db.DBPlaylistUpdateTrackMetadata
import com.futo.music.storage.db.DBTrack
import com.futo.music.storage.db.DBTrackUpdatePlayed
import java.time.OffsetDateTime

class StateDatabase(
    val context: Context
) {
    val onLibraryUpdated = Event0();

    val db = Room
        .databaseBuilder(context.applicationContext, AppDatabase::class.java, "fmusic")
        .fallbackToDestructiveMigration(true)
        .build();

    fun getRecentPlays(): List<IPlayable> {
        val recentAlbums = db.albumDao().getTopByRecentPlayed(15).filter { it.datePlayed.year > 2000 };
        val recentArtists = db.artistDao().getTopByRecentPlayed(15).filter { it.datePlayed.year > 2000 };
        val recentPlaylists = db.playlistDao().getTopByRecentPlayed(15).filter { it.datePlayed.year > 2000 };

        return (recentAlbums + recentArtists + recentPlaylists).sortedByDescending { it.datePlayed };
    }

    fun search(str: String): List<IPlayable> {
        val searchAlbums = db.albumDao().search(str);
        val searchArtists = db.artistDao().search(str);
        val searchTracks = db.tracksDao().search(str);

        return (searchAlbums + searchArtists + searchTracks).sortedBy { it.name.levenshtein(str) };
    }

    fun getAlbum(id: Long): DBAlbum? = db.albumDao().get(id);
    fun getTrack(id: Long): DBTrack? = db.tracksDao().get(id);
    fun getArtist(id: Long): DBArtist? = db.artistDao().get(id);
    fun getPlaylist(id: Long): DBPlaylist? = db.playlistDao().get(id);


    fun getAlbums(): List<DBAlbum> {
        return db.albumDao().getAll();
    }
    fun getAlbumsByRecent(): List<DBAlbum> {
        return db.albumDao().getAllByRecentPlayed();
    }
    fun getAlbumTracks(id: Long): List<DBTrack> {
        return db.tracksDao().getAlbumTracks(id);
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
    fun getTrackPlaylists(id: Long): List<DBPlaylist> {
        return db.playlistDao().getTrackPlaylists(id);
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
    fun setPlayedTrack(trackId: Long) {
        return db.tracksDao().setPlayed(DBTrackUpdatePlayed(trackId, OffsetDateTime.now()));
    }
    fun setPlayedPlaylist(playlistId: Long) {
        return db.playlistDao().setPlayed(DBPlaylistUpdatePlayed(playlistId, OffsetDateTime.now()))
    }

    fun getTrackAlbumArt(id: Long): String? {
        return db.albumDao().getTrackAlbumArts(id).firstOrNull()
    }


    fun insertOrUpdate(track: DBTrack): Long {
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