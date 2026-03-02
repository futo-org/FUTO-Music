package com.futo.music.models.playable

enum class PlayableType(val value: Int) {
    Unknown(0),
    Track(1),
    Artist(2),
    Album(3),
    Playlist(4),
    Vibe(5);
}