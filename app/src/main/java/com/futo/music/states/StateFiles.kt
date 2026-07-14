package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.futo.music.UIDialogs
import com.futo.music.constructs.Event1
import com.futo.music.files.DocumentFileItem
import com.futo.music.files.FastDocumentFile
import com.futo.music.files.M3UPlaylist
import com.futo.music.logging.Logger
import com.futo.music.parentPath
import com.futo.music.storage.db.DBDirectory
import com.futo.music.storage.db.DBDirectoryThumbnail
import com.futo.music.storage.db.DirectoryChildren
import com.futo.music.toFileName
import com.futo.music.toFileNameWithoutExtension
import java.time.OffsetDateTime

class StateFiles {
    val onScanning = Event1<String>();
    val onScanningFinished = Event1<DirectoryScanResult>();

    private val fileThumbnails = mutableMapOf<String, String>();
    private val albumThumbnail = mutableMapOf<Long, String>();

    fun getAlbumImage(albumId: Long): String? {
        return albumThumbnail.getOrDefault(albumId, null);
    }
    fun getDirectoryImage(path: String): String? {
        return fileThumbnails.getOrDefault(path, null);
    }

    fun prefill(thumbs: List<DBDirectoryThumbnail>) {
        for(thumb in thumbs) {
            if(thumb.albumId > 0)
                albumThumbnail.put(thumb.albumId, thumb.path);
            fileThumbnails.put(thumb.directory, thumb.path);
        }
    }

    fun scanAndProcessDirectory(context: Context, dir: DBDirectory) {
        UIDialogs.appToast("Scanning [${dir.name}]");
        val result = scanDirectory(context, dir);

        for(img in result.thumbnails) {
            val path = img.path.parentPath();
            val album = result.directoryAlbumMap.getOrDefault(path, -1);
            if(album.toInt() != -1) {
                albumThumbnail.put(album, img.path);
            }
            fileThumbnails.put(img.path.parentPath(), img.path);

            StateDatabase.instance.db.directoryThumbDao().insert(
                DBDirectoryThumbnail(
                    path = img.path,
                    directory = path,
                    albumId = album,
                    dateUpdated = OffsetDateTime.now(),
                    hidden = false
                )
            );

        }

        for(playlist in result.playlists) {
            try {
                val m3u = M3UPlaylist.parse(playlist.file.readAsText(context) ?: continue, playlist.file.uri);
                if(m3u != null)
                    Logger.i(TAG, "Playlist found: ${m3u.name}, ${m3u.path} with ${m3u.items.size} items");
            }
            catch(ex: Throwable) {
                Logger.e(TAG, "Playlist parse error: " + ex.message, ex);
            }
        }
    }

    fun scanDirectory(context: Context, dir: DBDirectory): DirectoryScanResult {
        val structure = dir.getDirectoryChildren(context);
        return scanDirectory(structure);
    }

    fun scanDirectory(dir: DirectoryChildren, root: Boolean = true): DirectoryScanResult {
        val name = dir.path.toFileName();
        onScanning.emit(name);

        val result = scanDirectoryFeatures(dir.path, dir.files);
        for(dir in dir.directories)
            result.add(scanDirectory(dir.getDirectoryChildren(), false));

        if(root)
            onScanningFinished.emit(result);
        return result;
    }

    private val priorityImages = listOf<String>(
        "album",
        "cover",
        "art",
        "front",
        "albumart",
        "albumartsmall",
        "thumb",
        "thumbnail",
        "folder",
        "disc",
        ""
    );
    fun scanDirectoryFeatures(path: String, files: List<DocumentFileItem>): DirectoryScanResult {
        val dir = DirectoryScanResult();

        var hasMusic = false;
        var albumId: Long? = null;
        var albumConfirmState = 0;

        val images = mutableListOf<Pair<String, DocumentFileItem>>();
        for(file in files){
            if(file.mimeType.startsWith("image/"))
                images.add(Pair(file.path.toFileNameWithoutExtension(), file));
            else if(file.name.endsWith(".m3u"))
                dir.playlists.add(DirectoryScanPlaylist(file.docFile));
            else if(file.mimeType.startsWith("audio/")) {
                hasMusic = true;

                if(albumConfirmState < 2) {
                    val trackId = StateDatabase.instance.getTrackIdByFileName(file.name);
                    if (trackId != null) {
                        val album = StateDatabase.instance.getTrackAlbums(trackId).firstOrNull();
                        if (album != null) {
                            if (albumId == null) {
                                albumId = album.id;
                            } else {
                                if (albumId == album.id) {
                                    albumConfirmState++;
                                } else {
                                    albumConfirmState = -1;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        //albumMap
        if(albumId != null && albumConfirmState > 0){
            dir.directoryAlbumMap.put(path, albumId);
        }

        //Thumbnails
        if(hasMusic) {
            if(images.size == 1)
                dir.thumbnails.add(DirectoryScanThumbnail(images[0].second.path));
            else if(images.size > 1) {
                var finalImage: DocumentFileItem? = null;
                var priority = 999;
                for(image in images) {
                    val imageNameLower = image.first.lowercase();
                    val priorityIndex = priorityImages.indexOf(imageNameLower);
                    if(priorityIndex >= 0) {
                        if(priority > priorityIndex) {
                            finalImage = image.second;
                            priority = priorityIndex
                        }
                    }
                    else if(finalImage == null)
                        finalImage = image.second;
                }
                if(finalImage != null)
                    dir.thumbnails.add(DirectoryScanThumbnail(finalImage.path));
            }
        }

        return dir;
    }

    companion object {

        private val TAG = "StateFiles";
        @SuppressLint("StaticFieldLeak")
        private var _instance : StateFiles? = null;
        val instance : StateFiles
            get(){
                if(_instance == null)
                    _instance = StateFiles();
                return _instance!!;
            };

    }
}

class DirectoryScanResult {
    val playlists = mutableListOf<DirectoryScanPlaylist>()
    val thumbnails = mutableListOf<DirectoryScanThumbnail>();

    val directoryAlbumMap = mutableMapOf<String, Long>();

    fun add(dir: DirectoryScanResult){
        playlists.addAll(dir.playlists);
        thumbnails.addAll(dir.thumbnails);
        for(d in dir.directoryAlbumMap)
            directoryAlbumMap.put(d.key, d.value);
    }
}

data class DirectoryScanPlaylist(
    val file: FastDocumentFile,
)
data class DirectoryScanThumbnail(
    val path: String
)