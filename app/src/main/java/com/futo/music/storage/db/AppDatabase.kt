package com.futo.music.storage.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DBTrack::class, DBArtist::class, DBAlbum::class, DBArtistTrack::class, DBAlbumArtist::class, DBAlbumTrack::class, DBPlaylist::class, DBPlaylistTrack::class],
    version = 3,
    autoMigrations = [
        AutoMigration(1, 2),
        AutoMigration(2, 3)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun tracksDao(): DBTrackDao
    abstract fun artistDao(): DBArtistDao
    abstract fun albumDao(): DBAlbumDao
    abstract fun playlistDao(): DBPlaylistDao
}