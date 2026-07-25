package io.github.mafflerbach.thoughtcanvas.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.mafflerbach.thoughtcanvas.core.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    /**
     * Insert-or-update. Uses Room's [Upsert] so an existing row is UPDATEd
     * in place instead of DELETE+INSERT — otherwise cascading foreign keys
     * would wipe attachments and tag links every time we bump `updatedAt`.
     */
    @Upsert
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE date = :date")
    suspend fun findByDate(date: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE date = :date")
    fun observeByDate(date: String): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("DELETE FROM journal_entries WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
