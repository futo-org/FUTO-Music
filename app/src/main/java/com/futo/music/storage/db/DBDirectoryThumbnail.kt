package com.futo.music.storage.db

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.futo.music.files.DocumentDirectoryItem
import com.futo.music.files.DocumentFileItem
import com.futo.music.files.FastDocumentFile
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.PlayableType
import com.futo.music.models.playable.Track
import com.futo.music.states.StateDatabase
import com.futo.music.toFileName
import com.futo.music.ui.adapters.FilesItemType
import com.futo.music.ui.adapters.IFileItem
import java.time.OffsetDateTime

@Entity(tableName = "files_thumb")
class DBDirectoryThumbnail(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,

    val dateUpdated: OffsetDateTime = OffsetDateTime.MIN,

    val path: String,
    val directory: String,
    val albumId: Long,


    @ColumnInfo(defaultValue = "FALSE")
    var hidden: Boolean = false
) {

}

@Dao
interface DBDirectoryThumbnailDao {
    @Query("SELECT * FROM files_thumb WHERE hidden != 1")
    fun getAll(): List<DBDirectoryThumbnail>;
    @Query("SELECT * FROM files_thumb WHERE hidden == 1")
    fun getAllHidden(): List<DBDirectoryThumbnail>;

    @Query("SELECT * FROM files_thumb WHERE id = :id")
    fun get(id: Long): DBDirectoryThumbnail?;


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg dir: DBDirectoryThumbnail): Array<Long>;

    @Query("DELETE FROM files_thumb WHERE id = :id")
    fun delete(id: Long);
}