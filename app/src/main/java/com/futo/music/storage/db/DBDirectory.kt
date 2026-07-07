package com.futo.music.storage.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.PlayableType
import com.futo.music.models.playable.Track
import com.futo.music.states.StateDatabase
import java.time.OffsetDateTime

@Entity(tableName = "directories")
class DBDirectory(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val name: String,

    val dateAdded: OffsetDateTime = OffsetDateTime.MIN,
    val dateUpdated: OffsetDateTime = OffsetDateTime.MIN,

    val path: String,

    @ColumnInfo(defaultValue = "FALSE")
    var hidden: Boolean = false
) {
}

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
}