package com.futo.music.files

//Simple M3U Playlist parser, only supports small subset that we actually use for the app.
//Supported Attributes: #EXTINF, #PLAYLIST
class M3UPlaylist {
    var name: String? = null;
    val items = mutableListOf<Item>();


    companion object {

        fun parse(str: String): M3UPlaylist? {
            if(!str.startsWith("#EXTM3U"))
                return null;
            val playlist = M3UPlaylist();

            val lines = str.split("\n");
            var i = 0;
            while(i < lines.size) {
                val line = lines[i];
                if(line.startsWith("#EXTINF:") && i < lines.size - 1) {
                    //We're just ignoring extinf info cuz not needed for player, and easily to mis-parse.
                    val extInfLine = line.substring(line.indexOf(":") + 1).trim();
                    val path = lines[i + 1];
                    playlist.items.add(Item(path.trim()));
                    i++
                }
                else if(line.startsWith("#PLAYLIST:")) {
                    val extPlaylistLine = line.substring(line.indexOf(":") + 1).trim();
                    playlist.name = extPlaylistLine;
                }
                i++;
            }

            if(playlist.items.isNotEmpty())
                return playlist;
            return null;
        }
    }

    data class Item(
        val path: String
    )
}