package io.github.mafflerbach.thoughtcanvas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.mafflerbach.thoughtcanvas.core.database.entity.BlockTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.CanvasTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkCanvasToTag(crossRef: CanvasTagCrossRef)

    @Query("DELETE FROM canvas_tag_cross_ref WHERE canvas_id = :canvasId")
    suspend fun clearTagsForCanvas(canvasId: String)

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN canvas_tag_cross_ref
          ON tags.id = canvas_tag_cross_ref.tag_id
        WHERE canvas_tag_cross_ref.canvas_id = :canvasId
        ORDER BY tags.name ASC
        """,
    )
    fun observeTagsForCanvas(canvasId: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkBlockToTag(crossRef: BlockTagCrossRef)

    @Query("DELETE FROM block_tag_cross_ref WHERE block_id = :blockId")
    suspend fun clearTagsForBlock(blockId: String)

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN block_tag_cross_ref
          ON tags.id = block_tag_cross_ref.tag_id
        WHERE block_tag_cross_ref.block_id = :blockId
        ORDER BY tags.name ASC
        """,
    )
    fun observeTagsForBlock(blockId: String): Flow<List<TagEntity>>
}
