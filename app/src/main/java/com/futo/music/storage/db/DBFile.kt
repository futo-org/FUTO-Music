package com.futo.music.storage.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.time.OffsetDateTime


enum class DBFileType(val value: Int) {
    Unknown(0),
    Media(1)
}

@Entity(tableName = "dir_files",
    indices = [
        Index(value = ["trackId", "rootId"])
    ])
class DBFile(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val name: String,

    var trackId: Long = -1,
    var rootId: Long = -1,

    val dateAdded: OffsetDateTime = OffsetDateTime.MIN,

    val path: String,
    var fileType: DBFileType = DBFileType.Unknown
) {

}
@Entity
class DBFileIds(
    val id: Long,
    val trackId: Long,
    val rootId: Long
);

@Dao
interface DBFileDao {
    @Query("SELECT * FROM dir_files")
    fun getAll(): List<DBFile>;

    @Query("SELECT id, trackId, rootId from dir_files")
    fun getAllIds(): List<DBFileIds>

    @Query("""SELECT DISTINCT at.albumId FROM album_tracks AS at
        INNER JOIN dir_files AS df ON df.trackId = at.trackId
    """)
    fun getAlbumIdsInFiles(): List<Long>

    @Query("""SELECT DISTINCT at.artistId FROM artist_tracks AS at
        INNER JOIN dir_files AS df ON df.trackId = at.trackId
    """)
    fun getArtistIdsInFiles(): List<Long>

    @Query("SELECT id, trackId, rootId from dir_files WHERE rootId = :rootId")
    fun getAllIdsInDirectory(rootId: Long): List<DBFileIds>

    @Query("SELECT * FROM dir_files WHERE id = :id")
    fun get(id: Long): DBFile?;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg files: DBFile): Array<Long>;

    @Query("DELETE FROM dir_files WHERE id = :id")
    fun delete(id: Long);
    @Query("DELETE FROM dir_files WHERE id IN (:ids)")
    fun deleteAll(ids: List<Long>);

    @Query("DELETE FROM dir_files WHERE rootId = :rootId")
    fun deleteRoot(rootId: Long);
}