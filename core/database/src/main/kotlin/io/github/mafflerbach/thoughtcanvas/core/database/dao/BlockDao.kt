package io.github.mafflerbach.thoughtcanvas.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.mafflerbach.thoughtcanvas.core.database.entity.BlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {
    @Upsert
    suspend fun upsert(block: BlockEntity)

    @Upsert
    suspend fun upsertAll(blocks: List<BlockEntity>)

    @Query("SELECT * FROM blocks WHERE canvas_id = :canvasId ORDER BY z ASC, updated_at ASC")
    fun observeForCanvas(canvasId: String): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks WHERE canvas_id = :canvasId")
    suspend fun listForCanvas(canvasId: String): List<BlockEntity>

    @Query("DELETE FROM blocks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM blocks WHERE canvas_id = :canvasId")
    suspend fun deleteForCanvas(canvasId: String)
}
