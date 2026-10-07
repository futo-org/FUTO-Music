package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.constructs.Event1
import com.futo.music.files.DocumentFileItem
import com.futo.music.files.FastDocumentFile
import com.futo.music.files.M3UPlaylist
import com.futo.music.logging.Logger
import com.futo.music.models.ImageVariable
import com.futo.music.parentPath
import com.futo.music.storage.db.DBDirectory
import com.futo.music.storage.db.DBDirectoryThumbnail
import com.futo.music.storage.db.DBFile
import com.futo.music.storage.db.DBFileType
import com.futo.music.storage.db.DirectoryChildren
import com.futo.music.toFileName
import com.futo.music.toFileNameWithoutExtension
import com.futo.music.toScoreRating
import java.time.OffsetDateTime
import java.util.UUID

class StateFiles {
    val onScanning = Event1<String>();
    val onScanningFinished = Event1<DirectoryScanResult>();


    var fileAccessIds: Map<Long, Long> = mapOf();
    var albumsInFiles: HashSet<Long> = HashSet();
    var artistsInFiles: HashSet<Long> = HashSet();
    var dirsInaccessibleIds: HashSet<Long> = HashSet();
    private val fileThumbnails = mutableMapOf<String, String>();
    private val albumThumbnail = mutableMapOf<Long, String>();

    fun getAlbumImage(albumId: Long): String? {
        return albumThumbnail.getOrDefault(albumId, null);
    }
    fun getDirectoryImage(path: String): String? {
        return fileThumbnails.getOrDefault(path, null);
    }

    fun prefillThumbnails(thumbs: List<DBDirectoryThumbnail>) {
        for(thumb in thumbs) {
            if(thumb.albumId > 0)
                albumThumbnail.put(thumb.albumId, thumb.path);
            fileThumbnails.put(thumb.directory, thumb.path);
        }
    }

    fun updateFileAccessIds() {
        val allIds = StateDatabase.instance.db.filesDao().getAllIds().map { Pair(it.trackId, it.id) }.distinctBy { it.first }.associate { Pair(it.first, it.second) };
        val allAlbumIds = StateDatabase.instance.db.filesDao().getAlbumIdsInFiles();
        val allArtistIds = StateDatabase.instance.db.filesDao().getArtistIdsInFiles();
        fileAccessIds = allIds;
        albumsInFiles = HashSet(allAlbumIds);
        artistsInFiles = HashSet(allArtistIds);

        StateApp.instance.refreshHome();
    }

    fun updateRootDirectoryValidation(context: Context) {
        val items = StateDatabase.instance.db.directoryDao().getAll();

        val inaccessible = HashSet<Long>();
        for(item in items) {
            try {
                val doc = item.getDirectoryDocument(context);
                if (doc == null) {
                    inaccessible.add(item.id);
                }
            } catch(ex: Throwable) {
                inaccessible.add(item.id);
            }
        }
        dirsInaccessibleIds = inaccessible;
    }

    fun scanAndProcessAll(context: Context) {
        val dirs = StateDatabase.instance.db.directoryDao().getAll();
        for(dir in dirs){
            try {
                scanAndProcessDirectory(context, dir, null, false, true);
            }
            catch(ex: Throwable) {
                Logger.e(TAG, "Failed to rescan dir ${dir.name}", ex);
                UIDialogs.appToast("Rescan ${dir.name} failed:\n" + ex.message);
            }
        }
    }
    fun scanAndProcessDirectory(context: Context, dir: DBDirectory, onlyFindTrackNames: MutableSet<String>? = null, preventNotify: Boolean = false, scanFileMetadata: Boolean = false) {
        UIDialogs.appToast("Scanning [${dir.name}]");
        val announcement = StateAnnouncement.instance.registerLoading("Scanning directory [${dir.name}]", "", ImageVariable.fromResource(R.drawable.ic_files), null, true);

        try {
            announcement.setProgress(0, "Scanning...");
            val result = scanDirectory(context, dir, onlyFindTrackNames);
            val allFiles = StateDatabase.instance.db.filesDao().getAllIdsInDirectory(dir.id).associateBy { it.trackId }.toMutableMap();
            announcement.setProgress(0.25, "Processing file registrations..");
            for (file in result.filePaths) {
                if (file.trackId >= 0) {
                    if (!allFiles.containsKey(file.trackId)) {
                        var popm: POPMHeaders? = null;
                        if(scanFileMetadata) {
                            try {
                                announcement.setProgress(0.25, "Scanning file headers for [${file.name}]");
                                val docFile = FastDocumentFile.fromUri(context, file.path);
                                if(docFile != null) {
                                    popm = readPOPMHeaders(context, docFile);
                                }
                            }
                            catch(ex: Throwable) {
                                Logger.e(TAG, "Failed to scan file metadata: " + ex.message, ex);
                            }
                        }

                        StateDatabase.instance.db.filesDao().insert(
                            DBFile(
                                name = file.name,
                                path = file.path.toString(),
                                dateAdded = OffsetDateTime.now(),
                                trackId = file.trackId,
                                rootId = dir.id,
                                fileType = DBFileType.Media,
                                ratingPOPM = popm?.popmRatingToStars()?.toScoreRating()
                            )
                        );
                    } else
                        allFiles.remove(file.trackId);
                }
            }
            announcement.setProgress(0.4, "Deleting missing files..");
            if(onlyFindTrackNames == null) {
                StateDatabase.instance.db.filesDao().deleteAll(allFiles.values.filter { it.rootId == dir.id }.map { it.id })
            }

            announcement.setProgress(0.6, "Processing thumbnails..");
            for (img in result.thumbnails) {
                val path = img.path.toString().parentPath();
                val album = result.directoryAlbumMap.getOrDefault(path, -1);
                if (album.toInt() != -1) {
                    albumThumbnail.put(album, img.path.toString());
                }
                fileThumbnails.put(img.path.toString().parentPath(), img.path.toString());

                StateDatabase.instance.db.directoryThumbDao().insert(
                    DBDirectoryThumbnail(
                        path = img.path.toString(),
                        directory = path,
                        albumId = album,
                        dateUpdated = OffsetDateTime.now(),
                        hidden = false
                    )
                );

            }

            announcement.setProgress(0.8, "Processing playlists..");
            for (playlist in result.playlists) {
                try {
                    val m3u = M3UPlaylist.parse(playlist.file.readAsText(context) ?: continue, playlist.file.uri);
                    if (m3u != null)
                        Logger.i(TAG, "Playlist found: ${m3u.name}, ${m3u.path} with ${m3u.items.size} items");
                } catch (ex: Throwable) {
                    Logger.e(TAG, "Playlist parse error: " + ex.message, ex);
                }
            }
            announcement.setProgress(1.0, "Done!");
            updateFileAccessIds();

            if(!preventNotify)
                StateAnnouncement.instance.registerAnnouncement(SessionAnnouncement("files_scanned_" + UUID.randomUUID().toString(), "Scanned directory [${dir.name}]", "thumbnails: ${result.thumbnails.size}, playlists: ${result.playlists.size}, tracks: ${result.filePaths.size}",
                    AnnouncementType.SESSION,
                    icon = ImageVariable.fromResource(R.drawable.ic_files)));
        }
        catch(ex: Throwable) {
            Logger.e(TAG, "Scanning failed for dir [${dir.name}]", ex);
            UIDialogs.appToast("Scanning failed for [${dir.name}]:\n" + ex.message);
        }
        finally {
            StateAnnouncement.instance.closeAnnouncement(announcement.id);
        }
    }

    fun scanDirectory(context: Context, dir: DBDirectory, onlyFindNames: MutableSet<String>? = null): DirectoryScanResult {
        val structure = dir.getDirectoryChildren(context);
        return scanDirectory(structure, onlyFindNames = onlyFindNames);
    }

    fun scanDirectory(dir: DirectoryChildren, root: Boolean = true, onlyFindNames: MutableSet<String>? = null): DirectoryScanResult {
        val name = dir.path.toString().toFileName();
        onScanning.emit(name);

        val result = scanDirectoryFeatures(Uri.parse(dir.path), dir.files, onlyFindNames);
        for(dir in dir.directories)
            result.add(scanDirectory(dir.getDirectoryChildren(), false, onlyFindNames));

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
    fun scanDirectoryFeatures(path: Uri, files: List<DocumentFileItem>, onlyFindNames: MutableSet<String>? = null): DirectoryScanResult {
        val dir = DirectoryScanResult();

        var hasMusic = false;
        var albumId: Long? = null;
        var albumConfirmState = 0;

        val images = mutableListOf<Pair<String, DocumentFileItem>>();
        for(file in files){
            var searchedFor = false;
            if(onlyFindNames != null && onlyFindNames.contains(file.name)) {
                onlyFindNames.remove(file.name);
                searchedFor = true;
            }
            else if(onlyFindNames != null)
                continue;
            if(file.mimeType.startsWith("image/"))
                images.add(Pair(file.path.toString().toFileNameWithoutExtension(), file));
            else if(file.name.endsWith(".m3u"))
                dir.playlists.add(DirectoryScanPlaylist(file.docFile));
            else if(file.mimeType.startsWith("audio/")) {
                hasMusic = true;

                val trackId = StateDatabase.instance.getTrackIdByFileName(file.name);
                if (trackId != null) {
                    dir.filePaths.add(DirectoryScanFile(file.uri, file.name, trackId));

                    if (albumConfirmState < 2 || searchedFor) {
                        val album = StateDatabase.instance.getTrackAlbums(trackId).firstOrNull();
                        if (album != null) {
                            if (albumId == null || searchedFor) {
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
            dir.directoryAlbumMap.put(path.toString(), albumId);
        }

        //Thumbnails
        if(hasMusic) {
            if(images.size == 1)
                dir.thumbnails.add(DirectoryScanThumbnail(images[0].second.uri));
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
                    dir.thumbnails.add(DirectoryScanThumbnail(finalImage.uri));
            }
        }

        return dir;
    }


    private fun readPOPMHeaders(context: Context, docFile: FastDocumentFile): POPMHeaders? {
        return docFile.readAsStream<POPMHeaders>(context) { input ->
            val header = ByteArray(10);
            if (input.read(header) != 10)
                return@readAsStream POPMHeaders();
            if (String(header, 0, 3, Charsets.ISO_8859_1) != "ID3")
                return@readAsStream POPMHeaders();

            val version = header[3].toInt() and 0xFF;
            if (version !in 3..4)
                return@readAsStream POPMHeaders();

            val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F);

            var consumed = 0;

            while (consumed + 10 <= tagSize) {
                val frameHeader = ByteArray(10);
                if (input.read(frameHeader) != 10)
                    return@readAsStream POPMHeaders();

                consumed += 10;

                val frameId = String(frameHeader, 0, 4, Charsets.ISO_8859_1);
                if (frameId.all { it == '\u0000' })
                    return@readAsStream POPMHeaders();

                val frameSize = if (version == 4) {
                    ((frameHeader[4].toInt() and 0x7F) shl 21) or
                            ((frameHeader[5].toInt() and 0x7F) shl 14) or
                            ((frameHeader[6].toInt() and 0x7F) shl 7) or
                            (frameHeader[7].toInt() and 0x7F);
                } else {
                    ((frameHeader[4].toInt() and 0xFF) shl 24) or
                            ((frameHeader[5].toInt() and 0xFF) shl 16) or
                            ((frameHeader[6].toInt() and 0xFF) shl 8) or
                            (frameHeader[7].toInt() and 0xFF);
                }

                if (frameSize <= 0 || consumed + frameSize > tagSize)
                    return@readAsStream POPMHeaders();

                if (frameId == "POPM") {
                    val data = ByteArray(frameSize);
                    var offset = 0;

                    while (offset < frameSize) {
                        val read = input.read(data, offset, frameSize - offset);
                        if (read < 0)
                            return@readAsStream POPMHeaders();

                        offset += read;
                    }

                    val ownerEnd = data.indexOf(0);
                    if (ownerEnd >= 0 && ownerEnd + 1 < data.size) {
                        val owner = String(data, 0, ownerEnd, Charsets.ISO_8859_1);
                        val rating = data[ownerEnd + 1].toInt() and 0xFF;

                        return@readAsStream POPMHeaders(owner, rating);
                    }

                    return@readAsStream POPMHeaders();
                }

                var remaining = frameSize.toLong();
                while (remaining > 0) {
                    val skipped = input.skip(remaining);

                    if (skipped > 0)
                        remaining -= skipped;
                    else {
                        if (input.read() == -1)
                            return@readAsStream POPMHeaders();

                        remaining--;
                    }
                }

                consumed += frameSize;
            }

            return@readAsStream POPMHeaders();
        }
    }
    data class POPMHeaders(
        val ratingType: String? = null,
        val rating: Int? = null
    ) {
        fun popmRatingToStars(): Int {
            val rating = this.rating ?: return 0;

            if (rating <= 0)
                return 0;
            if (rating >= 224)
                return 5;
            if (rating >= 160)
                return 4;
            if (rating >= 96)
                return 3;
            if (rating >= 32)
                return 2;

            return 1;
        }
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

    val filePaths = mutableListOf<DirectoryScanFile>();
    val directoryAlbumMap = mutableMapOf<String, Long>();

    fun add(dir: DirectoryScanResult){
        playlists.addAll(dir.playlists);
        thumbnails.addAll(dir.thumbnails);
        filePaths.addAll(dir.filePaths);
        for(d in dir.directoryAlbumMap)
            directoryAlbumMap.put(d.key, d.value);
    }
}

data class DirectoryScanFile(
    val path: Uri,
    val name: String,
    val trackId: Long
)
data class DirectoryScanPlaylist(
    val file: FastDocumentFile,
)
data class DirectoryScanThumbnail(
    val path: Uri
)