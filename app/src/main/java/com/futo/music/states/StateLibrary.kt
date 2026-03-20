package com.futo.music.states

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Artists
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.futo.music.RootApplication
import com.futo.music.constructs.Event0
import com.futo.music.logging.Logger
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.Album
import com.futo.music.models.playable.Artist
import com.futo.music.models.playable.PlayableType
import com.futo.music.models.playable.Track
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBAlbumArtist
import com.futo.music.storage.db.DBAlbumTrack
import com.futo.music.storage.db.DBAlbumUpdateTrackMetadata
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBArtistTrack
import com.futo.music.storage.db.DBArtistUpdateTrackMetadata
import com.futo.music.storage.db.DBPlaylistUpdateTrackMetadata
import com.futo.music.storage.db.DBTrack
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.StringArrayStorage
import com.futo.music.storage.file.StringStringMapStorage
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

//Integrations with Android Mediastore, ideally not used directly, but first synced to local DB.
//TODO: Clean this class up
class StateLibrary {

    val onSyncCompleted = Event0();

    private val _mediaStoreVersions = FragmentedStorage.get<StringStringMapStorage>("mediaStoreVersions");


    fun requiresSync(context: Context): List<String> {
        val volumes = MediaStore.getExternalVolumeNames(context);
        val changedVolumes = mutableListOf<String>();
        for(volume in volumes) {
            val mediaStoreVersion = _mediaStoreVersions.get(volume);
            if(mediaStoreVersion == null)
                changedVolumes.add(volume);
            else if(mediaStoreVersion != MediaStore.getVersion(context, volume))
                changedVolumes.add(volume);
        }
        return changedVolumes;
    }
    fun syncDatabase(context: Context, volumes: List<String>, onProgress: (Int, Int, String)->Unit) {
        for(vol in volumes)
            syncDatabaseVolume(context, vol, onProgress);
    }
    fun syncDatabaseVolume(context: Context, volume: String, onProgress: (Int, Int, String)->Unit) {
    }
    fun resetSyncs() {
        _mediaStoreVersions.map.clear();
        _mediaStoreVersions.saveBlocking();
    }
    fun requireSync(context: Context): Boolean {
        if(_mediaStoreVersions.map.count() == 0)
            return true;

        val generalMediaStoreVersion = MediaStore.getVersion(context);
        for(version in _mediaStoreVersions.map.toList()) {
            if(MediaStore.getVersion(context, version.first) != version.second)
                return true;
        }
        return false;
    }
    fun syncDatabase(context: Context, onProgress: (Int, Int, String, String)->Unit): ImportResult {
        val albums = getAlbums(context);
        var albumPos = 0;
        var albumNew = 0;
        for(album in albums) {
            if(updateAlbum(album))
                albumNew++;
            albumPos++;
            onProgress(albums.size, albumPos, "album", "Syncing Album " + album.name);
        }
        val artists = getArtists(context, ArtistOrdering.Alphabethic);
        var artistPos = 0;
        var artistNew = 0;
        for(artist in artists) {
            if(updateArtist(artist))
                artistNew++;
            artistPos++;
            onProgress(artists.size, artistPos, "artist", "Syncing artist " + artist.name);
        }
        var trackPos = 0;
        var trackNew = 0;
        allTracks(context) { count, progress, track ->
            if(updateTrack(track))
                trackNew++;
            trackPos++;
            onProgress(count, progress, "track", "Syncing track " + track.name);
        };

        val mediaStoreVersion = MediaStore.getVersion(context);
        _mediaStoreVersions.setAndSave(MediaStore.VOLUME_EXTERNAL_PRIMARY, mediaStoreVersion);

        syncDatabaseMetadata(onProgress);

        onSyncCompleted.emit();
        return ImportResult(albumPos, artistPos, trackPos,
            albumNew, artistNew, trackNew);
    }

    fun syncDatabaseMetadata(onProgress: (Int, Int, String, String)->Unit) {
        //TODO: Optimize these into more efficient queries

        val allAlbums = StateDatabase.instance.getAlbums();
        var albumPos = 0;
        for(album in allAlbums) {
            val allTracks = StateDatabase.instance.getAlbumTracks(album.id);
            val count = allTracks.size;
            val duration = if(count > 0) allTracks.sumOf { it.duration } else 0;
            StateDatabase.instance.db.albumDao().setTrackMetadata(DBAlbumUpdateTrackMetadata(album.id, count, duration));
            onProgress?.invoke(allAlbums.size, albumPos, "album", "Updating Album ${album.name}");
            albumPos++;
        }

        val allArtists = StateDatabase.instance.getArtists();
        var artistPos = 0;
        for(artist in allArtists) {
            val allTracks = StateDatabase.instance.getArtistTracks(artist.id);
            val likelyAlbumArts = allTracks.groupBy { it.mediaStoreAlbumId }.toList().sortedBy { it.second.size };
            var artUri: String? = null;
            for(album in likelyAlbumArts) {
                if(album.second.size > 3) {
                    val artAlbum = StateDatabase.instance.db.albumDao().getByMSID(album.first);
                    artUri = artAlbum?.artUri;
                }
                if(artUri != null)
                    break;
            }
            if(artUri == null && artist.artUri != null)
                artUri = artist.artUri;

            val count = allTracks.size;
            val duration = if(count > 0) allTracks.sumOf { it.duration } else 0;
            StateDatabase.instance.db.artistDao().setTrackMetadata(DBArtistUpdateTrackMetadata(artist.id, count, duration, artUri));
            onProgress?.invoke(allArtists.size, artistPos, "artist", "Updating Artist ${artist.name}");
            artistPos++;
        }

        val allPlaylists = StateDatabase.instance.getPlaylists();
        var playlistPos = 0;
        for(playlist in allPlaylists) {
            val allTracks = StateDatabase.instance.getPlaylistTracks(playlist.id);
            val count = allTracks.size;
            val duration = if(count > 0) allTracks.sumOf { it.duration } else 0;

            val artList = mutableListOf<Pair<String, Long>>()
            for(track in allTracks) {
                val albumArts = StateDatabase.instance.getTrackAlbumArt(track.id);
                if(albumArts?.isNotBlank() == true) {
                    artList.add(Pair(albumArts, track.id));
                    if(artList.size >= 4)
                        break;
                }
            }

            StateDatabase.instance.db.playlistDao().setTrackMetadata(DBPlaylistUpdateTrackMetadata(playlist.id, count, duration,
                artUri1 = if(artList.size > 0) artList[0].first else null,
                artUriTrack1 = if(artList.size > 0) artList[0].second else null,
                artUri2 = if(artList.size > 1) artList[1].first else null,
                artUriTrack2 = if(artList.size > 1) artList[1].second else null,
                artUri3 = if(artList.size > 2) artList[2].first else null,
                artUriTrack3 = if(artList.size > 2) artList[2].second else null,
                artUri4 = if(artList.size > 3) artList[3].first else null,
                artUriTrack4 = if(artList.size > 3) artList[3].second else null));
            onProgress?.invoke(allPlaylists.size, playlistPos, "playlist", "Updating Playlist ${playlist.name}");
            playlistPos++;
        }
    }


    class ImportResult(
        val albums: Int,
        val artists: Int,
        val tracks: Int,
        val albumsNew: Int,
        val artistsNew: Int,
        val tracksNew: Int
    )

    fun checkAlbumArt(albumMediastoreId: Long): Boolean {
        try {
            val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumMediastoreId);
            val bmp = RootApplication.applicationContext.contentResolver.loadThumbnail(albumUri, Size(512, 512), null);
            val exists = bmp != null;
            bmp.recycle();
            return exists;
        }
        catch(ex: Throwable) {
            Logger.e(TAG, "Failed to load thumbnail for album: ${albumMediastoreId}");
            return false;
        }
    }

    fun updateAlbum(album: AndroidAlbum): Boolean {
        val id = album.id.toLongOrNull() ?: return false;
        val existing = StateDatabase.instance.getAlbumByMSID(id);

        if(!checkAlbumArt(album.id.toLong()))
            album.thumbnail = null;

        val dbEntry = DBAlbum(
            id = existing?.id ?: 0,
            name = album.name,
            authors = album?.artist ?: existing?.authors ?: "",
            score = existing?.score ?: -1,
            artUri = album.thumbnail ?: existing?.artUri,
            dateAdded = if(existing != null) existing.dateAdded else OffsetDateTime.now(),
            datePlayed = if(existing != null) existing.datePlayed else OffsetDateTime.MIN,
            plays = if(existing != null) existing.plays else 0,
            mediaStoreId = album?.id?.toLongOrNull() ?: existing?.mediaStoreId ?: -1,
            mediaStoreArtistId = album.artistId ?: existing?.mediaStoreArtistId ?: -1
        )

        Logger.i(TAG, "Inserting album [${album.name}] (new: ${existing == null})")
        dbEntry.id = StateDatabase.instance.db.albumDao().insert(dbEntry).first();
        return existing == null;
    }
    fun updateArtist(artist: AndroidArtist): Boolean {
        val id = artist.id.toLongOrNull() ?: return false;
        val existing = StateDatabase.instance.getArtistByMSID(id);

        val dbAlbum = if(artist.id?.toLongOrNull() != null)
            StateDatabase.instance.db.albumDao().getByArtistMSID(artist.id.toLong())
        else null;

        val dbEntry = DBArtist(
            id = existing?.id ?: 0,
            name = artist.name,
            score = existing?.score ?: -1,
            artUri = existing?.artUri,
            dateAdded = if(existing != null) existing.dateAdded else OffsetDateTime.now(),
            datePlayed = if(existing != null) existing.datePlayed else OffsetDateTime.MIN,
            plays = if(existing != null) existing.plays else 0,
            mediaStoreId = artist.id?.toLongOrNull() ?: existing?.mediaStoreId ?: -1,
        );

        Logger.i(TAG, "Inserting artist [${artist.name}] (new: ${existing == null})")
        dbEntry.id = StateDatabase.instance.db.artistDao().insert(dbEntry).first();

        if(dbAlbum != null)
            StateDatabase.instance.db.albumDao().insert(DBAlbumArtist(dbAlbum.id, dbEntry.id));
        return existing == null;
    }
    fun updateTrack(track: Track): Boolean {
        val id = track.id.toLongOrNull() ?: return false;
        val existing = StateDatabase.instance.getTrackByMSID(id);
        val artistId = track.artist?.id?.toLongOrNull() ?: existing?.artistId;
        val albumId = track.album?.id?.toLongOrNull() ?: existing?.mediaStoreAlbumId;

        val dbArtist = if(artistId != null) StateDatabase.instance.getArtistByMSID(artistId) else null;
        val dbAlbum = if(albumId != null) StateDatabase.instance.getAlbumByMSID(albumId) else null;

        val dbEntry = DBTrack(
            id = existing?.id ?: 0,
            name = track.name,
            author = track.artist?.name ?: track.name,
            artistId = dbArtist?.id ?: -1,
            artistLine = dbArtist?.name ?: existing?.artistLine,
            albumLine = dbAlbum?.name ?: existing?.albumLine,
            contentUrl = track.uri.toString(),
            dateAdded = if(existing != null) existing.dateAdded else OffsetDateTime.now(),
            datePlayed = if(existing != null) existing.datePlayed else OffsetDateTime.MIN,
            plays = if(existing != null) existing.plays else 0,
            skips = if(existing != null) existing.skips else 0,
            score = existing?.score ?: -1,
            scoreCalculated = existing?.scoreCalculated ?: -1,
            duration = track.duration ?: 0,
            mediaStoreId = id,
            mediaStoreArtistId = track.artist?.id?.toLongOrNull() ?: existing?.mediaStoreArtistId ?: -1,
            mediaStoreAlbumId = track.album?.id?.toLongOrNull() ?: existing?.mediaStoreAlbumId ?: -1,
        )

        Logger.i(TAG, "Inserting track [${track.name}] (new: ${existing == null})")
        dbEntry.id = StateDatabase.instance.insertOrUpdate(dbEntry);
;        if(dbAlbum != null)
            StateDatabase.instance.db.albumDao().insert(DBAlbumTrack(dbAlbum.id, dbEntry.id));
        if(dbArtist != null)
            StateDatabase.instance.db.artistDao().insert(DBArtistTrack(dbArtist.id, dbEntry.id));
        return existing == null;
    }



    private val _files = FragmentedStorage.get<StringArrayStorage>("libraryFiles")

    fun searchTracks(context: Context, str: String): List<Track> {
        if(str.isNullOrBlank())
            return listOf();
        val resolver =  context.contentResolver;
        if(resolver == null) {
            Logger.w(TAG, "Album contentResolver not found");
            return listOf();
        }
        val cursor = resolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, StateLibrary.PROJECTION_MEDIA,
            "LOWER(" + MediaStore.Audio.Media.DISPLAY_NAME + ") LIKE ? ",
            arrayOf("%" + str.trim().lowercase() + "%"),
            null) ?: return listOf();
        return cursor.use {
            cursor.moveToFirst();
            val list = mutableListOf<Track>()
            while(!cursor.isAfterLast) {
                list.add(audioFromCursor(cursor));
                cursor.moveToNext();
            }
            return@use list;
        }
    }
    fun allTracks(context: Context, handle: (Int, Int, Track)->Unit) {
        val resolver =  context.contentResolver;
        if(resolver == null) {
            Logger.w(TAG, "Album contentResolver not found");
            return;
        }
        val cursor = resolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, StateLibrary.PROJECTION_MEDIA,
            null,
            null,
            null) ?: return;
        return cursor.use {
            cursor.moveToFirst();
            while(!cursor.isAfterLast) {
                handle(cursor.count, cursor.position, audioFromCursor(cursor))
                cursor.moveToNext();
            }
            return@use;
        }
    }

    fun getAlbums(context: Context): List<AndroidAlbum> {
        return AndroidAlbum.getAlbums(context);
    }
    fun getAlbum(context: Context, str: String): AndroidAlbum? {
        val idLong = str.toLongOrNull();
        if(idLong != null)
            return getAlbum(context, idLong);
        return null;
    }
    fun searchAlbums(context: Context, str: String): List<AndroidAlbum> {
        if(str.isNullOrBlank())
            return listOf();
        return AndroidAlbum.getAlbums(context, "LOWER(" + MediaStore.Audio.Albums.ALBUM + ") LIKE ? ",
            arrayOf("%" + str.trim().lowercase() + "%")
        );
    }

    fun getAlbum(context: Context, id: Long): AndroidAlbum? {
        return AndroidAlbum.getAlbum(context, id);
    }
    fun getAlbumTracks(context: Context, albumId: Long): List<Track> {
        val resolver =  context.contentResolver;
        if(resolver == null) {
            Logger.w(TAG, "Album contentResolver not found");
            return listOf();
        }
        val cursor = resolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, StateLibrary.PROJECTION_MEDIA, "${MediaStore.Audio.Media.ALBUM_ID} = ?", arrayOf(albumId.toString()),
            null) ?: return listOf();
        return cursor.use {
            cursor.moveToFirst();
            val list = mutableListOf<Track>()
            while(!cursor.isAfterLast) {
                list.add(StateLibrary.audioFromCursor(cursor));
                cursor.moveToNext();
            }
            return@use list;
        }
    }

    fun getArtists(context: Context, ordering: ArtistOrdering): List<AndroidArtist> {
        return AndroidArtist.getArtists(context, ordering);
    }
    fun getArtist(context: Context, str: String): AndroidArtist? {
        val idLong = str.toLongOrNull();
        if(idLong != null)
            return getArtist(context, idLong);
        return null;
    }
    fun searchArtists(context: Context, str: String): List<AndroidArtist> {
        if(str.isNullOrBlank())
            return listOf();
        return AndroidArtist.getArtists(context, ArtistOrdering.TrackCount, "LOWER(" + Artists.ARTIST + ") LIKE ? ",
            arrayOf("%" + str.trim().lowercase() + "%")
        );
    }

    fun getArtist(context: Context, id: Long): AndroidArtist? {
        return AndroidArtist.getArtist(context, id);
    }
    fun getArtistTracks(context: Context, artistId: Long): List<Track> {
        val resolver =  context.contentResolver;
        if(resolver == null) {
            Logger.w(TAG, "Album contentResolver not found");
            return listOf();
        }
        val cursor = resolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, StateLibrary.PROJECTION_MEDIA, "${MediaStore.Audio.Media.ARTIST_ID} = ?", arrayOf(artistId.toString()),
            null) ?: return listOf();
        return cursor.use {
            cursor.moveToFirst();
            val list = mutableListOf<Track>()
            while(!cursor.isAfterLast) {
                list.add(StateLibrary.audioFromCursor(cursor));
                cursor.moveToNext();
            }
            return@use list;
        }
    }

    companion object {
        val TAG = "Library";
        val PROJECTION_VIDEO = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        );
        val PROJECTION_MEDIA = arrayOf(
            MediaStore.Audio.Media._ID, //0
            MediaStore.Audio.Media.DISPLAY_NAME, //1
            MediaStore.Audio.Media.ARTIST, //2
            MediaStore.Audio.Media.ARTIST_ID, //3
            MediaStore.Audio.Media.ALBUM_ID, //4
            MediaStore.Audio.Media.DURATION, //5
            MediaStore.Audio.Media.DATE_ADDED, //6
            MediaStore.Audio.Media.MIME_TYPE, //7
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME, //8
            MediaStore.Audio.Media.TITLE //9
        );

        fun getDocumentTrack(context: Context, url: String): Track? {
            if(!url.contains("com.android.externalstorage.documents"))
                return null;
            val docFile = DocumentFile.fromSingleUri(context, url.toUri()) ?: return null;

            val contentUri = docFile.uri.toString();

            val mimeType = MimeTypeMap.getFileExtensionFromUrl(contentUri);

            if(docFile.name != null) {
                if (true && mimeType.startsWith("audio/")) { //StateApp.instance.hasMediaStoreAudioPermission
                    val aud = findAudioByName(context, docFile.name!!);
                    if (aud != null)
                        return aud;
                }
            }

            throw NotImplementedError("TODO: Implement conversion");
        }

        fun getAudioTrack(context: Context, url: String): Track? {
            val uri = Uri.parse(url);
            val id = uri.lastPathSegment?.toLongOrNull();
            if(id == null) {
                return getDocumentTrack(context, url);
            }

            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Album contentResolver not found");
                return null;
            }
            val cursor = resolver?.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, StateLibrary.PROJECTION_MEDIA, "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(id.toString()),
                null) ?: return null;
            return cursor.use {
                cursor.moveToFirst();
                if(cursor.isAfterLast)
                    return@use null;
                return@use audioFromCursor(cursor);
            }
        }
        fun findAudioByName(context: Context, name: String): Track? {
            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Audio contentResolver not found");
                return null;
            }
            val cursor = resolver?.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, StateLibrary.PROJECTION_MEDIA, "${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                arrayOf(name),
                null) ?: return null;
            return cursor.use {
                cursor.moveToFirst();
                if(cursor.isAfterLast)
                    return null;
                return@use audioFromCursor(cursor);
            }
        }

        fun audioFromCursor(cursor: Cursor): Track {
            val id = cursor.getString(0);
            val displayName = cursor.getString(1);
            val author = cursor.getString(2);
            val authorId = cursor.getStringOrNull(3);
            val albumId = cursor.getLong(4);
            val duration = cursor.getLong(5).let { if(it > 0) it / 1000 else 0 };
            val date = cursor.getLong(6);
            val contentType = cursor.getString(7);
            val category = cursor.getString(8);
            val title = cursor.getString(9);

            val idLong = id.toLongOrNull();
            val contentUrl = if(idLong != null )
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, idLong).toString();
            else
                "";

            val authorIdLong = authorId?.toLongOrNull();
            val authorUrl = if(authorIdLong != null)
                ContentUris.withAppendedId(Artists.EXTERNAL_CONTENT_URI, authorIdLong).toString();
            else
                "";


            val albumArtBase = Uri.parse("content://media/external/audio/albumart")
            val albumContentUrl = if (albumId > 0)
                ContentUris.withAppendedId(albumArtBase, albumId).toString()
            else null

            val dateObj = if(date > 0)
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(date), ZoneOffset.UTC)
            else null;

            val artist = if(authorId != null)
                Artist(author, null, authorId);
            else null;

            val album = if(albumArtBase != null)
                Album("", ImageVariable.fromUrl(albumContentUrl), listOf(), artist, albumId.toString());
            else null;

            return Track(if(title.isNullOrBlank()) displayName else title, contentUrl.toUri(), id)
                .withArtist(artist)
                .withAlbum(album)
                .withDuration(duration.toInt());
        }

        private var _instance : StateLibrary? = null;
        val instance : StateLibrary
            get(){
            if(_instance == null)
                _instance = StateLibrary();
            return _instance!!;
        };

        fun finish() {
            _instance?.let {
                _instance = null;
            }
        }
    }
}

class Bucket(val id: Long, val name: String);


enum class ArtistOrdering {
    Alphabethic,
    TrackCount,
    AlbumCount
}
class AndroidArtist {
    val type = PlayableType.Artist;
    val id: String;
    val name: String;
    val countTracks: Int;
    val countAlbums: Int;
    val thumbnail: String?;
    val contentUrl: String?;

    constructor(name: String, countTracks: Int = -1, countAlbums: Int = -1, thumbnail: String? = null, id: String? = null, contentUrl: String? = null) {
        this.id = id ?: ID_UNKNOWN;
        this.name = name;
        this.thumbnail = thumbnail;
        this.countTracks = countTracks;
        this.countAlbums = countAlbums;
        this.contentUrl = contentUrl;
    }


    fun toArtist(): Artist {
        return Artist(name, ImageVariable.fromUrl(thumbnail), id);
    }

    fun getAlbums(context: Context): List<AndroidAlbum> {
        return AndroidAlbum.getArtistAlbums(context, id.toLongOrNull() ?: return listOf());
    }

    fun getThumbnailOrAlbum(context: Context): String? {
        return thumbnail ?: tryGetArtistThumbnail(context, id.toLongOrNull());
    }

    companion object {
        val TAG = "Library";
        val ID_UNKNOWN = "UNKNOWN";
        val PROJECTION: Array<String> = arrayOf(
            Artists._ID,
            Artists.ARTIST,
            Artists.NUMBER_OF_TRACKS,
            Artists.NUMBER_OF_ALBUMS
        );

        val thumbnailCache = ConcurrentHashMap<Long, String>();

        fun tryGetArtistThumbnail(context: Context, artistId: Long?): String? {
            if(artistId == null)
                return null;
            if(thumbnailCache.containsKey(artistId))
                return thumbnailCache.get(artistId);
            else {
                val album = AndroidAlbum.getArtistAlbumWithThumbnail(context, artistId);
                thumbnailCache.put(artistId, album?.thumbnail ?: "");
                return album?.thumbnail;
            }
        }

        fun fromCursor(cursor: Cursor): AndroidArtist {
            val id = cursor.getString(0);
            val artist = cursor.getString(1);
            val numTracks = cursor.getInt(2);
            val numAlbums = cursor.getInt(3);

            val idLong = id.toLongOrNull()
            val uri = if (idLong != null)
                ContentUris.withAppendedId(Artists.EXTERNAL_CONTENT_URI, idLong)
            else null

            return AndroidArtist(artist, numTracks, numAlbums, null, id, uri?.toString())        }

        fun getArtist(context: Context, id: Long): AndroidArtist? {
            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Artist contentResolver not found");
                return null
            }
            val cursor = resolver.query(
                Artists.EXTERNAL_CONTENT_URI,
                AndroidArtist.PROJECTION,
                "${Artists._ID} = ?",
                arrayOf(id.toString()), null) ?: return null;
            return cursor.use {
                cursor.moveToFirst();
                if(cursor.isAfterLast)
                    return@use null;
                return@use fromCursor(cursor);
            }
        }
        fun getArtists(context: Context, ordering: ArtistOrdering = ArtistOrdering.Alphabethic, query: String? = null, args: Array<String>? = null): List<AndroidArtist> {
            val ordering = when(ordering) {
                ArtistOrdering.Alphabethic -> Artists.ARTIST + " ASC";
                ArtistOrdering.AlbumCount -> Artists.NUMBER_OF_ALBUMS + " DESC";
                ArtistOrdering.TrackCount -> Artists.NUMBER_OF_TRACKS + " DESC";
                else -> null
            }

            val cursor = context.contentResolver?.query(Artists.EXTERNAL_CONTENT_URI, PROJECTION,
                query,
                args,
                ordering) ?: return listOf();
            return cursor.use {
                cursor.moveToFirst();
                val list = mutableListOf<AndroidArtist>()
                while(!cursor.isAfterLast) {
                    val artist = fromCursor(cursor);
                    cursor.moveToNext();
                    if(artist.name == "<unknown>")
                        continue; //TODO: Better way of detecting unknown?
                    list.add(artist);
                }
                return@use list;
            }
        }
    }
}

class AndroidAlbum {
    val id: String;
    val name: String;
    val artist: String?;
    val artistId: Long?;
    val countTracks: Int;
    var thumbnail: String?;

    constructor(name: String, countTracks: Int = -1, artist: String? = null, id: String? = null, thumbnail: String? = null, artistId: Long? = null) {
        this.id = id ?: ID_UNKNOWN;
        this.name = name;
        this.artist = artist;
        this.countTracks = countTracks;
        this.thumbnail = thumbnail;
        this.artistId = artistId;
    }

    fun toAlbum(): Album {
        return Album(name, ImageVariable.fromUrl(thumbnail), null, null, id);
    }

    fun getTracks(context: Context): List<Track> {
        return getAlbumTracks(context, id.toLongOrNull() ?: return listOf())
    }

    companion object {
        val TAG = "StateLibrary";
        val ID_UNKNOWN = "UNKNOWN";
        val PROJECTION = arrayOf(
            MediaStore.Audio.Albums.ALBUM_ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.ARTIST_ID
        );

        fun fromCursor(cursor: Cursor): AndroidAlbum {
            val id = cursor.getString(0);
            val album = cursor.getString(1);
            val numTracks = cursor.getInt(2);
            val artist = cursor.getString(3);
            val artistId = cursor.getLong(4);

            val idLong = id.toLongOrNull()
            val albumArtBase = Uri.parse("content://media/external/audio/albumart")
            val uri = if (idLong != null) ContentUris.withAppendedId(albumArtBase, idLong) else null
            return AndroidAlbum(album, numTracks, artist, id, uri?.toString(), artistId)
        }

        fun getAlbumTracks(context: Context, albumId: Long): List<Track> {
            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Album contentResolver not found");
                return listOf();
            }
            val cursor = resolver?.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, StateLibrary.PROJECTION_MEDIA, "${MediaStore.Audio.Media.ALBUM_ID} = ?",
                arrayOf(albumId.toString()),
                null) ?: return listOf();
            return cursor.use {
                cursor.moveToFirst();
                val list = mutableListOf<Track>()
                while(!cursor.isAfterLast) {
                    list.add(StateLibrary.audioFromCursor(cursor));
                    cursor.moveToNext();
                }
                return@use list;
            }
        }
        fun getAlbum(context: Context, id: Long): AndroidAlbum? {
            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Album contentResolver not found");
                return null
            }
            val cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                PROJECTION,
                "${MediaStore.Audio.Albums.ALBUM_ID} = ?",
                arrayOf(id.toString()), null) ?: return null;
            return cursor.use {
                cursor.moveToFirst();
                if(cursor.isAfterLast)
                    return@use null;
                return@use fromCursor(cursor);
            }
        }
        fun getAlbums(context: Context, query: String? = null, args: Array<String>? = null): List<AndroidAlbum> {
            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Album contentResolver not found");
                return listOf();
            }
            val cursor = resolver?.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, PROJECTION, query, args,
                MediaStore.Audio.Albums.ALBUM + " ASC") ?: return listOf();
            return cursor.use {
                cursor.moveToFirst();
                val list = mutableListOf<AndroidAlbum>()
                while(!cursor.isAfterLast) {
                    list.add(fromCursor(cursor));
                    cursor.moveToNext();
                }
                return@use list;
            }
        }
        fun getArtistAlbums(context: Context, artistId: Long): List<AndroidAlbum> {
            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Album contentResolver not found");
                return listOf();
            }
            val cursor = resolver?.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, PROJECTION, "${MediaStore.Audio.Media.ARTIST_ID} = ?",
                arrayOf(artistId.toString()),
                MediaStore.Audio.Albums.ALBUM + " ASC") ?: return listOf();
            return cursor.use {
                cursor.moveToFirst();
                val list = mutableListOf<AndroidAlbum>()
                while(!cursor.isAfterLast) {
                    list.add(fromCursor(cursor));
                    cursor.moveToNext();
                }
                return@use list;
            }
        }
        fun getArtistAlbumWithThumbnail(context: Context, artistId: Long): AndroidAlbum? {
            val resolver =  context.contentResolver;
            if(resolver == null) {
                Logger.w(TAG, "Album contentResolver not found");
                return null;
            }
            val cursor = resolver?.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, PROJECTION, "${MediaStore.Audio.Media.ARTIST_ID} = ?",
                arrayOf(artistId.toString()),
                MediaStore.Audio.Albums.ALBUM + " ASC") ?: return null;
            return cursor.use {
                cursor.moveToFirst();
                while(!cursor.isAfterLast) {
                    val album = fromCursor(cursor);
                    if(album.thumbnail != null)
                        return album
                    cursor.moveToNext();
                }
                return@use null;
            }
        }
    }
}


class FileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean = false,
    val thumbnail: String? = null,

    var removable: Boolean = false
) {

    fun getSubFiles(context: Context): List<FileEntry> {
        if(isDirectory) {
            if(path.startsWith("content://"))
                return DocumentFile.fromTreeUri(context, path.toUri())?.listFiles()
                    ?.map { fromFile(it) } ?: return listOf();
            return File(path).listFiles()
                .map { fromFile(it) }
        }
        return listOf();
    }

    companion object {
        fun fromPath(path: String): FileEntry {
            /*
            val cursor = StateApp.instance.context.contentResolver.query(path.toUri(), null, null, null, null);
            cursor?.moveToFirst();
            val fileName = cursor?.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            cursor?.close();
            return FileEntry(path, fileName, );
             */
            val file = File(path);
            return FileEntry(file.path, file.name, file.isDirectory);
        }
        fun fromFile(file: File): FileEntry {
            return FileEntry(file.path, file.name, file.isDirectory);
        }
        fun fromFile(file: DocumentFile): FileEntry {
            return FileEntry(file.uri.toString(), file.name ?: "", file.isDirectory);
        }
    }
}