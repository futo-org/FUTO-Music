package com.futo.music.files

import android.net.Uri
import com.futo.music.toFileNameWithoutExtension
import java.nio.file.Path

//Simple M3U Playlist parser, only supports small subset that we actually use for the app.
//Supported Attributes: #EXTINF, #PLAYLIST
class M3UPlaylist {
    var name: String? = null;
    var path: Uri? = null;
    val items = mutableListOf<Item>();


    fun getRequiredSubFolders(): List<String> {
        var paths = mutableListOf<String>();
        for(item in items) {
            val path = item.path.split("/");
            var cancel = false;
            var dirPath = "";
            for(seg in 0..<(path.size-1)) {
                if(SAFE_DIR_CHARS.matches(path[seg])) {
                    dirPath += path[seg] + "/";
                }
                else {
                    cancel = true;
                    break;
                }
            }
            if(cancel)
                break;
            dirPath = dirPath.trim('/')
            if(!paths.contains(dirPath))
                paths.add(dirPath);
        }
        return paths;
    }

    companion object {

        fun parse(str: String, path: Uri? = null): M3UPlaylist? {
            if(!str.startsWith("#EXTM3U"))
                return null;
            val playlist = M3UPlaylist();
            playlist.path = path;

            val lines = str.split("\n");
            if(lines.size > 2 && !lines.subList(1, lines.size).any { it.contains("#") }) {
                //File name only list
                var i = 1;
                while (i < lines.size) {
                    val line = lines[i];
                    if(line.isNotEmpty())
                        playlist.items.add(Item(line.trim()));
                    i++
                }
            }
            else {
                var i = 0;
                while (i < lines.size) {
                    val line = lines[i];
                    if (line.startsWith("#EXTINF:") && i < lines.size - 1) {
                        //We're just ignoring extinf info cuz not needed for player, and easily to mis-parse.
                        val extInfLine = line.substring(line.indexOf(":") + 1).trim();
                        val path = lines[i + 1];
                        playlist.items.add(Item(path.trim()));
                        i++
                    } else if (line.startsWith("#PLAYLIST:")) {
                        val extPlaylistLine = line.substring(line.indexOf(":") + 1).trim();
                        playlist.name = extPlaylistLine;
                    }
                    i++;
                }
            }
            if(playlist.name == null && path != null)
                playlist.name = path.toString().toFileNameWithoutExtension();
            if(playlist.items.isNotEmpty())
                return playlist;
            return null;
        }

        private val SAFE_DIR_CHARS = Regex("^[A-Za-z0-9 _-]+$");
    }

    data class Item(
        val path: String
    )
}