package io.github.mafflerbach.thoughtcanvas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.mafflerbach.thoughtcanvas.core.database.entity.EntryTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    /**
     * Upsert a tag by name. Returns the row id of the resulting row, whether
     * newly inserted or already present.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkEntryToTag(crossRef: EntryTagCrossRef)

    @Query("DELETE FROM entry_tag_cross_ref WHERE entry_date = :date")
    suspend fun clearTagsForEntry(date: String)

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN entry_tag_cross_ref
          ON tags.id = entry_tag_cross_ref.tag_id
        WHERE entry_tag_cross_ref.entry_date = :date
        ORDER BY tags.name ASC
        """,
    )
    fun observeTagsForEntry(date: String): Flow<List<TagEntity>>
}
