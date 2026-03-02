package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.futo.music.RootApplication
import com.futo.music.levenshtein
import com.futo.music.levenshteinDistance
import com.futo.music.models.playable.IPlayable
import com.futo.music.storage.db.AppDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBAlbumUpdatePlayed
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBArtistUpdatePlayed
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBPlaylistUpdatePlayed
import com.futo.music.storage.db.DBTrack
import com.futo.music.storage.db.DBTrackUpdatePlayed
import java.time.OffsetDateTime

class StateDatabase(
    val context: Context
) {
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


    fun insertOrUpdate(track: DBTrack): Long {
        return db.tracksDao().insert(track).first();
    }
    fun insertOrUpdate(artist: DBArtist): Long {
        return db.artistDao().insert(artist).first();
    }
    fun insertOrUpdate(album: DBAlbum): Long {
        return db.albumDao().insert(album).first();
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