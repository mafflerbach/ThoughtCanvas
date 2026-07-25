package io.github.mafflerbach.thoughtcanvas.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.mafflerbach.thoughtcanvas.core.database.entity.EdgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EdgeDao {
    @Upsert
    suspend fun upsertAll(edges: List<EdgeEntity>)

    @Query("SELECT * FROM edges WHERE canvas_id = :canvasId")
    fun observeForCanvas(canvasId: String): Flow<List<EdgeEntity>>

    @Query("DELETE FROM edges WHERE canvas_id = :canvasId")
    suspend fun deleteForCanvas(canvasId: String)
}
