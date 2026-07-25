package io.github.mafflerbach.thoughtcanvas.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.mafflerbach.thoughtcanvas.core.database.entity.CanvasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasDao {
    @Upsert
    suspend fun upsert(canvas: CanvasEntity)

    @Query("SELECT * FROM canvases WHERE id = :id")
    suspend fun findById(id: String): CanvasEntity?

    @Query("SELECT * FROM canvases WHERE path = :path LIMIT 1")
    suspend fun findByPath(path: String): CanvasEntity?

    @Query("SELECT * FROM canvases WHERE id = :id")
    fun observeById(id: String): Flow<CanvasEntity?>

    @Query("SELECT * FROM canvases ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<CanvasEntity>>

    @Query("DELETE FROM canvases WHERE id = :id")
    suspend fun deleteById(id: String)
}
