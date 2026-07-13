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

@Entity(tableName = "directories")
class DBDirectory(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    override val name: String,

    val dateAdded: OffsetDateTime = OffsetDateTime.MIN,
    val dateUpdated: OffsetDateTime = OffsetDateTime.MIN,

    override val path: String,

    @ColumnInfo(defaultValue = "FALSE")
    var hidden: Boolean = false
): IFileItem {
    @Ignore
    override var type = FilesItemType.Root;

    fun getDirectoryChildren(context: Context): DirectoryChildren {
        val docFile = FastDocumentFile.fromUri(context, Uri.parse(path));
        val allFiles = docFile?.getFiles() ?: return DirectoryChildren(path, listOf(), listOf());
        val dirs = allFiles.filter { it.isDirectory }.map { DocumentDirectoryItem(it) };
        val files = allFiles.filter { !it.isDirectory }.map { DocumentFileItem(it) };
        return DirectoryChildren(path, dirs, files);
    }
    fun getFiles(context: Context): List<IFileItem> {
        val data = getDirectoryChildren(context);
        return data.directories + data.files;
    }

}

data class DirectoryChildren(
    val path: String,
    val directories: List<DocumentDirectoryItem>,
    val files: List<DocumentFileItem>
)

@Dao
interface DBDirectoryDao {
    @Query("SELECT * FROM directories WHERE hidden != 1")
    fun getAll(): List<DBDirectory>;
    @Query("SELECT * FROM directories WHERE hidden == 1")
    fun getAllHidden(): List<DBDirectory>;

    @Query("SELECT * FROM directories WHERE id = :id")
    fun get(id: Long): DBDirectory?;

    @Query("SELECT * FROM directories WHERE name = :name")
    fun getByName(name: String): DBDirectory?;


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg dir: DBDirectory): Array<Long>;

    @Query("DELETE FROM directories WHERE id = :id")
    fun delete(id: Long);
}